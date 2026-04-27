package org.aitu.vulnerabilitiesmvp;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldRegisterClientWithDefaultAccount() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "new_client",
                      "password": "StrongPass123!"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("new_client"))
            .andExpect(jsonPath("$.role").value("CLIENT"))
            .andExpect(jsonPath("$.defaultCurrency").value("KZT"));
    }

    @Test
    void shouldRejectInvalidLogin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "client_alice",
                      "password": "WrongPass123!"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void shouldRejectOversizedJsonBody() throws Exception {
        String payload = """
            {
              "username": "%s",
              "password": "StrongPass123!"
            }
            """.formatted("a".repeat(20_000));

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.message").value("Request body is too large"));
    }

    @Test
    void shouldRejectJsonThatExceedsParserStringLimit() throws Exception {
        String payload = """
            {
              "username": "%s",
              "password": "StrongPass123!"
            }
            """.formatted("a".repeat(5_000));

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Request JSON exceeds parsing safety limits"));
    }

    @Test
    void shouldRejectMultipartUploadRequests() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "payload.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "a".repeat(20_000).getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/auth/register")
                .file(file)
                .with(csrf()))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.message").value("File uploads are not supported by this API"));
    }
}
