package com.attendify.backend;

import com.attendify.backend.models.AttendanceRecord;
import com.attendify.backend.models.Classroom;
import com.attendify.backend.models.User;
import com.attendify.backend.repositories.AttendanceRecordRepository;
import com.attendify.backend.repositories.ClassroomRepository;
import com.attendify.backend.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    // Initializes default data in the persistent database only if tables are empty
    @Bean
    public CommandLineRunner initData(
            ClassroomRepository classroomRepository,
            UserRepository userRepository,
            AttendanceRecordRepository attendanceRecordRepository) {
        return args -> {
            if (classroomRepository.count() == 0) {
                classroomRepository.save(new Classroom("LAB-2-01", 2, 5.253000, 100.435000, 25.0));
                classroomRepository.save(new Classroom("DK-1", 1, 5.253250, 100.435300, 35.0));
            }

            if (userRepository.count() == 0) {
                userRepository.save(new User("STU001", "Lee Zheng Shen", "1234", "STUDENT", "FP-STU001-VERIFIED", true));
                userRepository.save(new User("STU002", "Aisyah Ahmad", "1234", "STUDENT", "FP-STU002-VERIFIED", true));
                userRepository.save(new User("LEC001", "Dr. Tan Wei Ming", "1234", "LECTURER", "FP-LEC001-VERIFIED", true));
                userRepository.save(new User("ADM001", "System Admin", "1234", "ADMIN", "FP-ADM001-VERIFIED", true));
            }

            if (attendanceRecordRepository.count() == 0) {
                attendanceRecordRepository.save(new AttendanceRecord(
                        "STU001", "Lee Zheng Shen", "LAB-2-01", "PRESENT", "FINGERPRINT",
                        "FP-STU001-VERIFIED", 1.8, 5.253010, 100.435010, LocalDateTime.now().minusDays(1)
                ));
            }

            System.out.println("\n  SMART ATTENDANCE SERVER READY (GPS Location + Preferred Biometric)");
            System.out.println("  -> Local:       http://localhost:8081/");
            System.out.println("  -> Check-in:    http://localhost:8081/student/attendance.html");
            System.out.println("  -> Admin:       http://localhost:8081/admin/geofence.html");
            System.out.println("  -> DB Console:  http://localhost:8081/h2-console\n");
        };
    }
}
