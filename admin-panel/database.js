const Database = require('better-sqlite3');
const path = require('path');

const dbPath = process.env.DB_PATH || path.join(__dirname, 'license.db');
const db = new Database(dbPath);

// Enable WAL mode for high concurrency
db.pragma('journal_mode = WAL');

// Initialize database schema
db.exec(`
  CREATE TABLE IF NOT EXISTS admins (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
  );

  CREATE TABLE IF NOT EXISTS licenses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    license_key TEXT UNIQUE NOT NULL,
    customer_name TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'UNACTIVATED', -- UNACTIVATED, ACTIVE, BLOCKED
    device_id TEXT DEFAULT NULL,
    device_model TEXT DEFAULT NULL,
    max_devices INTEGER NOT NULL DEFAULT 1,
    is_lifetime INTEGER NOT NULL DEFAULT 1,
    notes TEXT DEFAULT '',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    activated_at DATETIME DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
  );

  CREATE TABLE IF NOT EXISTS license_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    license_id INTEGER,
    license_key TEXT NOT NULL,
    action TEXT NOT NULL, -- GENERATE, ACTIVATE, REACTIVATE, REJECTED, BLOCK, UNBLOCK, RESET_DEVICE
    device_id TEXT DEFAULT NULL,
    ip_address TEXT DEFAULT NULL,
    details TEXT DEFAULT '',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (license_id) REFERENCES licenses(id) ON DELETE CASCADE
  );

  CREATE INDEX IF NOT EXISTS idx_licenses_key ON licenses(license_key);
  CREATE INDEX IF NOT EXISTS idx_licenses_device ON licenses(device_id);
`);

module.exports = db;
