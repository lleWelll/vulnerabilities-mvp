package org.aitu.vulnerabilitiesmvp;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.aitu.vulnerabilitiesmvp.enums.AuditEventType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FraudIntegrationTest extends AbstractIntegrationTest {

    @Test
    void operatorShouldSeeFraudFlags() throws Exception {
        mockMvc.perform(get("/api/fraud/flags")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].paymentId").exists());
    }

    @Test
    void clientShouldNotSeeFraudFlags() throws Exception {
        mockMvc.perform(get("/api/fraud/flags")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken))
            .andExpect(status().isForbidden());

        boolean deniedRequestWasAudited = auditEventRepository.findAll().stream()
            .anyMatch(event ->
                event.getEventType() == AuditEventType.ACCESS_DENIED
                    && "client_alice".equals(event.getActorUsername())
                    && "REQUEST".equals(event.getTargetType())
                    && event.getMetadata().contains("GET /api/fraud/flags")
            );

        assertThat(deniedRequestWasAudited).isTrue();
    }

    @Test
    void operatorShouldFlagPaymentManually() throws Exception {
        mockMvc.perform(post("/api/fraud/payments/{id}/flag", 2001)
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "riskLevel": "MEDIUM",
                      "reason": "Manual review required"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.manual").value(true))
            .andExpect(jsonPath("$.paymentId").value(2001));
    }
}
