package com.gridops.service;

import com.gridops.domain.AuditEvent;
import com.gridops.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;

    @Async
    @Transactional
    public void recordEvent(String entityType, UUID entityId, String action, String performedBy, String details) {
        try {
            AuditEvent event = AuditEvent.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .performedBy(performedBy != null ? performedBy : "SYSTEM_ENGINE")
                    .details(details)
                    .build();
            auditEventRepository.save(event);
            log.info("[AUDIT] {} - {} on {} ({}): {}", performedBy, action, entityType, entityId, details);
        } catch (Exception e) {
            log.error("Failed to record audit event: {}", e.getMessage());
        }
    }
}
