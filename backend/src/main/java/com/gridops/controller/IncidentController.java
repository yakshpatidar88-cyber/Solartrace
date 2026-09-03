package com.gridops.controller;

import com.gridops.domain.MaintenanceIncident;
import com.gridops.domain.WorkOrder;
import com.gridops.dto.OperationalDtos.*;
import com.gridops.repository.MaintenanceIncidentRepository;
import com.gridops.service.MaintenanceIncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IncidentController {

    private final MaintenanceIncidentService incidentService;
    private final MaintenanceIncidentRepository incidentRepository;

    @GetMapping
    public ResponseEntity<List<IncidentResponseDto>> getAllActiveIncidents() {
        return ResponseEntity.ok(incidentService.getAllActiveIncidents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponseDto> getIncidentById(@PathVariable UUID id) {
        return incidentRepository.findById(id)
                .map(incidentService::toResponseDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<WorkOrder> assignTechnician(
            @PathVariable UUID id,
            @Valid @RequestBody AssignWorkOrderRequest request) {
        return ResponseEntity.ok(incidentService.assignTechnician(id, request.getTechnicianId()));
    }

    @PostMapping("/work-orders/{workOrderId}/complete")
    public ResponseEntity<IncidentResponseDto> completeWorkOrder(
            @PathVariable UUID workOrderId,
            @Valid @RequestBody CompleteWorkOrderRequest request) {
        MaintenanceIncident updated = incidentService.completeWorkOrderAndStartVerification(workOrderId, request);
        return ResponseEntity.ok(incidentService.toResponseDto(updated));
    }
}
