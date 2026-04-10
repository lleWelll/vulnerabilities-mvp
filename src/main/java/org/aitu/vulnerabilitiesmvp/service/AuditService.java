package org.aitu.vulnerabilitiesmvp.service;

import org.aitu.vulnerabilitiesmvp.entity.AuditEvent;
import org.aitu.vulnerabilitiesmvp.enums.AuditEventType;
import org.aitu.vulnerabilitiesmvp.enums.AuditOutcome;
import org.aitu.vulnerabilitiesmvp.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void record(
        AuditEventType eventType,
        String actorUsername,
        String targetType,
        Long targetId,
        AuditOutcome outcome,
        String metadata
    ) {
        AuditEvent event = new AuditEvent();
        event.setEventType(eventType);
        event.setActorUsername(sanitize(actorUsername, 50));
        event.setTargetType(sanitize(targetType, 50));
        event.setTargetId(targetId);
        event.setOutcome(outcome);
        event.setMetadata(sanitize(metadata, 500));
        auditEventRepository.save(event);

        if (outcome == AuditOutcome.SUCCESS) {
            log.info("audit event={} actor={} targetType={} targetId={}", eventType, actorUsername, targetType, targetId);
        } else {
            log.warn("audit event={} actor={} targetType={} targetId={} outcome={}", eventType, actorUsername, targetType, targetId, outcome);
        }
    }

    private String sanitize(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String sanitized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return sanitized.length() > maxLength ? sanitized.substring(0, maxLength) : sanitized;
    }
}
