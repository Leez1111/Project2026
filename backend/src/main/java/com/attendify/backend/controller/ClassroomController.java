package com.attendify.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.attendify.backend.models.Classroom;
import com.attendify.backend.repositories.ClassroomRepository;

@RestController
@RequestMapping("/api/classrooms")
public class ClassroomController {

    private final ClassroomRepository classroomRepository;

    public ClassroomController(ClassroomRepository classroomRepository) {
        this.classroomRepository = classroomRepository;
    }

    @GetMapping
    public List<Classroom> getAllClassrooms() {
        return classroomRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createClassroom(@RequestBody Classroom classroom) {
        Map<String, Object> response = new HashMap<>();
        if (classroom.getRoomCode() == null || classroom.getRoomCode().isBlank()) {
            response.put("success", false);
            response.put("message", "Room code is required.");
            return ResponseEntity.badRequest().body(response);
        }
        if (classroom.getAllowedRadius() <= 0) {
            classroom.setAllowedRadius(25.0);
        }
        Classroom saved = classroomRepository.save(classroom);
        response.put("success", true);
        response.put("classroom", saved);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteClassroom(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        classroomRepository.deleteById(id);
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}

