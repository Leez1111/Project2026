package com.attendify.backend.repositories;

import com.attendify.backend.models.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    Optional<Classroom> findByRoomCode(String roomCode);
}