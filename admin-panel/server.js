const express = require('express');
const cors = require('cors');
const path = require('path');
const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const db = require('./database');

const app = express();
const PORT = process.env.PORT || 4000;
const JWT_SECRET = process.env.JWT_SECRET || 'aqeel_rider_jwt_secret_key_2026_super_secure';

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Helper to generate unique AR-XXXX-XXXX key
function generateLicenseCode() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  const part1 = Array.from(crypto.randomBytes(4)).map(b => chars[b % chars.length]).join('');
  const part2 = Array.from(crypto.randomBytes(4)).map(b => chars[b % chars.length]).join('');
  return `AR-${part1}-${part2}`;
}

// Log an action to license_logs
function logAction(licenseId, licenseKey, action, deviceId, ipAddress, details) {
  try {
    const stmt = db.prepare(`
      INSERT INTO license_logs (license_id, license_key, action, device_id, ip_address, details)
      VALUES (?, ?, ?, ?, ?, ?)
    `);
    stmt.run(licenseId, licenseKey, action, deviceId || null, ipAddress || null, details || '');
  } catch (err) {
    console.error('Error logging action:', err.message);
  }
}

// Admin Auth Middleware
function requireAdminAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ success: false, message: 'Authentication required' });
  }
  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.admin = decoded;
    next();
  } catch (err) {
    return res.status(401).json({ success: false, message: 'Invalid or expired token' });
  }
}

// Ensure default admin user exists
try {
  const adminCheck = db.prepare('SELECT id FROM admins WHERE username = ?').get('admin');
  if (!adminCheck) {
    const salt = bcrypt.genSaltSync(10);
    const hash = bcrypt.hashSync('admin123', salt);
    db.prepare('INSERT INTO admins (username, password_hash) VALUES (?, ?)').run('admin', hash);
    console.log('Default admin initialized (user: admin)');
  }
} catch (e) {
  console.error('Admin init check error:', e.message);
}

// ==========================================
// 1. ANDROID CLIENT ENDPOINTS
// ==========================================

/**
 * POST /api/license/activate
 * Body: { licenseKey, deviceId, customerName, deviceModel }
 */
app.post('/api/license/activate', (req, res) => {
  const { licenseKey, deviceId, customerName, deviceModel } = req.body;
  const ip = req.ip || req.connection.remoteAddress;

  if (!licenseKey || !deviceId) {
    return res.status(400).json({ status: 'ERROR', message: 'License key and Device ID are required.' });
  }

  const cleanKey = licenseKey.trim().toUpperCase();
  const cleanDevice = deviceId.trim();

  const license = db.prepare('SELECT * FROM licenses WHERE license_key = ?').get(cleanKey);

  if (!license) {
    logAction(null, cleanKey, 'REJECTED', cleanDevice, ip, 'Invalid license key attempted');
    return res.status(404).json({ status: 'ERROR', message: 'License key not found.' });
  }

  if (license.status === 'BLOCKED') {
    logAction(license.id, cleanKey, 'REJECTED', cleanDevice, ip, 'Blocked license activation attempted');
    return res.status(403).json({ status: 'BLOCKED', message: 'This license has been blocked by administrator.' });
  }

  // Check device binding
  if (!license.device_id) {
    // First-time activation: Bind to this device
    const custName = customerName || license.customer_name || 'Valued Customer';
    const devModel = deviceModel || license.device_model || 'Android Device';

    db.prepare(`
      UPDATE licenses 
      SET status = 'ACTIVE', device_id = ?, device_model = ?, customer_name = ?, activated_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
      WHERE id = ?
    `).run(cleanDevice, devModel, custName, license.id);

    logAction(license.id, cleanKey, 'ACTIVATE', cleanDevice, ip, `Bound to ${devModel} for ${custName}`);

    return res.json({
      status: 'ACTIVE',
      licenseKey: cleanKey,
      customerName: custName,
      deviceId: cleanDevice,
      lifetime: true,
      message: 'License activated successfully for this device.'
    });
  }

  // If already bound to this EXACT device: Allow reactivation (reinstall / cache clear)
  if (license.device_id === cleanDevice) {
    db.prepare(`
      UPDATE licenses 
      SET status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP
      WHERE id = ?
    `).run(license.id);

    logAction(license.id, cleanKey, 'REACTIVATE', cleanDevice, ip, 'Same-device re-activation allowed');

    return res.json({
      status: 'ACTIVE',
      licenseKey: cleanKey,
      customerName: license.customer_name,
      deviceId: cleanDevice,
      lifetime: true,
      message: 'Same device re-activated successfully.'
    });
  }

  // Second device protection: Device ID does not match
  logAction(license.id, cleanKey, 'REJECTED', cleanDevice, ip, `Second device rejected. Bound to ${license.device_id}`);
  return res.status(403).json({
    status: 'DEVICE_LIMIT_REACHED',
    message: 'This license is already registered on another device. Please reset device from Admin panel to transfer.'
  });
});

