package com.attendify.backend;

import com.attendify.backend.controller.AttendanceController;
import com.attendify.backend.controller.AuthController;
import com.attendify.backend.controller.ClassroomController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BackendApplicationTests {

	@Autowired
	private AttendanceController attendanceController;

	@Autowired
	private AuthController authController;

	@Autowired
	private ClassroomController classroomController;

	@Test
	void contextLoads() {
		assertNotNull(attendanceController);
		assertNotNull(authController);
		assertNotNull(classroomController);
	}

	@Test
	void testLocationTrackingAndFingerprintDatabaseFlow() {
		// 1. Outside classroom geofence -> Fingerprint scanner stays locked
		ResponseEntity<Map<String, Object>> outsideRes = attendanceController.verifyLocation(Map.of(
				"studentId", "STU001",
				"roomCode", "LAB-2-01",
				"latitude", 5.260000,
				"longitude", 100.440000
		));
		assertEquals(400, outsideRes.getStatusCode().value());
		assertEquals(false, outsideRes.getBody().get("biometricUnlocked"));

		// 2. Inside classroom geofence -> Fingerprint scanner unlocked & returns registered Fingerprint ID
		ResponseEntity<Map<String, Object>> insideRes = attendanceController.verifyLocation(Map.of(
				"studentId", "STU001",
				"roomCode", "LAB-2-01",
				"latitude", 5.253010,
				"longitude", 100.435010
		));
		assertEquals(200, insideRes.getStatusCode().value());
		assertEquals(true, insideRes.getBody().get("biometricUnlocked"));
		assertEquals("FP-STU001-VERIFIED", insideRes.getBody().get("fingerprintId"));

		// 3. Mismatched / Unregistered Fingerprint -> Rejected (401) by database verification
		ResponseEntity<Map<String, Object>> mismatchRes = attendanceController.verifyBiometric(Map.of(
				"studentId", "STU001",
				"roomCode", "LAB-2-01",
				"biometricType", "FINGERPRINT",
				"fingerprintId", "FP-WRONG-PERSON",
				"latitude", 5.253010,
				"longitude", 100.435010
		));
		assertEquals(401, mismatchRes.getStatusCode().value());
		assertEquals(false, mismatchRes.getBody().get("success"));

		// 4. Valid Registered Fingerprint -> Verified (200) against database and saved as PRESENT
		ResponseEntity<Map<String, Object>> validFpRes = attendanceController.verifyBiometric(Map.of(
				"studentId", "STU001",
				"roomCode", "LAB-2-01",
				"biometricType", "FINGERPRINT",
				"fingerprintId", "FP-STU001-VERIFIED",
				"latitude", 5.253010,
				"longitude", 100.435010
		));
		assertEquals(200, validFpRes.getStatusCode().value());
		assertEquals(true, validFpRes.getBody().get("success"));
		assertEquals("FINGERPRINT", validFpRes.getBody().get("biometricType"));
		assertEquals("FP-STU001-VERIFIED", validFpRes.getBody().get("fingerprintId"));
	}
}
