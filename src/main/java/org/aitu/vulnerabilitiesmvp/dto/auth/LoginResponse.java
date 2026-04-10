package org.aitu.vulnerabilitiesmvp.dto.auth;

import org.aitu.vulnerabilitiesmvp.enums.Role;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    Role role
) {
}
