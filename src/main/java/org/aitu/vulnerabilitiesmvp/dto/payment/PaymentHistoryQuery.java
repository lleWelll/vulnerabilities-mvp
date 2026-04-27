package org.aitu.vulnerabilitiesmvp.dto.payment;

import org.aitu.vulnerabilitiesmvp.enums.PaymentStatus;

public record PaymentHistoryQuery(
    int page,
    int size,
    PaymentStatus status,
    String receiverUsername
) {
}
