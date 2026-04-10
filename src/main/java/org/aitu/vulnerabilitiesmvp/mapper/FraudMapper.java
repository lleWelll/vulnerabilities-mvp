package org.aitu.vulnerabilitiesmvp.mapper;

import org.aitu.vulnerabilitiesmvp.dto.fraud.FraudFlagResponse;
import org.aitu.vulnerabilitiesmvp.entity.FraudFlag;
import org.springframework.stereotype.Component;

@Component
public class FraudMapper {

    public FraudFlagResponse toResponse(FraudFlag fraudFlag) {
        return new FraudFlagResponse(
            fraudFlag.getId(),
            fraudFlag.getPayment().getId(),
            fraudFlag.getPayment().getOwnerUser().getUsername(),
            fraudFlag.getPayment().getReceiverUser().getUsername(),
            fraudFlag.getPayment().getAmount(),
            fraudFlag.getPayment().getCurrency(),
            fraudFlag.getPayment().getStatus(),
            fraudFlag.getRiskLevel(),
            fraudFlag.getReason(),
            fraudFlag.isManual(),
            fraudFlag.getFlaggedBy() != null ? fraudFlag.getFlaggedBy().getUsername() : "SYSTEM",
            fraudFlag.getCreatedAt()
        );
    }
}
