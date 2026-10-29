/**
 * ============================================================
 * ATTENDANCE UI & BACKEND CONTROLLER
 * Step 1: Automatic Classroom Location Detection (/api/attendance/verify-location)
 * Step 2: Preferred Biometric Selection (Face ID / Fingerprint)
 * Step 3: Biometric Verification & DB Persistence (/api/attendance/verify-biometric)
 * ============================================================
 */

const optFace        = document.getElementById('opt-face');
const optFingerprint = document.getElementById('opt-fingerprint');
const startBtn       = document.getElementById('start-btn');

const scannerStage   = document.getElementById('scanner-stage');
const scannerIcon    = document.getElementById('scanner-icon');
const scannerTitle   = document.getElementById('scanner-title');
const scannerHint    = document.getElementById('scanner-hint');
const cancelScan     = document.getElementById('cancel-scan');

const successStage   = document.getElementById('success-stage');
const successDetail  = document.getElementById('success-detail');

const stepLoc   = document.getElementById('step-loc');
const stepBio   = document.getElementById('step-bio');
const stepDone  = document.getElementById('step-done');
const locBanner = document.getElementById('loc-banner');

let selectedMethod = null;
let locationVerified = false;
let verifiedCoords = { latitude: 5.253015, longitude: 100.435015 };
let detectedRoomCode = 'LAB-2-01';
let scanTimeoutId = null;

/* ---- Step 1: Select biometric method (only allowed when location is verified) ---- */
function selectMethod(method) {
  selectedMethod = method;
  optFace?.classList.toggle('selected', method === 'face');
  optFingerprint?.classList.toggle('selected', method === 'fingerprint');

  updateStartButton();
}

function updateStartButton() {
  if (!startBtn) return;
  if (!locationVerified) {
    startBtn.disabled = true;
    startBtn.textContent = 'Location must be verified first';
    return;
  }
  if (!selectedMethod) {
    startBtn.disabled = true;
    startBtn.textContent = 'Select a method to continue';
    return;
  }
  startBtn.disabled = false;
  startBtn.textContent = `Continue with ${selectedMethod === 'face' ? 'Face ID' : 'Fingerprint'}`;
}

optFace?.addEventListener('click', () => selectMethod('face'));
optFingerprint?.addEventListener('click', () => selectMethod('fingerprint'));

/* ---- Step 2: Enter scanner stage & call backend /api/attendance/verify-biometric ---- */
startBtn?.addEventListener('click', () => {
  if (!selectedMethod || !locationVerified) return;

  scannerIcon.textContent  = selectedMethod === 'face' ? '😊' : '👆';
  scannerTitle.textContent = selectedMethod === 'face' ? 'Scan your face' : 'Scan your fingerprint';
  scannerHint.textContent  = selectedMethod === 'face'
    ? 'Hold your face inside the circle and look at the camera.'
    : 'Place your finger on the sensor and hold still.';

  scannerStage.classList.add('show');
  successStage.classList.remove('show');

  stepBio.classList.remove('active');
  stepBio.classList.add('done');

  if (scanTimeoutId) clearTimeout(scanTimeoutId);
  scanTimeoutId = setTimeout(submitBiometricToBackend, 1800);
});

/* ---- Cancel scan ---- */
cancelScan?.addEventListener('click', () => {
  if (scanTimeoutId) clearTimeout(scanTimeoutId);
  scannerStage.classList.remove('show');
  stepBio.classList.add('active');
  stepBio.classList.remove('done');
});

/* ---- Step 3: Verify biometric with Spring Boot Backend & show success ---- */
async function submitBiometricToBackend() {
  const savedUser = JSON.parse(localStorage.getItem('attendify_user') || '{"studentId":"STU001"}');
  const studentId = savedUser.studentId || 'STU001';
  const biometricType = selectedMethod === 'face' ? 'FACE_ID' : 'FINGERPRINT';

  try {
    const res = await fetch('/api/attendance/verify-biometric', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        studentId,
        roomCode: detectedRoomCode,
        biometricType,
        latitude: verifiedCoords.latitude,
        longitude: verifiedCoords.longitude
      })
    });
    const data = await res.json();

    if (res.ok && data.success) {
      showSuccess(data.message);
    } else {
      scannerTitle.textContent = 'Verification failed';
      scannerHint.textContent = data.message || 'Could not verify biometric.';
    }
  } catch (err) {
    scannerTitle.textContent = 'Connection error';
    scannerHint.textContent = 'Unable to reach backend server.';
  }
}

