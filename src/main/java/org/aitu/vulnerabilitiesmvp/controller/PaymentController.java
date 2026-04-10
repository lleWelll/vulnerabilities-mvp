package org.aitu.vulnerabilitiesmvp.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.aitu.vulnerabilitiesmvp.config.AppProperties;
import org.aitu.vulnerabilitiesmvp.dto.common.PagedResponse;
import org.aitu.vulnerabilitiesmvp.dto.payment.CreatePaymentRequest;
import org.aitu.vulnerabilitiesmvp.dto.payment.PaymentResponse;
import org.aitu.vulnerabilitiesmvp.security.AppUserPrincipal;
import org.aitu.vulnerabilitiesmvp.service.PaymentService;
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
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final AppProperties appProperties;

    public PaymentController(PaymentService paymentService, AppProperties appProperties) {
        this.paymentService = paymentService;
        this.appProperties = appProperties;
    }

    @PostMapping
    public PaymentResponse createPayment(
        @AuthenticationPrincipal AppUserPrincipal principal,
        @Valid @RequestBody CreatePaymentRequest request
    ) {
        return paymentService.createPayment(principal.getId(), principal.getUsername(), request);
    }

    @PostMapping("/{id}/confirm")
    public PaymentResponse confirmPayment(
        @PathVariable("id") Long paymentId,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return paymentService.confirmPayment(paymentId, principal);
    }

    @GetMapping("/history")
    public PagedResponse<PaymentResponse> history(
        @AuthenticationPrincipal AppUserPrincipal principal,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(required = false) @Min(1) Integer size
    ) {
        int requestedSize = size == null ? appProperties.getPayments().getDefaultPageSize() : size;
        return paymentService.getPaymentHistory(principal, page, requestedSize);
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(
        @PathVariable("id") Long paymentId,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return paymentService.getPayment(paymentId, principal);
    }
}
