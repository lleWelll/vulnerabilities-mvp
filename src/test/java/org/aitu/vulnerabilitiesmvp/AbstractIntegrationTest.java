package org.aitu.vulnerabilitiesmvp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aitu.vulnerabilitiesmvp.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected AuditEventRepository auditEventRepository;

    protected String clientToken;
    protected String bobToken;
    protected String operatorToken;

    @BeforeEach
    void authenticateSeedUsers() throws Exception {
        clientToken = loginAndGetToken("client_alice", "AlicePass123!");
        bobToken = loginAndGetToken("client_bob", "BobPass123!");
        operatorToken = loginAndGetToken("operator_jane", "OperatorPass123!");
    }

    protected String loginAndGetToken(String username, String password) throws Exception {
        String payload = """
            {
              "username": "%s",
              "password": "%s"
            }
            """.formatted(username, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }
}
