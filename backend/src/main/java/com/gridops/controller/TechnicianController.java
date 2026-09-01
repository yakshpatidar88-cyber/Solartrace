package com.gridops.controller;

import com.gridops.domain.Technician;
import com.gridops.repository.TechnicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/technicians")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TechnicianController {

    private final TechnicianRepository technicianRepository;

    @GetMapping
    public ResponseEntity<List<Technician>> getAllTechnicians(@RequestParam(required = false) Boolean availableOnly) {
        if (Boolean.TRUE.equals(availableOnly)) {
            return ResponseEntity.ok(technicianRepository.findByIsAvailableTrue());
        }
        return ResponseEntity.ok(technicianRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Technician> createTechnician(@RequestBody Technician technician) {
        return ResponseEntity.ok(technicianRepository.save(technician));
    }
}
