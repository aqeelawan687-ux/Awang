let token = localStorage.getItem('ar_admin_token') || null;
let currentTab = 'licenses';

// DOM Elements
const authScreen = document.getElementById('auth-screen');
const appScreen = document.getElementById('app-screen');
const loginForm = document.getElementById('login-form');
const loginError = document.getElementById('login-error');
const logoutBtn = document.getElementById('logout-btn');
const refreshBtn = document.getElementById('refresh-btn');
const openGenerateBtn = document.getElementById('open-generate-btn');
const generateModal = document.getElementById('generate-modal');
const generateForm = document.getElementById('generate-form');
const searchInput = document.getElementById('search-input');
const statusFilter = document.getElementById('status-filter');
const toastEl = document.getElementById('toast');

// Initialize
document.addEventListener('DOMContentLoaded', () => {
  if (token) {
    showApp();
  } else {
    showAuth();
  }
  setupEventListeners();
});

function showAuth() {
  authScreen.style.display = 'flex';
  appScreen.style.display = 'none';
}

function showApp() {
  authScreen.style.display = 'none';
  appScreen.style.display = 'flex';
  loadStats();
  loadLicenses();
  loadLogs();
}

function showToast(msg, isError = false) {
  toastEl.textContent = msg;
  toastEl.style.borderColor = isError ? '#ef4444' : '#10b981';
  toastEl.style.display = 'block';
  setTimeout(() => {
    toastEl.style.display = 'none';
  }, 3000);
}

function setupEventListeners() {
  // Login
  loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    loginError.style.display = 'none';
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();

    try {
      const res = await fetch('/api/admin/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      const data = await res.json();
      if (res.ok && data.token) {
        token = data.token;
        localStorage.setItem('ar_admin_token', token);
        document.getElementById('display-admin-name').textContent = data.username;
        showApp();
      } else {
        loginError.textContent = data.message || 'Invalid credentials';
        loginError.style.display = 'block';
      }
    } catch (err) {
      loginError.textContent = 'Connection failed: ' + err.message;
      loginError.style.display = 'block';
    }
  });

  // Logout
  logoutBtn.addEventListener('click', () => {
    token = null;
    localStorage.removeItem('ar_admin_token');
    showAuth();
  });

  // Navigation
  document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', () => {
      document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
      item.classList.add('active');
      currentTab = item.dataset.tab;
      document.querySelectorAll('.tab-pane').forEach(p => p.style.display = 'none');
      document.getElementById(`tab-${currentTab}`).style.display = 'block';
      document.getElementById('page-title').textContent = currentTab === 'licenses' ? 'Licenses Dashboard' : 'System Audit Logs';
    });
  });

  // Refresh
  refreshBtn.addEventListener('click', () => {
    loadStats();
    if (currentTab === 'licenses') loadLicenses();
    else loadLogs();
    showToast('Data refreshed');
  });

  // Search & Filter
  searchInput.addEventListener('input', debounce(() => loadLicenses(), 300));
  statusFilter.addEventListener('change', () => loadLicenses());

  // Generate Modal
  openGenerateBtn.addEventListener('click', () => {
    generateModal.style.display = 'flex';
  });

  document.querySelectorAll('.close-modal-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      generateModal.style.display = 'none';
    });
  });

  generateForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const customerName = document.getElementById('gen-customer').value.trim();
    const notes = document.getElementById('gen-notes').value.trim();

    try {
      const res = await fetch('/api/admin/licenses/generate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ customerName, notes, maxDevices: 1 })
      });
      const data = await res.json();
      if (res.ok && data.success) {
        generateModal.style.display = 'none';
        generateForm.reset();
        showToast(`License Generated: ${data.license.license_key}`);
        loadStats();
        loadLicenses();
        loadLogs();
      } else {
        showToast(data.message || 'Failed to generate', true);
      }
    } catch (err) {
      showToast('Error: ' + err.message, true);
    }
  });
}

