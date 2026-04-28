package org.aitu.vulnerabilitiesmvp.service;

import static org.mockito.Mockito.verify;

import org.aitu.vulnerabilitiesmvp.entity.AuditEvent;
import org.aitu.vulnerabilitiesmvp.enums.AuditEventType;
import org.aitu.vulnerabilitiesmvp.enums.AuditOutcome;
import org.aitu.vulnerabilitiesmvp.repository.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void shouldSanitizeAuditFieldsBeforePersisting() {
        auditService.record(
            AuditEventType.LOGIN_FAILED,
            "attacker\r\nforged",
            "AUTH\tTARGET",
            null,
            AuditOutcome.FAILURE,
            "line1\r\nline2\t" + "x".repeat(600)
        );

        ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEvent savedEvent = eventCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(savedEvent.getActorUsername()).isEqualTo("attacker forged");
        org.assertj.core.api.Assertions.assertThat(savedEvent.getTargetType()).isEqualTo("AUTH TARGET");
        org.assertj.core.api.Assertions.assertThat(savedEvent.getMetadata()).startsWith("line1 line2 ");
        org.assertj.core.api.Assertions.assertThat(savedEvent.getMetadata()).hasSize(500);
    }
}
