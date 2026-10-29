package com.attendify.backend.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "classrooms")
public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomCode;       // e.g., "LAB-2-01"
    private int floorLevel;        // e.g., 2
    private double latitude;       // Classroom center latitude
    private double longitude;      // Classroom center longitude
    private double allowedRadius;  // Allowed range in meters (e.g., 25.0)

    public Classroom() {}

    public Classroom(String roomCode, int floorLevel, double latitude, double longitude, double allowedRadius) {
        this.roomCode = roomCode;
        this.floorLevel = floorLevel;
        this.latitude = latitude;
        this.longitude = longitude;
        this.allowedRadius = allowedRadius;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public int getFloorLevel() { return floorLevel; }
    public void setFloorLevel(int floorLevel) { this.floorLevel = floorLevel; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getAllowedRadius() { return allowedRadius; }
    public void setAllowedRadius(double allowedRadius) { this.allowedRadius = allowedRadius; }
}
