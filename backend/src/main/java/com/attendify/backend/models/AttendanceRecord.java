package com.attendify.backend.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentId;
    private String studentName;
    private String roomCode;
    private String status;        // "PRESENT", "LATE", "ABSENT"
    private String biometricType; // "FINGERPRINT"
    private String fingerprintId; // Matched fingerprint template ID from database
    private double distanceMeters;
    private double latitude;
    private double longitude;
    private LocalDateTime timestamp;

    public AttendanceRecord() {}

    public AttendanceRecord(String studentId, String studentName, String roomCode, String status, String biometricType, double latitude, double longitude, LocalDateTime timestamp) {
        this(studentId, studentName, roomCode, status, biometricType, "FP-" + studentId + "-VERIFIED", 0.0, latitude, longitude, timestamp);
    }

    public AttendanceRecord(String studentId, String studentName, String roomCode, String status, String biometricType, String fingerprintId, double distanceMeters, double latitude, double longitude, LocalDateTime timestamp) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.roomCode = roomCode;
        this.status = status;
        this.biometricType = biometricType;
        this.fingerprintId = fingerprintId;
        this.distanceMeters = distanceMeters;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBiometricType() { return biometricType; }
    public void setBiometricType(String biometricType) { this.biometricType = biometricType; }

    public String getFingerprintId() { return fingerprintId; }
    public void setFingerprintId(String fingerprintId) { this.fingerprintId = fingerprintId; }

    public double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(double distanceMeters) { this.distanceMeters = distanceMeters; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
