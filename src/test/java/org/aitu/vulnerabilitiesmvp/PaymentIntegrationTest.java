package org.aitu.vulnerabilitiesmvp;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.aitu.vulnerabilitiesmvp.entity.AuditEvent;
import org.aitu.vulnerabilitiesmvp.enums.AuditEventType;
import org.aitu.vulnerabilitiesmvp.enums.AuditOutcome;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldCreateAndConfirmPayment() throws Exception {
        String createPayload = """
            {
              "sourceAccountId": 1001,
              "receiverAccountId": 1002,
              "amount": 250.00,
              "currency": "KZT",
              "description": "Integration payment"
            }
            """;

        String createResponse = mockMvc.perform(post("/api/payments")
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();

        long paymentId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(post("/api/payments/{id}/confirm", paymentId)
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.confirmedAt").exists());
    }

    @Test
    void shouldRejectPaymentFromAnotherUsersAccount() throws Exception {
        mockMvc.perform(post("/api/payments")
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sourceAccountId": 1002,
                      "receiverAccountId": 1001,
                      "amount": 250.00,
                      "currency": "KZT",
                      "description": "Forbidden attempt"
                    }
                    """))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnHistoryOnlyForCurrentClient() throws Exception {
        mockMvc.perform(get("/api/payments/history")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].receiverUsername").exists());
    }

    @Test
    void operatorShouldAccessFlaggedPaymentForFraudReview() throws Exception {
        mockMvc.perform(get("/api/payments/{id}", 2001)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(2001))
            .andExpect(jsonPath("$.flagged").value(true));
    }

    @Test
    void operatorShouldNotAccessUnflaggedPaymentOfAnotherClient() throws Exception {
        mockMvc.perform(get("/api/payments/{id}", 2002)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("You are not allowed to access this payment"));
    }

    @Test
    void secondClientShouldNotAccessAnotherClientsPayment() throws Exception {
        mockMvc.perform(get("/api/payments/{id}", 2001)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bobToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("You are not allowed to access this payment"));
    }

    @Test
    void operatorShouldNotAccessClientHistory() throws Exception {
        mockMvc.perform(get("/api/payments/history")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("You are not allowed to access this resource"));

        AuditEvent latestEvent = auditEventRepository.findTopByOrderByIdDesc()
            .orElseThrow(() -> new AssertionError("Expected ACCESS_DENIED audit event"));

        org.assertj.core.api.Assertions.assertThat(latestEvent.getEventType()).isEqualTo(AuditEventType.ACCESS_DENIED);
        org.assertj.core.api.Assertions.assertThat(latestEvent.getActorUsername()).isEqualTo("operator_jane");
        org.assertj.core.api.Assertions.assertThat(latestEvent.getTargetType()).isEqualTo("REQUEST");
        org.assertj.core.api.Assertions.assertThat(latestEvent.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
        org.assertj.core.api.Assertions.assertThat(latestEvent.getMetadata()).isEqualTo("GET /api/payments/history");
    }

    @Test
    void shouldExportHistoryAsCsvAndEscapeFormulaInjection() throws Exception {
        mockMvc.perform(post("/api/payments")
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sourceAccountId": 1001,
                      "receiverAccountId": 1002,
                      "amount": 125.00,
                      "currency": "KZT",
                      "description": "=SUM(1,1)"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/payments/history/export")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                .param("format", "CSV")
                .param("receiverUsername", "client_bob")
                .param("fileName", "history_export"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("history_export-")))
            .andExpect(content().contentTypeCompatibleWith(new MediaType("text", "csv")))
            .andExpect(content().string(containsString("\"'=SUM(1,1)\"")));
    }

    @Test
    void shouldExportHistoryAsXmlWithEscapedMarkup() throws Exception {
        mockMvc.perform(post("/api/payments")
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sourceAccountId": 1001,
                      "receiverAccountId": 1002,
                      "amount": 225.00,
                      "currency": "KZT",
                      "description": "<script>alert(1)</script>"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/payments/history/export")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                .param("format", "XML")
                .param("fileName", "xml_export"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
            .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")));
    }

    @Test
    void shouldRejectInvalidExportFileName() throws Exception {
        mockMvc.perform(get("/api/payments/history/export")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                .param("format", "JSON")
                .param("fileName", "../evil"))
            .andExpect(status().isBadRequest());
    }
}
