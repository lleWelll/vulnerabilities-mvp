package org.aitu.vulnerabilitiesmvp.mapper;

import org.aitu.vulnerabilitiesmvp.dto.payment.PaymentResponse;
import org.aitu.vulnerabilitiesmvp.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getOwnerAccount().getId(),
            payment.getReceiverAccount().getId(),
            payment.getReceiverUser().getUsername(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getStatus(),
            payment.isFlagged(),
            payment.getDescription(),
            payment.getCreatedAt(),
            payment.getConfirmedAt()
        );
    }
}
