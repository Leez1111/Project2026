/**
 * ============================================================
 * NAVIGATION & BACKEND DATA CONTROLLER
 * Handles: sidebar drawer, overlay, bottom-nav, active links,
 * user session display, and live backend API data binding
 * ============================================================
 */
document.addEventListener('DOMContentLoaded', () => {

  const sidebar = document.querySelector('.sidebar');
  const menuBtn = document.querySelector('.menu-btn');
  const overlay = document.querySelector('.sidebar-overlay');

  // Centralised open/close with body scroll lock
  function openSidebar() {
    sidebar?.classList.add('open');
    overlay?.classList.add('show');
    document.body.style.overflow = 'hidden';
  }
  function closeSidebar() {
    sidebar?.classList.remove('open');
    overlay?.classList.remove('show');
    document.body.style.overflow = '';
  }

  // Hamburger toggles drawer (tablet only)
  menuBtn?.addEventListener('click', () => {
    sidebar?.classList.contains('open') ? closeSidebar() : openSidebar();
  });

  // Overlay click closes drawer
  overlay?.addEventListener('click', closeSidebar);

  // Auto-close drawer after link tap on mobile/tablet & clear session on Logout
  document.querySelectorAll('.sidebar a').forEach(a => {
    a.addEventListener('click', () => {
      if (window.innerWidth < 1024) closeSidebar();
      const href = a.getAttribute('href');
      if (href && href.includes('index.html')) {
        localStorage.removeItem('attendify_user');
      }
    });
  });

  // Highlight active link in BOTH sidebar and bottom nav
  const path = window.location.pathname;
  document
    .querySelectorAll('.sidebar a, .bottom-nav a')
    .forEach(a => {
      const href = a.getAttribute('href');
      if (href && path.endsWith(href)) a.classList.add('active');
    });

  // Auto-close drawer when resizing up to desktop
  window.addEventListener('resize', () => {
    if (window.innerWidth >= 1024) closeSidebar();
  });

  // Populate logged-in user info
  const savedUser = JSON.parse(localStorage.getItem('attendify_user') || 'null');
  const userName = savedUser ? savedUser.name : 'Lee Zheng Shen';
  const initials = userName
    .split(' ')
    .map(w => w[0])
    .join('')
    .substring(0, 2)
    .toUpperCase();

  setText('topbar-user-name', userName);
  setText('topbar-user-initials', initials || 'LZ');
  setText('sidebar-user-name', savedUser ? `${savedUser.name} (${savedUser.studentId})` : 'Lee Zheng Shen (STU001)');

  // Load backend data for active page
  initStudentPages(savedUser);
  initLecturerPages();
  initAdminPages();
});

