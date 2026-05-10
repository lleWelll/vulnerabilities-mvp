package org.aitu.vulnerabilitiesmvp.service;

import org.aitu.vulnerabilitiesmvp.entity.Payment;
import org.aitu.vulnerabilitiesmvp.enums.AuditEventType;
import org.aitu.vulnerabilitiesmvp.enums.AuditOutcome;
import org.aitu.vulnerabilitiesmvp.exception.ForbiddenOperationException;
import org.aitu.vulnerabilitiesmvp.security.AppUserPrincipal;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final AuditService auditService;

    public AuthorizationService(AuditService auditService) {
        this.auditService = auditService;
    }

    public void requirePaymentHistoryAccess(AppUserPrincipal principal) {
        if (principal.isClient()) {
            return;
        }
        deny(
            principal,
            "PAYMENT_HISTORY",
            null,
            "Only clients may access payment history",
            "You are not allowed to access payment history"
        );
    }

    public void requirePaymentReadAccess(Payment payment, AppUserPrincipal principal) {
        if (payment.getOwnerUser().getId().equals(principal.getId())) {
            return;
        }

        //if (principal.isOperator()) {
        //            return;
        //        }
        if (principal.isOperator() && payment.isFlagged()) {
            return;
        }

        String auditMessage = principal.isOperator()
            ? "Operator attempted to access unflagged payment"
            : "Attempt to access payment owned by another user";
        deny(
            principal,
            "PAYMENT",
            payment.getId(),
            auditMessage,
            "You are not allowed to access this payment"
        );
    }

    public void requirePaymentConfirmationAccess(Payment payment, AppUserPrincipal principal) {
        if (!principal.isClient()) {
            deny(
                principal,
                "PAYMENT",
                payment.getId(),
                "Non-client attempted to confirm payment",
                "You are not allowed to confirm this payment"
            );
        }
        if (!payment.getOwnerUser().getId().equals(principal.getId())) {
            deny(
                principal,
                "PAYMENT",
                payment.getId(),
                "Client attempted to confirm payment owned by another user",
                "You are not allowed to confirm this payment"
            );
        }
    }

    private void deny(
        AppUserPrincipal principal,
        String targetType,
        Long targetId,
        String auditMessage,
        String userMessage
    ) {
        auditService.record(
            AuditEventType.ACCESS_DENIED,
            principal.getUsername(),
            targetType,
            targetId,
            AuditOutcome.FAILURE,
            auditMessage
        );
        throw new ForbiddenOperationException(userMessage);
    }
}
