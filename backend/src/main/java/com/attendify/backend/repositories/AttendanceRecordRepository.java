package com.attendify.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.attendify.backend.models.AttendanceRecord;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findByStudentIdOrderByTimestampDesc(String studentId);
    List<AttendanceRecord> findAllByOrderByTimestampDesc();
}

