package org.aitu.vulnerabilitiesmvp.dto.fraud;

import java.math.BigDecimal;
import java.time.Instant;
import org.aitu.vulnerabilitiesmvp.enums.CurrencyCode;
import org.aitu.vulnerabilitiesmvp.enums.PaymentStatus;
import org.aitu.vulnerabilitiesmvp.enums.RiskLevel;

public record FraudFlagResponse(
    Long fraudFlagId,
    Long paymentId,
    String ownerUsername,
    String receiverUsername,
    BigDecimal amount,
    CurrencyCode currency,
    PaymentStatus paymentStatus,
    RiskLevel riskLevel,
    String reason,
    boolean manual,
    String flaggedBy,
    Instant createdAt
) {
}
