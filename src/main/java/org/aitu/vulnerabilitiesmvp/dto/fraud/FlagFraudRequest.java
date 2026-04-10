package org.aitu.vulnerabilitiesmvp.dto.fraud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.aitu.vulnerabilitiesmvp.enums.RiskLevel;

public record FlagFraudRequest(
    @NotNull
    RiskLevel riskLevel,

    @NotBlank
    @Size(min = 5, max = 255)
    String reason
) {
}
