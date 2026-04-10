package org.aitu.vulnerabilitiesmvp.service;

import java.time.Instant;
import org.aitu.vulnerabilitiesmvp.config.AppProperties;
import org.aitu.vulnerabilitiesmvp.dto.common.PagedResponse;
import org.aitu.vulnerabilitiesmvp.dto.fraud.FlagFraudRequest;
import org.aitu.vulnerabilitiesmvp.dto.fraud.FraudFlagResponse;
import org.aitu.vulnerabilitiesmvp.entity.FraudFlag;
import org.aitu.vulnerabilitiesmvp.entity.Payment;
import org.aitu.vulnerabilitiesmvp.entity.User;
import org.aitu.vulnerabilitiesmvp.enums.AuditEventType;
import org.aitu.vulnerabilitiesmvp.enums.AuditOutcome;
import org.aitu.vulnerabilitiesmvp.enums.PaymentStatus;
import org.aitu.vulnerabilitiesmvp.enums.RiskLevel;
import org.aitu.vulnerabilitiesmvp.exception.ResourceNotFoundException;
import org.aitu.vulnerabilitiesmvp.mapper.FraudMapper;
import org.aitu.vulnerabilitiesmvp.repository.FraudFlagRepository;
import org.aitu.vulnerabilitiesmvp.repository.PaymentRepository;
import org.aitu.vulnerabilitiesmvp.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudService {

    private final FraudFlagRepository fraudFlagRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final FraudMapper fraudMapper;
    private final AuditService auditService;
    private final AppProperties appProperties;

    public FraudService(
        FraudFlagRepository fraudFlagRepository,
        PaymentRepository paymentRepository,
        UserRepository userRepository,
        FraudMapper fraudMapper,
        AuditService auditService,
        AppProperties appProperties
    ) {
        this.fraudFlagRepository = fraudFlagRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.fraudMapper = fraudMapper;
        this.auditService = auditService;
        this.appProperties = appProperties;
    }

    @Transactional
    public void evaluateOnCreate(Payment payment) {
        if (payment.getAmount().compareTo(appProperties.getFraud().getLargeAmountThreshold()) >= 0) {
            createAutomaticFlag(payment, RiskLevel.HIGH, "Large amount threshold exceeded");
        }

        Instant cutoff = Instant.now().minusSeconds(appProperties.getFraud().getFrequencyWindowMinutes() * 60L);
        long recentPayments = paymentRepository.countByOwnerUserIdAndCreatedAtAfter(payment.getOwnerUser().getId(), cutoff);
        if (recentPayments >= appProperties.getFraud().getFrequencyThreshold()) {
            createAutomaticFlag(payment, RiskLevel.MEDIUM, "Transfer frequency threshold exceeded");
        }
    }

    @Transactional
    public void evaluateOnConfirm(Payment payment) {
        if (payment.getAmount().compareTo(appProperties.getFraud().getLargeAmountThreshold().multiply(new java.math.BigDecimal("2"))) >= 0) {
            createAutomaticFlag(payment, RiskLevel.HIGH, "Confirmed payment exceeds elevated risk threshold");
        }
    }

    @Transactional
    public FraudFlagResponse flagPaymentManually(Long paymentId, Long operatorId, String operatorUsername, FlagFraudRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        User operator = userRepository.findById(operatorId)
            .orElseThrow(() -> new ResourceNotFoundException("Operator not found"));

        FraudFlag fraudFlag = new FraudFlag();
        fraudFlag.setPayment(payment);
        fraudFlag.setRiskLevel(request.riskLevel());
        fraudFlag.setReason(request.reason().trim());
        fraudFlag.setFlaggedBy(operator);
        fraudFlag.setManual(true);
        FraudFlag savedFlag = fraudFlagRepository.save(fraudFlag);

        markPaymentFlagged(payment);
        auditService.record(
            AuditEventType.PAYMENT_FLAGGED,
            operatorUsername,
            "PAYMENT",
            payment.getId(),
            AuditOutcome.SUCCESS,
            "Manual fraud flag created"
        );

        return fraudMapper.toResponse(savedFlag);
    }

    @Transactional(readOnly = true)
    public PagedResponse<FraudFlagResponse> getFlags(int page, int size) {
        int normalizedSize = Math.min(size, appProperties.getPayments().getMaxPageSize());
        Page<FraudFlag> fraudFlags = fraudFlagRepository.findAll(
            PageRequest.of(page, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return new PagedResponse<>(
            fraudFlags.getContent().stream().map(fraudMapper::toResponse).toList(),
            fraudFlags.getNumber(),
            fraudFlags.getSize(),
            fraudFlags.getTotalElements(),
            fraudFlags.getTotalPages()
        );
    }

    private void createAutomaticFlag(Payment payment, RiskLevel riskLevel, String reason) {
        if (fraudFlagRepository.existsByPaymentIdAndReason(payment.getId(), reason)) {
            return;
        }
        FraudFlag fraudFlag = new FraudFlag();
        fraudFlag.setPayment(payment);
        fraudFlag.setRiskLevel(riskLevel);
        fraudFlag.setReason(reason);
        fraudFlag.setManual(false);
        fraudFlagRepository.save(fraudFlag);
        markPaymentFlagged(payment);
        auditService.record(
            AuditEventType.PAYMENT_FLAGGED,
            payment.getOwnerUser().getUsername(),
            "PAYMENT",
            payment.getId(),
            AuditOutcome.SUCCESS,
            reason
        );
    }

    private void markPaymentFlagged(Payment payment) {
        payment.setFlagged(true);
        if (payment.getStatus() == PaymentStatus.CREATED) {
            payment.setStatus(PaymentStatus.FLAGGED);
        }
    }
}
