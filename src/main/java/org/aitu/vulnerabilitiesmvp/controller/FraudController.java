package org.aitu.vulnerabilitiesmvp.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.aitu.vulnerabilitiesmvp.config.AppProperties;
import org.aitu.vulnerabilitiesmvp.dto.common.PagedResponse;
import org.aitu.vulnerabilitiesmvp.dto.fraud.FlagFraudRequest;
import org.aitu.vulnerabilitiesmvp.dto.fraud.FraudFlagResponse;
import org.aitu.vulnerabilitiesmvp.security.AppUserPrincipal;
import org.aitu.vulnerabilitiesmvp.service.FraudService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/fraud")
public class FraudController {

    private final FraudService fraudService;
    private final AppProperties appProperties;

    public FraudController(FraudService fraudService, AppProperties appProperties) {
        this.fraudService = fraudService;
        this.appProperties = appProperties;
    }

    @PostMapping("/payments/{id}/flag")
    public FraudFlagResponse flagPayment(
        @PathVariable("id") Long paymentId,
        @AuthenticationPrincipal AppUserPrincipal principal,
        @Valid @RequestBody FlagFraudRequest request
    ) {
        return fraudService.flagPaymentManually(paymentId, principal.getId(), principal.getUsername(), request);
    }

    @GetMapping("/flags")
    public PagedResponse<FraudFlagResponse> getFlags(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(required = false) @Min(1) Integer size
    ) {
        int requestedSize = size == null ? appProperties.getPayments().getDefaultPageSize() : size;
        return fraudService.getFlags(page, requestedSize);
    }
}
