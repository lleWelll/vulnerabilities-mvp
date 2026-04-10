package org.aitu.vulnerabilitiesmvp;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

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
    }

    @Test
    void operatorShouldFlagPaymentManually() throws Exception {
        mockMvc.perform(post("/api/fraud/payments/{id}/flag", 2001)
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
