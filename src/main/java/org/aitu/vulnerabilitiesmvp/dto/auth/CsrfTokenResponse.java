package org.aitu.vulnerabilitiesmvp.dto.auth;

public record CsrfTokenResponse(
    String headerName,
    String parameterName,
    String token
) {
}