// ==================== STUDENT PAGES ====================
async function initStudentPages(user) {
  const studentId = user?.studentId || 'STU001';

  // Student Dashboard
  const todayClassesBody = document.getElementById('today-classes-body');
  if (todayClassesBody) {
    try {
      const [roomsRes, attRes] = await Promise.all([
        fetch('/api/classrooms'),
        fetch(`/api/attendance?studentId=${encodeURIComponent(studentId)}`)
      ]);
      const rooms = await roomsRes.json();
      const records = await attRes.json();

      const presentCount = records.filter(r => r.status === 'PRESENT').length;
      const totalClasses = Math.max(rooms.length, 1);
      const pct = Math.min(100, Math.round((presentCount / totalClasses) * 100));

      setText('stat-overall', `${pct}%`);
      setText('stat-classes-today', rooms.length);
      setText('stat-present', presentCount);
      setText('stat-pending', Math.max(0, rooms.length - presentCount));

      if (rooms.length > 0) {
        todayClassesBody.innerHTML = rooms.map((r, idx) => {
          const checkedIn = records.some(rec => rec.roomCode === r.roomCode);
          return `
            <tr>
              <td data-label="Course"><strong>${r.roomCode}</strong> (Floor ${r.floorLevel})</td>
              <td data-label="Time">${idx === 0 ? '10:00 AM – 12:00 PM' : '02:00 PM – 04:00 PM'}</td>
              <td data-label="Status">
                <span class="badge ${checkedIn ? 'badge-present' : 'badge-late'}">
                  ${checkedIn ? 'Present' : 'Pending Check-in'}
                </span>
              </td>
            </tr>
          `;
        }).join('');
      }
    } catch (e) {
      console.error('Error loading student dashboard:', e);
    }
  }

  // Student Attendance History
  const recordsBody = document.getElementById('records-body');
  if (recordsBody) {
    try {
      const res = await fetch(`/api/attendance?studentId=${encodeURIComponent(studentId)}`);
      const records = await res.json();

      const present = records.filter(r => r.status === 'PRESENT').length;
      const late = records.filter(r => r.status === 'LATE').length;
      const absent = records.filter(r => r.status === 'ABSENT').length;
      const total = present + late + absent;

      setText('stat-present', present);
      setText('stat-late', late);
      setText('stat-absent', absent);
      setText('stat-percent', total > 0 ? `${Math.round((present / total) * 100)}%` : '100%');

      if (records.length > 0) {
        recordsBody.innerHTML = records.map(r => {
          const dt = r.timestamp ? new Date(r.timestamp) : new Date();
          const bioIcon = r.biometricType === 'FACE_ID' ? '😊 Face ID' : '👆 Fingerprint';
          return `
            <tr>
              <td data-label="Date">${dt.toLocaleDateString()}</td>
              <td data-label="Course"><strong>${r.roomCode}</strong> <span style="font-size:12px; color:var(--text-muted);">(${bioIcon})</span></td>
              <td data-label="Time">${dt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</td>
              <td data-label="Status"><span class="badge badge-present">${r.status}</span></td>
            </tr>
          `;
        }).join('');
      }
    } catch (e) {
      console.error('Error loading student history:', e);
    }
  }
}

// ==================== LECTURER PAGES ====================
async function initLecturerPages() {
  const coursesBody = document.getElementById('courses-body');
  if (coursesBody) {
    try {
      const [roomsRes, usersRes, attRes] = await Promise.all([
        fetch('/api/classrooms'),
        fetch('/api/users'),
        fetch('/api/attendance')
      ]);
      const rooms = await roomsRes.json();
      const users = await usersRes.json();
      const records = await attRes.json();
      const studentCount = users.filter(u => u.role === 'STUDENT').length;

      setText('stat-active', rooms.length);
      setText('stat-courses', rooms.length);
      setText('stat-students', studentCount);
      setText('stat-checked', records.length);

      if (rooms.length > 0) {
        coursesBody.innerHTML = rooms.map(r => `
          <tr>
            <td data-label="Course"><strong>${r.roomCode}</strong> (Level ${r.floorLevel})</td>
            <td data-label="Students">${studentCount} enrolled</td>
            <td data-label="Action"><a href="session.html?room=${encodeURIComponent(r.roomCode)}" class="btn btn-primary" style="padding:6px 12px; font-size:12px;">Monitor Session</a></td>
          </tr>
        `).join('');
      }
    } catch (e) {
      console.error('Error loading lecturer dashboard:', e);
    }
  }

  // Lecturer Live Session page
  const feedList = document.getElementById('feed-list');
  if (feedList) {
    const qrBox = document.getElementById('qr-box');
    const params = new URLSearchParams(window.location.search);
    const activeRoom = params.get('room') || 'LAB-2-01';
    if (qrBox) {
      qrBox.innerHTML = `
        <div style="text-align:center; padding:12px; background:#fff; border-radius:8px;">
          <div style="font-size:32px; margin-bottom:4px;">📍</div>
          <div style="font-weight:800; color:var(--text); font-size:16px;">${activeRoom}</div>
          <div style="font-size:11px; color:var(--text-muted); margin-top:4px;">GPS Geofence Active</div>
        </div>
      `;
    }
    setText('session-end', 'Active');
    setText('qr-timer', 'Live GPS');

    const loadCheckins = async () => {
      try {
        const [attRes, usersRes] = await Promise.all([
          fetch('/api/attendance'),
          fetch('/api/users')
        ]);
        const records = await attRes.json();
        const students = (await usersRes.json()).filter(u => u.role === 'STUDENT');
        setText('checkin-count', `${records.length} / ${students.length}`);

        if (records.length > 0) {
          feedList.innerHTML = records.map(r => {
            const dt = r.timestamp ? new Date(r.timestamp) : new Date();
            const bioIcon = r.biometricType === 'FACE_ID' ? '😊 Face ID' : '👆 Fingerprint';
            return `
              <li style="padding:12px; background:var(--bg); border-radius:var(--radius-sm); display:flex; justify-content:space-between; align-items:center;">
                <div>
                  <strong>${r.studentName || r.studentId}</strong>
                  <div style="font-size:12px; color:var(--text-muted);">${r.studentId} · ${r.roomCode} · ${bioIcon}</div>
                </div>
                <div style="text-align:right;">
                  <span class="badge badge-present">${r.status}</span>
                  <div style="font-size:11px; color:var(--text-muted); margin-top:2px;">${dt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</div>
                </div>
              </li>
            `;
          }).join('');
        }
      } catch (e) {
        console.error('Failed to load live check-ins:', e);
      }
    };

    loadCheckins();
    setInterval(loadCheckins, 5000);
  }
}

