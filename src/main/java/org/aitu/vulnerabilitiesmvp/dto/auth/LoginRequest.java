package org.aitu.vulnerabilitiesmvp.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank
    @Size(max = 50)
    String username,

    @NotBlank
    @Size(min = 10, max = 72)
    String password
) {
}
