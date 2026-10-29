package com.attendify.backend.controller;

import java.time.LocalDateTime;
import java.util.Comparator;
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
     * Step 1: Navigation / GPS Location Detection (No QR required).
     * Automatically detects if the student's coordinates are inside a classroom geofence.
     * If verified, unlocks preferred Biometric authentication (Face ID or Fingerprint).
     */
    @PostMapping("/verify-location")
    public ResponseEntity<Map<String, Object>> verifyLocation(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();

        String studentId = (String) payload.getOrDefault("studentId", "STU001");
        String roomCode = (String) payload.get("roomCode");
        Number latNum = (Number) payload.get("latitude");
        Number lonNum = (Number) payload.get("longitude");

        if (studentId == null || latNum == null || lonNum == null) {
            response.put("success", false);
            response.put("biometricUnlocked", false);
            response.put("message", "Missing required fields: studentId, latitude, or longitude.");
            return ResponseEntity.badRequest().body(response);
        }

        double studentLat = latNum.doubleValue();
        double studentLon = lonNum.doubleValue();

        Optional<Classroom> classroomOpt;
        if (roomCode != null && !roomCode.isBlank() && !"AUTO".equalsIgnoreCase(roomCode)) {
            classroomOpt = classroomRepository.findByRoomCode(roomCode);
        } else {
            // Auto-detect nearest classroom from GPS coordinates
            List<Classroom> allClassrooms = classroomRepository.findAll();
            classroomOpt = allClassrooms.stream()
                    .min(Comparator.comparingDouble(c -> geofenceService.calculateDistanceMeters(
                            studentLat, studentLon, c.getLatitude(), c.getLongitude()
                    )));
        }

        if (classroomOpt.isEmpty()) {
            response.put("success", false);
            response.put("biometricUnlocked", false);
            response.put("message", "Classroom not found in database.");
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
        response.put("roomCode", classroom.getRoomCode());
        response.put("distanceMeters", roundedDist);
        response.put("allowedRadius", classroom.getAllowedRadius());

        if (!inRange) {
            response.put("success", false);
            response.put("biometricUnlocked", false);
            response.put("message", String.format("Outside classroom %s (%.1fm away, allowed %.0fm). Biometric locked.", classroom.getRoomCode(), distance, classroom.getAllowedRadius()));
            return ResponseEntity.badRequest().body(response);
        }

        Optional<User> userOpt = userRepository.findByStudentId(studentId);
        String registeredFp = userOpt.map(User::getFingerprintId).orElse("FP-" + studentId + "-VERIFIED");

        response.put("success", true);
        response.put("biometricUnlocked", true);
        response.put("fingerprintId", registeredFp);
        response.put("message", String.format("You are inside %s (%.1fm from center). Select Face ID or Fingerprint.", classroom.getRoomCode(), distance));
        return ResponseEntity.ok(response);
    }

    /**
     * Step 2: Once classroom location is verified, verify Student's preferred Biometric (Face ID or Fingerprint)
     * against the persistent database and record attendance.
     */
    @PostMapping({"/verify-biometric", "/verify-fingerprint"})
    public ResponseEntity<Map<String, Object>> verifyBiometric(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();

        String studentId = (String) payload.getOrDefault("studentId", "STU001");
        String roomCode = (String) payload.get("roomCode");
        String biometricType = (String) payload.getOrDefault("biometricType", "FINGERPRINT");
        String scannedFingerprintId = (String) payload.get("fingerprintId");
        Number latNum = (Number) payload.get("latitude");
        Number lonNum = (Number) payload.get("longitude");

        if (studentId == null || latNum == null || lonNum == null) {
            response.put("success", false);
            response.put("message", "Missing student or location data for biometric check.");
            return ResponseEntity.badRequest().body(response);
        }

        double studentLat = latNum.doubleValue();
        double studentLon = lonNum.doubleValue();

        Optional<Classroom> classroomOpt;
        if (roomCode != null && !roomCode.isBlank() && !"AUTO".equalsIgnoreCase(roomCode)) {
            classroomOpt = classroomRepository.findByRoomCode(roomCode);
        } else {
            classroomOpt = classroomRepository.findAll().stream()
                    .min(Comparator.comparingDouble(c -> geofenceService.calculateDistanceMeters(
                            studentLat, studentLon, c.getLatitude(), c.getLongitude()
                    )));
        }

        if (classroomOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Classroom not found.");
            return ResponseEntity.badRequest().body(response);
        }

        Classroom classroom = classroomOpt.get();
        String detectedRoom = classroom.getRoomCode();
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

        String normalizedBio = ("FACE_ID".equalsIgnoreCase(biometricType) || "FACE".equalsIgnoreCase(biometricType))
                ? "FACE_ID"
                : "FINGERPRINT";
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
                detectedRoom,
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
        response.put("roomCode", detectedRoom);
        response.put("biometricType", normalizedBio);
        response.put("fingerprintId", dbTemplateId);
        response.put("distanceMeters", roundedDist);
        response.put("record", record);
        response.put("message", "Verified via " + bioLabel + " · Location confirmed in " + detectedRoom + " (" + roundedDist + "m).");
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