/**
 * GET /api/license/verify
 * Query params: code or key, deviceId
 */
app.get('/api/license/verify', (req, res) => {
  const licenseKey = (req.query.code || req.query.key || '').trim().toUpperCase();
  const deviceId = (req.query.deviceId || '').trim();
  const ip = req.ip || req.connection.remoteAddress;

  if (!licenseKey) {
    return res.status(400).json({ status: 'ERROR', message: 'License key is required' });
  }

  const license = db.prepare('SELECT * FROM licenses WHERE license_key = ?').get(licenseKey);

  if (!license) {
    return res.status(404).json({ status: 'INVALID', message: 'License not found' });
  }

  if (license.status === 'BLOCKED') {
    return res.status(403).json({ status: 'BLOCKED', message: 'License is blocked' });
  }

  if (deviceId && license.device_id && license.device_id !== deviceId) {
    return res.status(403).json({ status: 'DEVICE_MISMATCH', message: 'License belongs to a different device' });
  }

  return res.json({
    status: license.status,
    licenseKey: license.license_key,
    customerName: license.customer_name,
    deviceId: license.device_id,
    lifetime: Boolean(license.is_lifetime)
  });
});

// ==========================================
// 2. OPERATIONS & ADMIN ENDPOINTS
// ==========================================

/**
 * POST /api/license/generate & POST /api/admin/licenses/generate
 */
function handleGenerate(req, res) {
  const { customerName, notes, maxDevices } = req.body || {};
  let key = generateLicenseCode();

  // Ensure key uniqueness
  while (db.prepare('SELECT id FROM licenses WHERE license_key = ?').get(key)) {
    key = generateLicenseCode();
  }

  const stmt = db.prepare(`
    INSERT INTO licenses (license_key, customer_name, status, notes, max_devices, is_lifetime)
    VALUES (?, ?, 'UNACTIVATED', ?, ?, 1)
  `);

  const info = stmt.run(key, customerName || '', notes || '', maxDevices || 1);
  const created = db.prepare('SELECT * FROM licenses WHERE id = ?').get(info.lastInsertRowid);

  logAction(created.id, key, 'GENERATE', null, req.ip, `Generated for ${customerName || 'N/A'}`);

  res.status(201).json({
    success: true,
    license: created,
    message: 'License generated successfully.'
  });
}

app.post('/api/license/generate', handleGenerate);
app.post('/api/admin/licenses/generate', handleGenerate);

/**
 * POST /api/license/block & POST /api/admin/licenses/:id/block
 */
function handleBlock(req, res) {
  const keyOrId = req.params.id || req.body.licenseKey || req.body.id;
  if (!keyOrId) return res.status(400).json({ success: false, message: 'License ID or key is required' });

  const license = db.prepare('SELECT * FROM licenses WHERE id = ? OR license_key = ?').get(keyOrId, keyOrId);
  if (!license) return res.status(404).json({ success: false, message: 'License not found' });

  db.prepare(`UPDATE licenses SET status = 'BLOCKED', updated_at = CURRENT_TIMESTAMP WHERE id = ?`).run(license.id);
  logAction(license.id, license.license_key, 'BLOCK', license.device_id, req.ip, 'License blocked by admin');

  res.json({ success: true, status: 'BLOCKED', message: `License ${license.license_key} has been blocked.` });
}

app.post('/api/license/block', handleBlock);
app.post('/api/admin/licenses/:id/block', handleBlock);

/**
 * POST /api/license/unblock & POST /api/admin/licenses/:id/unblock
 */
