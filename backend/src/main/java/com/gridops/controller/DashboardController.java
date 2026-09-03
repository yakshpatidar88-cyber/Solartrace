package com.gridops.controller;

import com.gridops.domain.AuditEvent;
import com.gridops.dto.OperationalDtos.DashboardSummaryDto;
import com.gridops.repository.AuditEventRepository;
import com.gridops.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuditEventRepository auditEventRepository;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getSummary() {
        return ResponseEntity.ok(dashboardService.getFleetSummary());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditEvent>> getRecentAuditLogs() {
        return ResponseEntity.ok(auditEventRepository.findTop50ByOrderByCreatedAtDesc());
    }
}
