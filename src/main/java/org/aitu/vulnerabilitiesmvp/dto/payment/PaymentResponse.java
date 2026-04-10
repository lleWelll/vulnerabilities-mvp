package org.aitu.vulnerabilitiesmvp.dto.payment;

import java.math.BigDecimal;
import java.time.Instant;
import org.aitu.vulnerabilitiesmvp.enums.CurrencyCode;
import org.aitu.vulnerabilitiesmvp.enums.PaymentStatus;

public record PaymentResponse(
    Long id,
    Long sourceAccountId,
    Long receiverAccountId,
    String receiverUsername,
    BigDecimal amount,
    CurrencyCode currency,
    PaymentStatus status,
    boolean flagged,
    String description,
    Instant createdAt,
    Instant confirmedAt
) {
}
