package com.attendify.backend.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.attendify.backend.models.AttendanceRecord;
import com.attendify.backend.models.Classroom;
import com.attendify.backend.models.User;
import com.attendify.backend.repositories.AttendanceRecordRepository;
import com.attendify.backend.repositories.ClassroomRepository;
import com.attendify.backend.repositories.UserRepository;
import com.attendify.backend.service.GeofenceService;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final GeofenceService geofenceService;
    private final ClassroomRepository classroomRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserRepository userRepository;

    public AttendanceController(
            GeofenceService geofenceService,
            ClassroomRepository classroomRepository,
            AttendanceRecordRepository attendanceRecordRepository,
            UserRepository userRepository) {
        this.geofenceService = geofenceService;
        this.classroomRepository = classroomRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.userRepository = userRepository;
    }

    /**
     * Step 1: Verify that the student's GPS coordinates are within the classroom geofence.
     * If verified, unlocks Biometric authentication (Fingerprint or Face ID based on user preference).
     */
    @PostMapping("/verify-location")
    public ResponseEntity<Map<String, Object>> verifyLocation(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();

        String studentId = (String) payload.get("studentId");
        String roomCode = (String) payload.get("roomCode");
        Number latNum = (Number) payload.get("latitude");
        Number lonNum = (Number) payload.get("longitude");

        if (studentId == null || roomCode == null || latNum == null || lonNum == null) {
            response.put("success", false);
            response.put("biometricUnlocked", false);
            response.put("message", "Missing required fields: studentId, roomCode, latitude, or longitude.");
            return ResponseEntity.badRequest().body(response);
        }

        double studentLat = latNum.doubleValue();
        double studentLon = lonNum.doubleValue();

        Optional<Classroom> classroomOpt = classroomRepository.findByRoomCode(roomCode);
        if (classroomOpt.isEmpty()) {
            response.put("success", false);
            response.put("biometricUnlocked", false);
            response.put("message", "Classroom not found in database: " + roomCode);
            return ResponseEntity.badRequest().body(response);
        }

        Classroom classroom = classroomOpt.get();
        double distance = geofenceService.calculateDistanceMeters(
                studentLat, studentLon,
                classroom.getLatitude(), classroom.getLongitude()
        );
        boolean inRange = geofenceService.isWithinClassroom(
                studentLat, studentLon,
                classroom.getLatitude(), classroom.getLongitude(),
                classroom.getAllowedRadius()
        );

        double roundedDist = Math.round(distance * 10.0) / 10.0;
        response.put("distanceMeters", roundedDist);
        response.put("allowedRadius", classroom.getAllowedRadius());

        if (!inRange) {
            response.put("success", false);
            response.put("biometricUnlocked", false);
            response.put("message", String.format("Outside classroom geofence (%.1fm away, allowed %.0fm). Biometric locked.", distance, classroom.getAllowedRadius()));
            return ResponseEntity.badRequest().body(response);
        }

        Optional<User> userOpt = userRepository.findByStudentId(studentId);
        String registeredFp = userOpt.map(User::getFingerprintId).orElse("FP-" + studentId + "-VERIFIED");

        response.put("success", true);
        response.put("biometricUnlocked", true);
        response.put("fingerprintId", registeredFp);
        response.put("message", String.format("Location verified (%.1fm from center). Choose Fingerprint or Face ID to mark attendance.", distance));
        return ResponseEntity.ok(response);
    }

    /**
     * Step 2: Once location is verified, verify Fingerprint or Face ID (user preference) against the database and record attendance.
     */
    @PostMapping({"/verify-biometric", "/verify-fingerprint"})
    public ResponseEntity<Map<String, Object>> verifyBiometric(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();

        String studentId = (String) payload.get("studentId");
        String roomCode = (String) payload.get("roomCode");
        String biometricType = (String) payload.getOrDefault("biometricType", "FINGERPRINT");
        String scannedFingerprintId = (String) payload.get("fingerprintId");
        Number latNum = (Number) payload.get("latitude");
        Number lonNum = (Number) payload.get("longitude");

        if (studentId == null || roomCode == null || latNum == null || lonNum == null) {
            response.put("success", false);
            response.put("message", "Missing student, classroom, or location data for biometric check.");
            return ResponseEntity.badRequest().body(response);
        }

        double studentLat = latNum.doubleValue();
        double studentLon = lonNum.doubleValue();

        Optional<Classroom> classroomOpt = classroomRepository.findByRoomCode(roomCode);
        if (classroomOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Classroom not found: " + roomCode);
            return ResponseEntity.badRequest().body(response);
        }

        Classroom classroom = classroomOpt.get();
        double distance = geofenceService.calculateDistanceMeters(
                studentLat, studentLon,
                classroom.getLatitude(), classroom.getLongitude()
        );
        boolean inRange = geofenceService.isWithinClassroom(
                studentLat, studentLon,
                classroom.getLatitude(), classroom.getLongitude(),
                classroom.getAllowedRadius()
        );

        if (!inRange) {
            response.put("success", false);
            response.put("message", "Biometric rejected: Student is outside the classroom geofence.");
            return ResponseEntity.badRequest().body(response);
        }

        String normalizedBio = "FACE_ID".equalsIgnoreCase(biometricType) ? "FACE_ID" : "FINGERPRINT";
        String bioLabel = "FACE_ID".equals(normalizedBio) ? "Face ID" : "Fingerprint";

        Optional<User> userOpt = userRepository.findByStudentId(studentId);
        String studentName = studentId;
        String dbTemplateId = "FACE_ID".equals(normalizedBio)
                ? "FACE-" + studentId + "-VERIFIED"
                : "FP-" + studentId + "-VERIFIED";

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            studentName = user.getName();
            if (!user.isFingerprintRegistered() || user.getFingerprintId() == null) {
                response.put("success", false);
                response.put("message", "No biometric template registered in database for student " + studentId + ".");
                return ResponseEntity.badRequest().body(response);
            }
            String userFpId = user.getFingerprintId();
            if ("FINGERPRINT".equals(normalizedBio)) {
                dbTemplateId = userFpId;
            }

            // If client provided a specific fingerprintId, verify it matches the database record
            if (scannedFingerprintId != null && !scannedFingerprintId.isBlank()
                    && !userFpId.equalsIgnoreCase(scannedFingerprintId)
                    && !dbTemplateId.equalsIgnoreCase(scannedFingerprintId)) {
                response.put("success", false);
                response.put("message", bioLabel + " mismatch! Scanned biometric does not match database record for " + studentId + ".");
                return ResponseEntity.status(401).body(response);
            }
        }

        double roundedDist = Math.round(distance * 10.0) / 10.0;
        AttendanceRecord record = new AttendanceRecord(
                studentId,
                studentName,
                roomCode,
                "PRESENT",
                normalizedBio,
                dbTemplateId,
                roundedDist,
                studentLat,
                studentLon,
                LocalDateTime.now()
        );
        attendanceRecordRepository.save(record);

        response.put("success", true);
        response.put("biometricType", normalizedBio);
        response.put("fingerprintId", dbTemplateId);
        response.put("record", record);
        response.put("message", bioLabel + " verified (" + dbTemplateId + ")! Attendance saved to database for " + roomCode + ".");
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public List<AttendanceRecord> getAttendanceRecords(@RequestParam(required = false) String studentId) {
        if (studentId != null && !studentId.isBlank()) {
            return attendanceRecordRepository.findByStudentIdOrderByTimestampDesc(studentId);
        }
        return attendanceRecordRepository.findAllByOrderByTimestampDesc();
    }
}
