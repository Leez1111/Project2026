package com.attendify.backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String studentId;
    private String name;
    private String password;
    private String role; // "STUDENT", "LECTURER", or "ADMIN"

    // Fingerprint Biometric Data stored in database
    private String fingerprintId;         // e.g., "FP-STU001-8F92A"
    private boolean fingerprintRegistered;

    public User() {}

    public User(String studentId, String name, String password, String role) {
        this(studentId, name, password, role, "FP-" + studentId + "-VERIFIED", true);
    }

    public User(String studentId, String name, String password, String role, String fingerprintId, boolean fingerprintRegistered) {
        this.studentId = studentId;
        this.name = name;
        this.password = password;
        this.role = role;
        this.fingerprintId = fingerprintId;
        this.fingerprintRegistered = fingerprintRegistered;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFingerprintId() { return fingerprintId; }
    public void setFingerprintId(String fingerprintId) { this.fingerprintId = fingerprintId; }

    public boolean isFingerprintRegistered() { return fingerprintRegistered; }
    public void setFingerprintRegistered(boolean fingerprintRegistered) { this.fingerprintRegistered = fingerprintRegistered; }
}
