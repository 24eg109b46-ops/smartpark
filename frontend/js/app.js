/**
 * SmartPark Client Application
 * Communicates with Spring Boot REST Backend (Local or Render)
 */

// Configurable Backend API Endpoint
let API_BASE = localStorage.getItem('smartpark_api_base') || 'http://localhost:8080/api';
let currentUser = JSON.parse(localStorage.getItem('smartpark_user') || 'null');
let charts = {};

document.addEventListener('DOMContentLoaded', () => {
    initApiConfig();
    updateAuthUI();
    showView('home');
    loadHomeStats();
});

// Configure API Target (Local or Render)
function initApiConfig() {
    const input = document.getElementById('apiBaseInput');
    if (input) {
        input.value = API_BASE;
    }
}

function saveApiConfig() {
    const input = document.getElementById('apiBaseInput');
    if (input && input.value.trim()) {
        API_BASE = input.value.trim().replace(/\/+$/, '');
        localStorage.setItem('smartpark_api_base', API_BASE);
        showToast('Backend endpoint updated: ' + API_BASE, 'success');
        loadHomeStats();
    }
}

// Navigation View Switcher
function showView(viewId) {
    document.querySelectorAll('.section-view').forEach(el => el.classList.remove('active'));
    const target = document.getElementById('view-' + viewId);
    if (target) {
        target.classList.add('active');
        window.scrollTo(0, 0);
    }

    if (viewId === 'home') loadHomeStats();
    if (viewId === 'driver-search') loadSearchZones();
    if (viewId === 'driver-bookings') loadDriverBookings();
    if (viewId === 'driver-vehicles') loadDriverVehicles();
    if (viewId === 'operator-grid') loadOperatorGrid();
    if (viewId === 'operator-bookings') loadOperatorBookings();
    if (viewId === 'admin-dashboard') loadAdminStats();
    if (viewId === 'admin-users') loadAdminUsers();
    if (viewId === 'admin-pricing') loadAdminPricing();
}

// User Authentication State
function updateAuthUI() {
    const authNav = document.getElementById('authNav');
    const roleBadge = document.getElementById('userRoleBadge');
    const userNameSpan = document.getElementById('userNameSpan');

    if (currentUser) {
        document.querySelectorAll('.auth-only').forEach(el => el.classList.remove('d-none'));
        document.querySelectorAll('.guest-only').forEach(el => el.classList.add('d-none'));
        if (roleBadge) roleBadge.textContent = currentUser.role.replace('ROLE_', '');
        if (userNameSpan) userNameSpan.textContent = currentUser.name;

        // Role-based visibility
        document.querySelectorAll('.role-driver').forEach(el => {
            el.classList.toggle('d-none', currentUser.role !== 'ROLE_DRIVER');
        });
        document.querySelectorAll('.role-operator').forEach(el => {
            el.classList.toggle('d-none', currentUser.role !== 'ROLE_OPERATOR');
        });
        document.querySelectorAll('.role-admin').forEach(el => {
            el.classList.toggle('d-none', currentUser.role !== 'ROLE_ADMIN');
        });
    } else {
        document.querySelectorAll('.auth-only').forEach(el => el.classList.add('d-none'));
        document.querySelectorAll('.guest-only').forEach(el => el.classList.remove('d-none'));
    }
}

