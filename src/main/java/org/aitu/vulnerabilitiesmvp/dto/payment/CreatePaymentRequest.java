package org.aitu.vulnerabilitiesmvp.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.aitu.vulnerabilitiesmvp.enums.CurrencyCode;

public record CreatePaymentRequest(
    @NotNull
    @Positive
    Long sourceAccountId,

    @NotNull
    @Positive
    Long receiverAccountId,

    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 15, fraction = 2)
    BigDecimal amount,

    @NotNull
    CurrencyCode currency,

    @Size(max = 255)
    String description
) {
}
