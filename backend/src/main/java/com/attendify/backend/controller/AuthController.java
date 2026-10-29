package com.attendify.backend.controller;

import com.attendify.backend.models.User;
import com.attendify.backend.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        Map<String, Object> response = new HashMap<>();
        String studentId = credentials.get("studentId");
        String password = credentials.get("password");

        if (studentId == null || studentId.isBlank() || password == null || password.isBlank()) {
            response.put("success", false);
            response.put("message", "Missing ID or password.");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<User> userOpt = userRepository.findByStudentId(studentId.trim());
        if (userOpt.isEmpty() || !userOpt.get().getPassword().equals(password)) {
            response.put("success", false);
            response.put("message", "Invalid ID or password.");
            return ResponseEntity.status(401).body(response);
        }

        User user = userOpt.get();
        response.put("success", true);
        response.put("studentId", user.getStudentId());
        response.put("name", user.getName());
        response.put("role", user.getRole());
        response.put("fingerprintId", user.getFingerprintId());
        response.put("fingerprintRegistered", user.isFingerprintRegistered());
        response.put("message", "Login successful for " + user.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody User newUser) {
        Map<String, Object> response = new HashMap<>();
        if (newUser.getStudentId() == null || newUser.getStudentId().isBlank() || newUser.getName() == null || newUser.getName().isBlank()) {
            response.put("success", false);
            response.put("message", "User ID and Name are required.");
            return ResponseEntity.badRequest().body(response);
        }
        String cleanId = newUser.getStudentId().trim().toUpperCase();
        newUser.setStudentId(cleanId);
        if (newUser.getPassword() == null || newUser.getPassword().isBlank()) {
            newUser.setPassword("1234");
        }
        if (newUser.getRole() == null || newUser.getRole().isBlank()) {
            newUser.setRole("STUDENT");
        }
        if (newUser.getFingerprintId() == null || newUser.getFingerprintId().isBlank()) {
            newUser.setFingerprintId("FP-" + cleanId + "-VERIFIED");
            newUser.setFingerprintRegistered(true);
        }
        User saved = userRepository.save(newUser);
        response.put("success", true);
        response.put("user", saved);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{studentId}/fingerprint")
    public ResponseEntity<Map<String, Object>> registerFingerprint(
            @PathVariable String studentId,
            @RequestBody(required = false) Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOpt = userRepository.findByStudentId(studentId.trim());
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found: " + studentId);
            return ResponseEntity.badRequest().body(response);
        }

        User user = userOpt.get();
        String newFpId = (payload != null && payload.get("fingerprintId") != null && !payload.get("fingerprintId").isBlank())
                ? payload.get("fingerprintId")
                : "FP-" + user.getStudentId() + "-VERIFIED";

        user.setFingerprintId(newFpId);
        user.setFingerprintRegistered(true);
        userRepository.save(user);

        response.put("success", true);
        response.put("fingerprintId", user.getFingerprintId());
        response.put("message", "Fingerprint registered in database for " + user.getName());
        return ResponseEntity.ok(response);
    }
}