async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;

    try {
        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Login failed');

        currentUser = data;
        localStorage.setItem('smartpark_user', JSON.stringify(currentUser));
        updateAuthUI();
        showToast('Signed in successfully as ' + data.name, 'success');

        if (data.role === 'ROLE_DRIVER') showView('driver-dashboard');
        else if (data.role === 'ROLE_OPERATOR') showView('operator-grid');
        else if (data.role === 'ROLE_ADMIN') showView('admin-dashboard');
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

function quickFill(email, pass) {
    document.getElementById('loginEmail').value = email;
    document.getElementById('loginPassword').value = pass;
}

function handleLogout() {
    currentUser = null;
    localStorage.removeItem('smartpark_user');
    updateAuthUI();
    showToast('Signed out', 'info');
    showView('home');
}

async function handleRegister(e) {
    e.preventDefault();
    const name = document.getElementById('regName').value;
    const email = document.getElementById('regEmail').value;
    const phone = document.getElementById('regPhone').value;
    const password = document.getElementById('regPassword').value;
    const confirmPassword = document.getElementById('regConfirmPassword').value;

    try {
        const res = await fetch(`${API_BASE}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, phone, password, confirmPassword })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Registration failed');

        showToast('Registration successful! Please sign in.', 'success');
        showView('login');
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

// ----------------------------------------------------
// Home Metrics & Public Tariffs
// ----------------------------------------------------
async function loadHomeStats() {
    try {
        const res = await fetch(`${API_BASE}/admin/stats`);
        if (res.ok) {
            const data = await res.json();
            document.getElementById('statTotalSlots').textContent = data.totalSlots || 47;
            document.getElementById('statAvailableSlots').textContent = data.availableSlots || 35;
            document.getElementById('statOccupiedSlots').textContent = data.occupiedSlots || 10;
        }

        const priceRes = await fetch(`${API_BASE}/admin/pricing`);
        if (priceRes.ok) {
            const rules = await priceRes.json();
            const container = document.getElementById('publicTariffTable');
            if (container) {
                container.innerHTML = rules.map(r => `
                    <tr>
                        <td><span class="badge bg-secondary">${r.vehicleType}</span></td>
                        <td class="fw-semibold">${r.zoneName || 'Global'}</td>
                        <td class="text-success fw-bold">$${r.ratePerHour} / hr</td>
                        <td>$${r.minCharge}</td>
                        <td>${r.freeMinutes} mins</td>
                    </tr>
                `).join('');
            }
        }
    } catch (err) {
        console.warn('Backend not responding yet on ' + API_BASE);
    }
}

// ----------------------------------------------------
// Driver Workflows
// ----------------------------------------------------
async function loadSearchZones() {
    try {
        const res = await fetch(`${API_BASE}/slots/zones`);
        const zones = await res.json();
        const select = document.getElementById('searchZoneSelect');
        select.innerHTML = '<option value="">All Zones</option>' + zones.map(z => `
            <option value="${z.id}">${z.name} (${z.location})</option>
        `).join('');
        searchAvailableSlots();
    } catch (err) {
        console.error(err);
    }
}

async function searchAvailableSlots() {
    const zoneId = document.getElementById('searchZoneSelect').value;
    const vehicleType = document.getElementById('searchTypeSelect').value;
    const duration = document.getElementById('searchDuration').value || 2;

    const query = new URLSearchParams();
    if (zoneId) query.append('zoneId', zoneId);
    if (vehicleType) query.append('vehicleType', vehicleType);
    query.append('durationHours', duration);

    try {
        const res = await fetch(`${API_BASE}/slots/search?${query.toString()}`);
        const slots = await res.json();
        const container = document.getElementById('searchResultsGrid');

        if (slots.length === 0) {
            container.innerHTML = `<div class="col-12 text-center py-5 text-muted">No vacant slots found for criteria.</div>`;
            return;
        }

        container.innerHTML = slots.map(s => `
            <div class="col-sm-6 col-md-4 col-lg-3">
                <div class="card p-3 text-center card-hover h-100 border-0 shadow-sm">
                    <div class="d-flex justify-content-between mb-2">
                        <span class="badge bg-success-subtle text-success">Vacant</span>
                        <span class="badge bg-light text-dark border">${s.vehicleType}</span>
                    </div>
                    <div class="h3 fw-extrabold text-primary mb-1">${s.slotNumber}</div>
                    <div class="fw-semibold small">${s.zoneName}</div>
                    <small class="text-muted">${s.zoneLocation}</small>
                    <button class="btn btn-accent btn-sm mt-3 w-100" onclick="openReserveModal(${s.id}, '${s.slotNumber}', '${s.zoneName}', '${s.vehicleType}')">
                        Reserve Bay
                    </button>
                </div>
            </div>
        `).join('');
    } catch (err) {
        showToast('Error searching slots: ' + err.message, 'danger');
    }
}

async function openReserveModal(slotId, slotNumber, zoneName, vehicleType) {
    if (!currentUser) {
        showToast('Please sign in as a driver first', 'warning');
        showView('login');
        return;
    }

    document.getElementById('modalSlotId').value = slotId;
    document.getElementById('modalSlotDetails').textContent = `${slotNumber} in ${zoneName} (${vehicleType})`;

    // Load user vehicles
    try {
        const res = await fetch(`${API_BASE}/vehicles/user/${currentUser.id}`);
        const vehicles = await res.json();
        const vSelect = document.getElementById('modalVehicleSelect');

        const matching = vehicles.filter(v => v.vehicleType === vehicleType);
        if (matching.length === 0) {
            vSelect.innerHTML = `<option disabled selected>No ${vehicleType} registered in your garage</option>`;
        } else {
            vSelect.innerHTML = matching.map(v => `
                <option value="${v.id}">${v.vehicleNumber} (${v.brand || ''} ${v.model || ''})</option>
            `).join('');
        }

        const modal = new bootstrap.Modal(document.getElementById('reserveSlotModal'));
        modal.show();
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

async function submitReservation() {
    const slotId = document.getElementById('modalSlotId').value;
    const vehicleId = document.getElementById('modalVehicleSelect').value;
    const durationHours = document.getElementById('modalDuration').value || 2;
    const entryTime = new Date(Date.now() + 5 * 60000).toISOString().slice(0, 16);

    if (!vehicleId) {
        showToast('Please select a matching vehicle', 'warning');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/bookings?userId=${currentUser.id}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ slotId, vehicleId, durationHours, entryTime })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Failed to reserve');

        bootstrap.Modal.getInstance(document.getElementById('reserveSlotModal')).hide();
        showToast('Reservation confirmed! Code: ' + data.bookingCode, 'success');
        showBookingPass(data);
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

function showBookingPass(booking) {
    document.getElementById('passCode').textContent = booking.bookingCode;
    document.getElementById('passSlot').textContent = booking.slotNumber + ' (' + booking.zoneName + ')';
    document.getElementById('passVehicle').textContent = booking.vehicleNumber;
    document.getElementById('passAmount').textContent = '$' + booking.amount;
    document.getElementById('passStatus').textContent = booking.status;
    document.getElementById('passQrImage').src = booking.qrCode;

    const modal = new bootstrap.Modal(document.getElementById('passModal'));
    modal.show();
}

async function loadDriverBookings() {
    if (!currentUser) return;
    try {
        const res = await fetch(`${API_BASE}/bookings/user/${currentUser.id}`);
        const bookings = await res.json();
        const tbody = document.getElementById('driverBookingsTbody');

        if (bookings.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">No bookings yet.</td></tr>`;
            return;
        }

        tbody.innerHTML = bookings.map(b => `
            <tr>
                <td class="fw-bold font-monospace text-primary">${b.bookingCode}</td>
                <td>${b.vehicleNumber} <small class="text-muted">(${b.vehicleType})</small></td>
                <td><strong>${b.slotNumber}</strong> (${b.zoneName})</td>
                <td>${b.entryTime || '—'}</td>
                <td class="text-success fw-bold">$${b.amount}</td>
                <td><span class="badge ${b.status === 'ACTIVE' ? 'bg-danger' : (b.status === 'CONFIRMED' ? 'bg-warning text-dark' : 'bg-success')}">${b.status}</span></td>
            </tr>
        `).join('');
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

async function loadDriverVehicles() {
    if (!currentUser) return;
    try {
        const res = await fetch(`${API_BASE}/vehicles/user/${currentUser.id}`);
        const vehicles = await res.json();
        const container = document.getElementById('driverVehiclesGrid');

        if (vehicles.length === 0) {
            container.innerHTML = `<div class="col-12 text-center py-4 text-muted">No vehicles in garage.</div>`;
            return;
        }

        container.innerHTML = vehicles.map(v => `
            <div class="col-md-4">
                <div class="card p-3 border-0 shadow-sm">
                    <div class="d-flex justify-content-between mb-2">
                        <span class="badge bg-primary font-monospace">${v.vehicleNumber}</span>
                        <span class="badge bg-secondary">${v.vehicleType}</span>
                    </div>
                    <div class="fw-bold">${v.brand || ''} ${v.model || ''}</div>
                </div>
            </div>
        `).join('');
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

async function submitAddVehicle(e) {
    e.preventDefault();
    if (!currentUser) return;

    const vehicleNumber = document.getElementById('vehPlate').value;
    const vehicleType = document.getElementById('vehType').value;
    const brand = document.getElementById('vehBrand').value;
    const model = document.getElementById('vehModel').value;

    try {
        const res = await fetch(`${API_BASE}/vehicles/user/${currentUser.id}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ vehicleNumber, vehicleType, brand, model })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Failed to add vehicle');

        showToast('Vehicle registered!', 'success');
        bootstrap.Modal.getInstance(document.getElementById('addVehicleModal')).hide();
        loadDriverVehicles();
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

// ----------------------------------------------------
// Operator Workflows
// ----------------------------------------------------
async function loadOperatorGrid() {
    try {
        const zonesRes = await fetch(`${API_BASE}/slots/zones`);
        const zones = await zonesRes.json();
        const zoneSelect = document.getElementById('opZoneSelect');

        zoneSelect.innerHTML = zones.map(z => `
            <option value="${z.id}">${z.name}</option>
        `).join('');

        if (zones.length > 0) {
            refreshOpSlots(zones[0].id);
        }
    } catch (err) {
        console.error(err);
    }
}

async function refreshOpSlots(zoneId) {
    zoneId = zoneId || document.getElementById('opZoneSelect').value;
    try {
        const res = await fetch(`${API_BASE}/slots/zone/${zoneId}`);
        const slots = await res.json();
        const container = document.getElementById('operatorGridContainer');

        container.innerHTML = slots.map(s => `
            <div class="col">
                <div class="slot-box ${s.status}">
                    <div class="d-flex justify-content-between small mb-1">
                        <span style="font-size:0.65rem;">${s.vehicleType}</span>
                    </div>
                    <div class="h5 fw-bold mb-1">${s.slotNumber}</div>
                    <span class="badge w-100 ${s.status === 'AVAILABLE' ? 'bg-success' : (s.status === 'RESERVED' ? 'bg-warning text-dark' : (s.status === 'OCCUPIED' ? 'bg-danger' : 'bg-secondary'))}" style="font-size:0.65rem;">
                        ${s.status}
                    </span>
                </div>
            </div>
        `).join('');
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

async function loadOperatorBookings() {
    try {
        const res = await fetch(`${API_BASE}/bookings`);
        const bookings = await res.json();
        const tbody = document.getElementById('opBookingsTbody');

        tbody.innerHTML = bookings.map(b => `
            <tr>
                <td class="fw-bold font-monospace text-primary">${b.bookingCode}</td>
                <td>${b.userName}<br><small class="text-muted">${b.vehicleNumber}</small></td>
                <td><strong>${b.slotNumber}</strong> (${b.zoneName})</td>
                <td><span class="badge ${b.status === 'ACTIVE' ? 'bg-danger' : (b.status === 'CONFIRMED' ? 'bg-warning text-dark' : 'bg-success')}">${b.status}</span></td>
                <td>$${b.amount}</td>
                <td>
                    ${b.status === 'CONFIRMED' ? `<button class="btn btn-sm btn-success py-0" onclick="opCheckIn(${b.id})">Check-In</button>` : ''}
                    ${b.status === 'ACTIVE' ? `<button class="btn btn-sm btn-danger py-0" onclick="opCheckOut(${b.id})">Check-Out</button>` : ''}
                </td>
            </tr>
        `).join('');
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

async function opCheckIn(bookingId) {
    try {
        const res = await fetch(`${API_BASE}/bookings/${bookingId}/check-in`, { method: 'POST' });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);
        showToast('Check-in confirmed!', 'success');
        loadOperatorBookings();
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

async function opCheckOut(bookingId) {
    try {
        const res = await fetch(`${API_BASE}/bookings/${bookingId}/check-out`, { method: 'POST' });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);
        showToast('Check-out completed! Amount: $' + data.booking.amount, 'info');
        loadOperatorBookings();
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

// ----------------------------------------------------
// Admin Workflows & Chart.js Analytics
// ----------------------------------------------------
async function loadAdminStats() {
    try {
        const res = await fetch(`${API_BASE}/admin/stats`);
        const stats = await res.json();

        document.getElementById('adminTotalRev').textContent = '$' + (stats.totalRevenue || 0);
        document.getElementById('adminTodayRev').textContent = '$' + (stats.todayRevenue || 0);
        document.getElementById('adminTodayBookings').textContent = stats.todayBookings || 0;
        document.getElementById('adminTotalUsers').textContent = stats.totalUsers || 0;

        renderAdminCharts(stats);
    } catch (err) {
        showToast('Failed to load admin metrics: ' + err.message, 'danger');
    }
}

function renderAdminCharts(stats) {
    // 1. Revenue Chart
    const revCtx = document.getElementById('chartRevenue');
    if (revCtx) {
        if (charts.rev) charts.rev.destroy();
        charts.rev = new Chart(revCtx, {
            type: 'bar',
            data: {
                labels: Object.keys(stats.revenueLast7Days || {}),
                datasets: [{
                    label: 'Revenue ($)',
                    data: Object.values(stats.revenueLast7Days || {}),
                    backgroundColor: '#10b981',
                    borderRadius: 6
                }]
            },
            options: { responsive: true, plugins: { legend: { display: false } } }
        });
    }

    // 2. Occupancy Doughnut
    const occCtx = document.getElementById('chartOccupancy');
    if (occCtx) {
        if (charts.occ) charts.occ.destroy();
        charts.occ = new Chart(occCtx, {
            type: 'doughnut',
            data: {
                labels: Object.keys(stats.occupancyByZone || {}),
                datasets: [{
                    data: Object.values(stats.occupancyByZone || {}),
                    backgroundColor: ['#1e3a8a', '#10b981', '#f59e0b', '#8b5cf6']
                }]
            },
            options: { responsive: true, plugins: { legend: { position: 'bottom' } } }
        });
    }
}

async function loadAdminUsers() {
    try {
        const res = await fetch(`${API_BASE}/admin/users`);
        const users = await res.json();
        const tbody = document.getElementById('adminUsersTbody');
        tbody.innerHTML = users.map(u => `
            <tr>
                <td class="fw-bold">${u.name}</td>
                <td>${u.email}</td>
                <td><span class="badge ${u.role === 'ROLE_ADMIN' ? 'bg-danger' : (u.role === 'ROLE_OPERATOR' ? 'bg-warning text-dark' : 'bg-success')}">${u.role}</span></td>
                <td><span class="badge ${u.status === 'ACTIVE' ? 'bg-success' : 'bg-danger'}">${u.status}</span></td>
                <td>
                    ${u.role !== 'ROLE_ADMIN' ? `<button class="btn btn-sm btn-outline-secondary py-0" onclick="toggleUser(${u.id})">${u.status === 'ACTIVE' ? 'Block' : 'Unblock'}</button>` : ''}
                </td>
            </tr>
        `).join('');
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

async function toggleUser(id) {
    try {
        const res = await fetch(`${API_BASE}/admin/users/${id}/toggle-status`, { method: 'PATCH' });
        const data = await res.json();
        showToast('Status updated to ' + data.status, 'success');
        loadAdminUsers();
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

async function downloadCsvReport() {
    window.location.href = `${API_BASE}/admin/reports/csv`;
}

// ----------------------------------------------------
// UI Notification Toast
// ----------------------------------------------------
function showToast(message, type = 'primary') {
    const toastEl = document.getElementById('appToast');
    const toastBody = document.getElementById('toastBody');
    if (toastEl && toastBody) {
        toastEl.className = `toast align-items-center text-bg-${type} border-0`;
        toastBody.textContent = message;
        const toast = new bootstrap.Toast(toastEl);
        toast.show();
    }
}