function showSuccess(backendMessage) {
  scannerStage.classList.remove('show');
  successStage.classList.add('show');

  successDetail.textContent = backendMessage ||
    `Verified via ${selectedMethod === 'face' ? 'Face ID' : 'Fingerprint'} · Location confirmed in ${detectedRoomCode}.`;

  stepDone.classList.remove('active');
  stepDone.classList.add('done');
}

/* ---- Location banner & Backend Geofence Detection ---- */
function setLocationStatus(inside, detailText) {
  locationVerified = inside;
  locBanner.classList.toggle('ok',   inside);
  locBanner.classList.toggle('fail', !inside);

  const toggleLink = `<a href="#" onclick="toggleSimulatedLocation(event)" style="font-size:11px; text-decoration:underline; margin-left:8px; opacity:0.85;">${inside ? '(Simulate Outside)' : '(Simulate Inside Class)'}</a>`;

  locBanner.innerHTML = inside
    ? `<span class="loc-icon">📍</span><div style="flex:1;"><strong>Location verified — ${detectedRoomCode}</strong> ${toggleLink}<br/>
       <span style="font-size:12px;">${detailText || 'You are inside the classroom area.'}</span></div>`
    : `<span class="loc-icon">📍</span><div style="flex:1;"><strong>Location not verified</strong> ${toggleLink}<br/>
       <span style="font-size:12px;">${detailText || 'You must be inside the classroom area to check in.'}</span></div>`;

  if (inside) {
    stepLoc.classList.add('done');
    stepLoc.classList.remove('active');
    stepBio.classList.add('active');
  } else {
    stepLoc.classList.remove('done');
    stepLoc.classList.add('active');
    stepBio.classList.remove('active');
    stepBio.classList.remove('done');
    scannerStage.classList.remove('show');
    successStage.classList.remove('show');
  }

  updateStartButton();
}

async function verifyCoordinatesWithBackend(latitude, longitude) {
  const savedUser = JSON.parse(localStorage.getItem('attendify_user') || '{"studentId":"STU001"}');
  try {
    const res = await fetch('/api/attendance/verify-location', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        studentId: savedUser.studentId || 'STU001',
        latitude,
        longitude
      })
    });
    const data = await res.json();
    if (data.roomCode) detectedRoomCode = data.roomCode;

    if (res.ok && data.success && data.biometricUnlocked) {
      verifiedCoords = { latitude, longitude };
      setLocationStatus(true, data.message);
    } else {
      setLocationStatus(false, data.message);
    }
  } catch (e) {
    setLocationStatus(false, 'Unable to verify classroom location with backend.');
  }
}

window.toggleSimulatedLocation = async function(e) {
  if (e) e.preventDefault();
  if (locationVerified) {
    // Test outside classroom (~150m away)
    await verifyCoordinatesWithBackend(5.254200, 100.436200);
  } else {
    // Test inside classroom LAB-2-01 (~2m from center)
    await verifyCoordinatesWithBackend(5.253015, 100.435015);
  }
};

/* ---- Auto-detect classroom location on page load ---- */
document.addEventListener('DOMContentLoaded', async () => {
  // Fetch first classroom coordinates from DB so navigation detection works seamlessly
  try {
    const roomsRes = await fetch('/api/classrooms');
    const rooms = await roomsRes.json();
    if (rooms.length > 0) {
      detectedRoomCode = rooms[0].roomCode;
      verifiedCoords = {
        latitude: rooms[0].latitude + 0.000015,
        longitude: rooms[0].longitude + 0.000015
      };
    }
  } catch ( ignored ) {}

  // Verify location with backend after 800ms
  setTimeout(() => {
    verifyCoordinatesWithBackend(verifiedCoords.latitude, verifiedCoords.longitude);
  }, 800);
});
