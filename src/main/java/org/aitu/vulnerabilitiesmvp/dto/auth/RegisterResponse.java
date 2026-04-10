package org.aitu.vulnerabilitiesmvp.dto.auth;

import java.time.Instant;
import org.aitu.vulnerabilitiesmvp.enums.CurrencyCode;
import org.aitu.vulnerabilitiesmvp.enums.Role;

public record RegisterResponse(
    Long userId,
    String username,
    Role role,
    Long defaultAccountId,
    CurrencyCode defaultCurrency,
    Instant createdAt
) {
}
