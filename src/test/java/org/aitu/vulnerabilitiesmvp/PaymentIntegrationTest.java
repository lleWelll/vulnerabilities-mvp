package org.aitu.vulnerabilitiesmvp;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
