# FYP2026 — Smart Attendance System (Attendify)

A **QR + GPS Geofencing + Biometric (Fingerprint / Face ID)** Smart Campus Attendance System built with **Java Spring Boot** and **FYP UI**.

## Key Features
1. **Classroom QR Identification:** Identifies classroom sessions (`LAB-2-01`, `DK-1`).
2. **GPS Location Tracking & Geofencing:** Uses the Haversine distance algorithm to verify the student is physically inside the classroom radius before unlocking biometric verification.
3. **User-Preferred Biometric Authentication:** Once GPS location is verified, students can authenticate using **Fingerprint** or **Face ID** matched against the persistent database.
4. **Role-Based Portals:** Dedicated dashboards for **Student**, **Lecturer** (Live Session QR & real-time check-in feed), and **Admin** (Geofence & User/Biometric Management).

## Quick Start
```bash
npm run dev
```
Then open **http://localhost:8081/** in your browser.

### Demo Credentials
- **Student:** `STU001` / `1234`
- **Lecturer:** `LEC001` / `1234`
- **Admin:** `ADM001` / `1234`