// Fetch Stats
async function loadStats() {
  try {
    const res = await fetch('/api/admin/stats', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    if (res.ok) {
      const data = await res.json();
      document.getElementById('stat-total').textContent = data.total || 0;
      document.getElementById('stat-active').textContent = data.active || 0;
      document.getElementById('stat-unactivated').textContent = data.unactivated || 0;
      document.getElementById('stat-blocked').textContent = data.blocked || 0;
    }
  } catch (err) {
    console.error('Stats error:', err);
  }
}

// Fetch Licenses
async function loadLicenses() {
  const tbody = document.getElementById('licenses-tbody');
  const search = searchInput.value.trim();
  const status = statusFilter.value;

  try {
    const query = new URLSearchParams({ search, status });
    const res = await fetch(`/api/admin/licenses?${query}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (res.ok) {
      const data = await res.json();
      renderLicensesTable(data.licenses || []);
    } else {
      tbody.innerHTML = `<tr><td colspan="7" class="text-center">Failed to load licenses</td></tr>`;
    }
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center">Error connecting: ${err.message}</td></tr>`;
  }
}

function renderLicensesTable(licenses) {
  const tbody = document.getElementById('licenses-tbody');
  if (!licenses.length) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center" style="padding:32px;color:var(--text-muted);">No licenses found.</td></tr>`;
    return;
  }

  tbody.innerHTML = licenses.map(lic => {
    let statusBadge = `<span class="badge badge-unactivated">UNACTIVATED</span>`;
    if (lic.status === 'ACTIVE') statusBadge = `<span class="badge badge-active">ACTIVE</span>`;
    if (lic.status === 'BLOCKED') statusBadge = `<span class="badge badge-blocked">BLOCKED</span>`;

    const deviceText = lic.device_id 
      ? `<span class="device-code">${lic.device_id}</span>` 
      : `<span class="text-muted">None (Available)</span>`;

    const modelText = lic.device_model || `<span class="text-muted">-</span>`;

    return `
      <tr>
        <td>
          <div class="license-code">
            ${lic.license_key}
            <button class="btn btn-sm btn-secondary" onclick="copyText('${lic.license_key}')" title="Copy Key">📋</button>
          </div>
        </td>
        <td><strong>${escapeHtml(lic.customer_name || 'N/A')}</strong><br><small class="text-muted">${escapeHtml(lic.notes || '')}</small></td>
        <td>${statusBadge}</td>
        <td>${deviceText}</td>
        <td>${modelText}</td>
        <td><small class="text-muted">Created: ${formatDate(lic.created_at)}<br>Activated: ${formatDate(lic.activated_at)}</small></td>
        <td>
          <div class="action-btns">
            ${lic.status === 'BLOCKED'
              ? `<button class="btn btn-sm btn-action-unblock" onclick="unblockLicense(${lic.id})">Unblock</button>`
              : `<button class="btn btn-sm btn-action-block" onclick="blockLicense(${lic.id})">Block</button>`
            }
            ${lic.device_id
              ? `<button class="btn btn-sm btn-action-reset" onclick="resetDevice(${lic.id})" title="Clear device binding">Reset Device</button>`
              : ''
            }
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

// Fetch Logs
async function loadLogs() {
  const tbody = document.getElementById('logs-tbody');
  try {
    const res = await fetch('/api/admin/logs', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    if (res.ok) {
      const data = await res.json();
      renderLogsTable(data.logs || []);
    }
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center">Error: ${err.message}</td></tr>`;
  }
}

function renderLogsTable(logs) {
  const tbody = document.getElementById('logs-tbody');
  if (!logs.length) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center">No logs recorded yet.</td></tr>`;
    return;
  }

  tbody.innerHTML = logs.map(l => `
    <tr>
      <td><small class="text-muted">${formatDate(l.created_at)}</small></td>
      <td><code>${l.license_key}</code></td>
      <td><strong>${l.action}</strong></td>
      <td><small class="device-code">${l.device_id || '-'}</small></td>
      <td><small class="text-muted">${l.ip_address || '-'}</small></td>
      <td><small>${escapeHtml(l.details || '')}</small></td>
    </tr>
  `).join('');
}

// License Actions
window.blockLicense = async function(id) {
  if (!confirm('Are you sure you want to BLOCK this license? The rider app will immediately show blocked access.')) return;
  try {
    const res = await fetch(`/api/admin/licenses/${id}/block`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showToast(data.message);
      loadStats();
      loadLicenses();
      loadLogs();
    } else {
      showToast(data.message || 'Block failed', true);
    }
  } catch (err) {
    showToast('Error: ' + err.message, true);
  }
};

window.unblockLicense = async function(id) {
  try {
    const res = await fetch(`/api/admin/licenses/${id}/unblock`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showToast(data.message);
      loadStats();
      loadLicenses();
      loadLogs();
    } else {
      showToast(data.message || 'Unblock failed', true);
    }
  } catch (err) {
    showToast('Error: ' + err.message, true);
  }
};

window.resetDevice = async function(id) {
  if (!confirm('Reset device binding? This unlinks the current device, allowing the customer to activate on a new phone.')) return;
  try {
    const res = await fetch(`/api/admin/licenses/${id}/reset-device`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showToast(data.message);
      loadStats();
      loadLicenses();
      loadLogs();
    } else {
      showToast(data.message || 'Reset failed', true);
    }
  } catch (err) {
    showToast('Error: ' + err.message, true);
  }
};

window.copyText = function(text) {
  navigator.clipboard.writeText(text);
  showToast(`Copied ${text} to clipboard!`);
};

function formatDate(dStr) {
  if (!dStr) return '-';
  const d = new Date(dStr);
  return isNaN(d.getTime()) ? dStr : d.toLocaleString();
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function debounce(func, wait) {
  let timeout;
  return function(...args) {
    clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(this, args), wait);
  };
}
