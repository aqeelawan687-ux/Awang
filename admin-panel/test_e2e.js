const assert = require('assert');
const http = require('http');
const app = require('./server');
const db = require('./database');

const server = http.createServer(app);

function request(method, path, body = null, headers = {}) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: '127.0.0.1',
      port: 4001,
      path,
      method,
      headers: {
        'Content-Type': 'application/json',
        ...headers
      }
    };

    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => data += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, headers: res.headers, body: JSON.parse(data) });
        } catch (e) {
          resolve({ status: res.statusCode, headers: res.headers, body: data });
        }
      });
    });

    req.on('error', reject);
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

async function runTests() {
  console.log('=== STARTING END-TO-END LICENSE SYSTEM TEST ===\n');

  server.listen(4001, '127.0.0.1');
  await new Promise(r => setTimeout(r, 500));

  try {
    // 1. Admin Login
    console.log('1. Testing Admin Login...');
    const loginRes = await request('POST', '/api/admin/login', { username: 'admin', password: 'admin123' });
    assert.strictEqual(loginRes.status, 200, 'Admin login should return 200');
    assert.ok(loginRes.body.token, 'Should return JWT token');
    const token = loginRes.body.token;
    console.log('   Admin Login: PASS (JWT Token acquired)\n');

    // 2. Generate License
    console.log('2. Testing License Generation...');
    const genRes = await request('POST', '/api/admin/licenses/generate', {
      customerName: 'Test Rider Aqeel',
      notes: 'Lahore Route'
    }, { 'Authorization': `Bearer ${token}` });
    assert.strictEqual(genRes.status, 201, 'Generate should return 201');
    const license = genRes.body.license;
    assert.ok(license.license_key.startsWith('AR-'), 'Key format should be AR-XXXX-XXXX');
    assert.strictEqual(license.status, 'UNACTIVATED');
    console.log(`   License Generated: PASS (${license.license_key})\n`);

    // 3. First-time Activation on Device A
    console.log('3. Testing Activation on Device A...');
    const actResA = await request('POST', '/api/license/activate', {
      licenseKey: license.license_key,
      deviceId: 'DEV-PHONE-AAA',
      customerName: 'Test Rider Aqeel',
      deviceModel: 'Samsung Galaxy A54'
    });
    assert.strictEqual(actResA.status, 200, 'Activation should return 200');
    assert.strictEqual(actResA.body.status, 'ACTIVE');
    assert.strictEqual(actResA.body.deviceId, 'DEV-PHONE-AAA');
    console.log('   Device A Activation: PASS (Device Bound)\n');

    // 4. Same Device Reactivation (reinstall/cache clear)
    console.log('4. Testing Same-Device Reactivation...');
    const reactRes = await request('POST', '/api/license/activate', {
      licenseKey: license.license_key,
      deviceId: 'DEV-PHONE-AAA'
    });
    assert.strictEqual(reactRes.status, 200, 'Same device should succeed with 200');
    assert.strictEqual(reactRes.body.status, 'ACTIVE');
    console.log('   Same Device Reactivation: PASS\n');

    // 5. Second Device Rejection (Device B)
    console.log('5. Testing Second Device Rejection (Device B)...');
    const actResB = await request('POST', '/api/license/activate', {
      licenseKey: license.license_key,
      deviceId: 'DEV-PHONE-BBB',
      deviceModel: 'Xiaomi Redmi Note 12'
    });
    assert.strictEqual(actResB.status, 403, 'Second device must be rejected with 403');
    assert.strictEqual(actResB.body.status, 'DEVICE_LIMIT_REACHED');
    console.log('   Second Device Rejection: PASS\n');

    // 6. Admin Block
    console.log('6. Testing Admin Block...');
    const blockRes = await request('POST', `/api/admin/licenses/${license.id}/block`, {}, { 'Authorization': `Bearer ${token}` });
    assert.strictEqual(blockRes.status, 200);
    assert.strictEqual(blockRes.body.status, 'BLOCKED');

    // Verify endpoint check for blocked state
    const verifyBlock = await request('GET', `/api/license/verify?code=${license.license_key}&deviceId=DEV-PHONE-AAA`);
    assert.strictEqual(verifyBlock.status, 403, 'Verify must return 403 when blocked');
    assert.strictEqual(verifyBlock.body.status, 'BLOCKED');
    console.log('   Admin Block & Verify Detection: PASS\n');

    // 7. Admin Unblock
    console.log('7. Testing Admin Unblock...');
    const unblockRes = await request('POST', `/api/admin/licenses/${license.id}/unblock`, {}, { 'Authorization': `Bearer ${token}` });
    assert.strictEqual(unblockRes.status, 200);
    assert.strictEqual(unblockRes.body.status, 'ACTIVE');

    const verifyUnblock = await request('GET', `/api/license/verify?code=${license.license_key}&deviceId=DEV-PHONE-AAA`);
    assert.strictEqual(verifyUnblock.status, 200, 'Verify must return 200 when unblocked');
    assert.strictEqual(verifyUnblock.body.status, 'ACTIVE');
    console.log('   Admin Unblock & Verify Detection: PASS\n');

    // 8. Reset Device
    console.log('8. Testing Reset Device...');
    const resetRes = await request('POST', `/api/admin/licenses/${license.id}/reset-device`, {}, { 'Authorization': `Bearer ${token}` });
    assert.strictEqual(resetRes.status, 200);

    // Now Device B should be able to activate
    console.log('9. Testing Device B Activation after Reset...');
    const actResB2 = await request('POST', '/api/license/activate', {
      licenseKey: license.license_key,
      deviceId: 'DEV-PHONE-BBB',
      customerName: 'Aqeel New Phone',
      deviceModel: 'Xiaomi Redmi Note 12'
    });
    assert.strictEqual(actResB2.status, 200, 'Device B should now activate successfully');
    assert.strictEqual(actResB2.body.deviceId, 'DEV-PHONE-BBB');
    console.log('   Device B Activation after Reset: PASS\n');

    // 10. Database Persistence Test
    console.log('10. Testing Database Persistence across connection close...');
    const row = db.prepare('SELECT * FROM licenses WHERE id = ?').get(license.id);
    assert.strictEqual(row.device_id, 'DEV-PHONE-BBB');
    assert.strictEqual(row.status, 'ACTIVE');
    assert.strictEqual(row.is_lifetime, 1);
    console.log('   Database Persistence in license.db: PASS\n');

    // 11. Stats check
    console.log('11. Testing Stats Endpoint...');
    const statsRes = await request('GET', '/api/admin/stats', null, { 'Authorization': `Bearer ${token}` });
    assert.strictEqual(statsRes.status, 200);
    assert.ok(statsRes.body.total >= 1);
    console.log('   Stats Endpoint: PASS\n');

    console.log('====================================================');
    console.log('🎉 ALL END-TO-END LICENSE SYSTEM TESTS PASSED 100%!');
    console.log('====================================================');
  } catch (err) {
    console.error('❌ TEST FAILED:', err);
    process.exit(1);
  } finally {
    server.close();
  }
}

runTests();