function handleUnblock(req, res) {
  const keyOrId = req.params.id || req.body.licenseKey || req.body.id;
  if (!keyOrId) return res.status(400).json({ success: false, message: 'License ID or key is required' });

  const license = db.prepare('SELECT * FROM licenses WHERE id = ? OR license_key = ?').get(keyOrId, keyOrId);
  if (!license) return res.status(404).json({ success: false, message: 'License not found' });

  const newStatus = license.device_id ? 'ACTIVE' : 'UNACTIVATED';
  db.prepare(`UPDATE licenses SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?`).run(newStatus, license.id);
  logAction(license.id, license.license_key, 'UNBLOCK', license.device_id, req.ip, 'License unblocked by admin');

  res.json({ success: true, status: newStatus, message: `License ${license.license_key} has been unblocked.` });
}

app.post('/api/license/unblock', handleUnblock);
app.post('/api/admin/licenses/:id/unblock', handleUnblock);

/**
 * POST /api/license/reset-device & POST /api/admin/licenses/:id/reset-device
 */
function handleResetDevice(req, res) {
  const keyOrId = req.params.id || req.body.licenseKey || req.body.id;
  if (!keyOrId) return res.status(400).json({ success: false, message: 'License ID or key is required' });

  const license = db.prepare('SELECT * FROM licenses WHERE id = ? OR license_key = ?').get(keyOrId, keyOrId);
  if (!license) return res.status(404).json({ success: false, message: 'License not found' });

  const oldDevice = license.device_id;
  db.prepare(`
    UPDATE licenses 
    SET device_id = NULL, device_model = NULL, status = 'UNACTIVATED', updated_at = CURRENT_TIMESTAMP 
    WHERE id = ?
  `).run(license.id);

  logAction(license.id, license.license_key, 'RESET_DEVICE', oldDevice, req.ip, `Device unlinked (was ${oldDevice})`);

  res.json({ success: true, message: `Device unlinked from ${license.license_key}. License is now available for new device.` });
}

app.post('/api/license/reset-device', handleResetDevice);
app.post('/api/admin/licenses/:id/reset-device', handleResetDevice);

// ==========================================
// 3. ADMIN PANEL API (Auth, Stats, Lists)
// ==========================================

// Admin Login
app.post('/api/admin/login', (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ success: false, message: 'Username and password required' });
  }

  const admin = db.prepare('SELECT * FROM admins WHERE username = ?').get(username);
  if (!admin || !bcrypt.compareSync(password, admin.password_hash)) {
    return res.status(401).json({ success: false, message: 'Invalid admin credentials' });
  }

  const token = jwt.sign({ id: admin.id, username: admin.username }, JWT_SECRET, { expiresIn: '7d' });
  res.json({ success: true, token, username: admin.username });
});

// Admin Stats
app.get('/api/admin/stats', (req, res) => {
  const total = db.prepare('SELECT COUNT(*) as count FROM licenses').get().count;
  const active = db.prepare("SELECT COUNT(*) as count FROM licenses WHERE status = 'ACTIVE'").get().count;
  const blocked = db.prepare("SELECT COUNT(*) as count FROM licenses WHERE status = 'BLOCKED'").get().count;
  const unactivated = db.prepare("SELECT COUNT(*) as count FROM licenses WHERE status = 'UNACTIVATED'").get().count;

  res.json({
    total,
    active,
    blocked,
    unactivated
  });
});

// Admin Licenses List
app.get('/api/admin/licenses', (req, res) => {
  const { status, search } = req.query;
  let query = 'SELECT * FROM licenses WHERE 1=1';
  const params = [];

  if (status && status !== 'ALL') {
    query += ' AND status = ?';
    params.push(status);
  }

  if (search) {
    query += ' AND (license_key LIKE ? OR customer_name LIKE ? OR device_id LIKE ? OR device_model LIKE ?)';
    const s = `%${search}%`;
    params.push(s, s, s, s);
  }

  query += ' ORDER BY id DESC';
  const licenses = db.prepare(query).all(...params);
  res.json({ success: true, licenses });
});

// Admin Logs
app.get('/api/admin/logs', (req, res) => {
  const logs = db.prepare('SELECT * FROM license_logs ORDER BY id DESC LIMIT 100').all();
  res.json({ success: true, logs });
});

// Catch-all for SPA UI
app.use((req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Start Server
if (require.main === module) {
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Aqeel Rider License Server & Admin Panel running on http://0.0.0.0:${PORT}`);
  });
}

module.exports = app;