// ==================== ADMIN PAGES ====================
async function initAdminPages() {
  const statTotalUsers = document.getElementById('stat-total-users');
  if (statTotalUsers && document.getElementById('system-health-list')) {
    try {
      const [usersRes, roomsRes, attRes] = await Promise.all([
        fetch('/api/users'),
        fetch('/api/classrooms'),
        fetch('/api/attendance')
      ]);
      const users = await usersRes.json();
      const rooms = await roomsRes.json();
      const records = await attRes.json();

      setText('stat-total-users', users.length);
      setText('stat-students', users.filter(u => u.role === 'STUDENT').length);
      setText('stat-lecturers', users.filter(u => u.role === 'LECTURER').length);
      setText('stat-courses', rooms.length);

      document.getElementById('system-health-list').innerHTML = `
        <li style="display:flex; justify-content:space-between; padding:10px; background:var(--bg); border-radius:var(--radius-sm);">
          <span><span class="dot dot-green"></span>Spring Boot Backend API</span>
          <span class="badge badge-present">Online</span>
        </li>
        <li style="display:flex; justify-content:space-between; padding:10px; background:var(--bg); border-radius:var(--radius-sm);">
          <span><span class="dot dot-green"></span>Persistent Local Database (./data/attendifydb)</span>
          <span class="badge badge-present">Connected</span>
        </li>
        <li style="display:flex; justify-content:space-between; padding:10px; background:var(--bg); border-radius:var(--radius-sm);">
          <span><span class="dot dot-green"></span>Geofence + Biometric Service</span>
          <span class="badge badge-present">${rooms.length} Active Zones</span>
        </li>
      `;

      const recentList = document.getElementById('recent-activity-list');
      if (records.length > 0 && recentList) {
        recentList.innerHTML = records.slice(0, 5).map(r => `
          <li style="display:flex; justify-content:space-between; padding:10px; background:var(--bg); border-radius:var(--radius-sm);">
            <span><strong>${r.studentName}</strong> checked into <strong>${r.roomCode}</strong></span>
            <span class="badge badge-present">${r.status}</span>
          </li>
        `).join('');
      }
    } catch (e) {
      console.error('Error loading admin dashboard:', e);
    }
  }

  // Admin Users Page
  const usersBody = document.getElementById('users-body');
  if (usersBody) {
    const loadUsers = async () => {
      const res = await fetch('/api/users');
      const users = await res.json();
      if (users.length > 0) {
        usersBody.innerHTML = users.map(u => `
          <tr>
            <td data-label="Name"><strong>${u.name}</strong> <span style="color:var(--text-muted); font-size:12px;">(${u.studentId})</span></td>
            <td data-label="Role"><span class="badge badge-info">${u.role}</span></td>
            <td data-label="Email">${u.studentId.toLowerCase()}@campus.edu.my</td>
            <td data-label="Status"><span class="badge badge-present">Biometric Active</span></td>
          </tr>
        `).join('');
      }
    };
    loadUsers();

    const addUserBtn = document.getElementById('add-user-btn');
    if (addUserBtn) {
      addUserBtn.addEventListener('click', async () => {
        const studentId = prompt('Enter User/Student ID (e.g. STU003):');
        if (!studentId) return;
        const name = prompt('Enter Full Name:');
        if (!name) return;
        const role = (prompt('Enter Role (STUDENT, LECTURER, ADMIN):', 'STUDENT') || 'STUDENT').toUpperCase();
        await fetch('/api/users', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ studentId, name, role, password: '1234' })
        });
        loadUsers();
      });
    }
  }

  // Admin Geofence Page
  const locationsBody = document.getElementById('locations-body');
  if (locationsBody) {
    const loadLocations = async () => {
      const res = await fetch('/api/classrooms');
      const rooms = await res.json();
      if (rooms.length > 0) {
        locationsBody.innerHTML = rooms.map(r => `
          <tr style="cursor:pointer;" onclick="previewLocation('${r.roomCode}', ${r.latitude}, ${r.longitude}, ${r.allowedRadius})">
            <td data-label="Room">
              <strong>${r.roomCode}</strong>
              <div style="font-size:12px; color:var(--text-muted);">Floor ${r.floorLevel} · ${r.latitude.toFixed(5)}, ${r.longitude.toFixed(5)}</div>
            </td>
            <td data-label="Radius"><span class="badge badge-info">${r.allowedRadius}m</span></td>
            <td data-label="Action">
              <button class="btn btn-outline" style="padding:5px 10px; font-size:12px;" onclick="event.stopPropagation(); deleteLocation(${r.id})">Delete</button>
            </td>
          </tr>
        `).join('');
        previewLocation(rooms[0].roomCode, rooms[0].latitude, rooms[0].longitude, rooms[0].allowedRadius);
      }
    };

    window.previewLocation = (roomCode, lat, lon, radius) => {
      const ring = document.querySelector('.radius-ring');
      const pin = document.querySelector('.map-placeholder .pin');
      const text = document.getElementById('map-placeholder-text');
      if (ring) ring.style.display = 'block';
      if (pin) pin.style.display = 'block';
      if (text) {
        text.innerHTML = `<strong>${roomCode}</strong><br/>Center: ${lat.toFixed(6)}, ${lon.toFixed(6)} · Radius: ${radius}m`;
      }
    };

    window.deleteLocation = async (id) => {
      await fetch(`/api/classrooms/${id}`, { method: 'DELETE' });
      loadLocations();
    };

    loadLocations();

    const addLocationBtn = document.getElementById('add-location-btn');
    if (addLocationBtn) {
      addLocationBtn.addEventListener('click', async () => {
        const roomCode = prompt('Enter Classroom Code (e.g. LAB-3-02):');
        if (!roomCode) return;
        const floorLevel = parseInt(prompt('Enter Floor Level:', '2') || '1', 10);
        const latitude = parseFloat(prompt('Enter Center Latitude:', '5.253000') || '5.253000');
        const longitude = parseFloat(prompt('Enter Center Longitude:', '100.435000') || '100.435000');
        const allowedRadius = parseFloat(prompt('Enter Allowed Radius (meters):', '25') || '25');

        await fetch('/api/classrooms', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ roomCode, floorLevel, latitude, longitude, allowedRadius })
        });
        loadLocations();
      });
    }
  }
}

function setText(id, val) {
  const el = document.getElementById(id);
  if (el) el.textContent = val;
}
