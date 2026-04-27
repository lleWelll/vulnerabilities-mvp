package org.aitu.vulnerabilitiesmvp.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username must contain only letters, digits, dots, underscores, and hyphens")
    String username,

    @NotBlank
    @Size(min = 10, max = 72)
    String password
) {
}
