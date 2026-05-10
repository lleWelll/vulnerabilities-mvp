package org.aitu.vulnerabilitiesmvp.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank
//    @Size(min = 3)
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username must contain only letters, digits, dots, underscores, and hyphens")
    String username,

    @NotBlank
//    @Size(min = 10)
    @Size(min = 10, max = 72)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$",
        message = "Password must contain uppercase, lowercase, digit, and special character"
    )
    String password
) {
}
