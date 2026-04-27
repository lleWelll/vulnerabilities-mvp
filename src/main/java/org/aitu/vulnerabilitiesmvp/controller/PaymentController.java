package org.aitu.vulnerabilitiesmvp.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.aitu.vulnerabilitiesmvp.config.AppProperties;
import org.aitu.vulnerabilitiesmvp.dto.common.PagedResponse;
import org.aitu.vulnerabilitiesmvp.dto.payment.CreatePaymentRequest;
import org.aitu.vulnerabilitiesmvp.dto.payment.PaymentExportFormat;
import org.aitu.vulnerabilitiesmvp.dto.payment.PaymentHistoryQuery;
import org.aitu.vulnerabilitiesmvp.dto.payment.PaymentResponse;
import org.aitu.vulnerabilitiesmvp.security.AppUserPrincipal;
import org.aitu.vulnerabilitiesmvp.service.PaymentExportService;
import org.aitu.vulnerabilitiesmvp.service.PaymentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
    private final PaymentExportService paymentExportService;
    private final AppProperties appProperties;

    public PaymentController(
        PaymentService paymentService,
        PaymentExportService paymentExportService,
        AppProperties appProperties
    ) {
        this.paymentService = paymentService;
        this.paymentExportService = paymentExportService;
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
        @RequestParam(required = false) @Min(1) Integer size,
        @RequestParam(required = false) org.aitu.vulnerabilitiesmvp.enums.PaymentStatus status,
        @RequestParam(required = false) @Size(max = 50)
        @Pattern(
            regexp = "^[A-Za-z0-9._-]+$",
            message = "receiverUsername must contain only letters, digits, dots, underscores, and hyphens"
        ) String receiverUsername
    ) {
        int requestedSize = size == null ? appProperties.getPayments().getDefaultPageSize() : size;
        return paymentService.getPaymentHistory(principal, new PaymentHistoryQuery(page, requestedSize, status, receiverUsername));
    }

    @GetMapping("/history/export")
    public ResponseEntity<ByteArrayResource> exportHistory(
        @AuthenticationPrincipal AppUserPrincipal principal,
        @RequestParam(defaultValue = "CSV") PaymentExportFormat format,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(required = false) @Min(1) Integer size,
        @RequestParam(required = false) org.aitu.vulnerabilitiesmvp.enums.PaymentStatus status,
        @RequestParam(required = false) @Size(max = 50)
        @Pattern(
            regexp = "^[A-Za-z0-9._-]+$",
            message = "receiverUsername must contain only letters, digits, dots, underscores, and hyphens"
        ) String receiverUsername,
        @RequestParam(required = false) @Size(max = 64)
        @Pattern(
            regexp = "^[A-Za-z0-9._-]+$",
            message = "fileName must contain only letters, digits, dots, underscores, and hyphens"
        ) String fileName
    ) {
        int requestedSize = size == null ? appProperties.getExports().getMaxRows() : size;
        PaymentExportService.ExportedPayload payload = paymentExportService.exportHistory(
            principal,
            new PaymentHistoryQuery(page, requestedSize, status, receiverUsername),
            format,
            fileName
        );
        return ResponseEntity.ok()
            .contentType(payload.mediaType())
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(payload.fileName()).build().toString()
            )
            .body(new ByteArrayResource(payload.content()));
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(
        @PathVariable("id") Long paymentId,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return paymentService.getPayment(paymentId, principal);
    }
}
