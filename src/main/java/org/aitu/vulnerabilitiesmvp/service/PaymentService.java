package org.aitu.vulnerabilitiesmvp.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.aitu.vulnerabilitiesmvp.config.AppProperties;
import org.aitu.vulnerabilitiesmvp.dto.common.PagedResponse;
import org.aitu.vulnerabilitiesmvp.dto.payment.CreatePaymentRequest;
import org.aitu.vulnerabilitiesmvp.dto.payment.PaymentHistoryQuery;
import org.aitu.vulnerabilitiesmvp.dto.payment.PaymentResponse;
import org.aitu.vulnerabilitiesmvp.entity.Account;
import org.aitu.vulnerabilitiesmvp.entity.Payment;
import org.aitu.vulnerabilitiesmvp.entity.User;
import org.aitu.vulnerabilitiesmvp.enums.AuditEventType;
import org.aitu.vulnerabilitiesmvp.enums.AuditOutcome;
import org.aitu.vulnerabilitiesmvp.enums.PaymentStatus;
import org.aitu.vulnerabilitiesmvp.exception.BusinessConflictException;
import org.aitu.vulnerabilitiesmvp.exception.InsufficientFundsException;
import org.aitu.vulnerabilitiesmvp.exception.ResourceNotFoundException;
import org.aitu.vulnerabilitiesmvp.mapper.PaymentMapper;
import org.aitu.vulnerabilitiesmvp.repository.AccountRepository;
import org.aitu.vulnerabilitiesmvp.repository.PaymentRepository;
import org.aitu.vulnerabilitiesmvp.security.AppUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final PaymentMapper paymentMapper;
    private final FraudService fraudService;
    private final AuditService auditService;
    private final AuthorizationService authorizationService;
    private final AppProperties appProperties;
    private final InputNormalizationService inputNormalizationService;

    public PaymentService(
        PaymentRepository paymentRepository,
        AccountRepository accountRepository,
        PaymentMapper paymentMapper,
        FraudService fraudService,
        AuditService auditService,
        AuthorizationService authorizationService,
        AppProperties appProperties,
        InputNormalizationService inputNormalizationService
    ) {
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.paymentMapper = paymentMapper;
        this.fraudService = fraudService;
        this.auditService = auditService;
        this.authorizationService = authorizationService;
        this.appProperties = appProperties;
        this.inputNormalizationService = inputNormalizationService;
    }

    @Transactional
    public PaymentResponse createPayment(Long currentUserId, String currentUsername, CreatePaymentRequest request) {
        Account sourceAccount = accountRepository.findByIdAndOwnerId(request.sourceAccountId(), currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
        Account receiverAccount = accountRepository.findById(request.receiverAccountId())
            .orElseThrow(() -> new ResourceNotFoundException("Receiver account not found"));

        validateCreateRequest(currentUserId, sourceAccount, receiverAccount, request);
        User owner = sourceAccount.getOwner();
        User receiver = receiverAccount.getOwner();

        Payment payment = new Payment();
        payment.setOwnerUser(owner);
        payment.setOwnerAccount(sourceAccount);
        payment.setReceiverUser(receiver);
        payment.setReceiverAccount(receiverAccount);
        payment.setAmount(request.amount().setScale(2));
        payment.setCurrency(request.currency());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setDescription(inputNormalizationService.normalizeFreeText(request.description(), 255, "description"));

        Payment savedPayment = paymentRepository.save(payment);
        fraudService.evaluateOnCreate(savedPayment);
        auditService.record(
            AuditEventType.PAYMENT_CREATED,
            currentUsername,
            "PAYMENT",
            savedPayment.getId(),
            AuditOutcome.SUCCESS,
            "Payment created"
        );
        return paymentMapper.toResponse(savedPayment);
    }

    @Transactional
    public PaymentResponse confirmPayment(Long paymentId, AppUserPrincipal principal) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        authorizationService.requirePaymentConfirmationAccess(payment, principal);

        if (payment.getStatus() == PaymentStatus.CONFIRMED) {
            throw new BusinessConflictException("Payment has already been confirmed");
        }
        if (payment.getStatus() == PaymentStatus.REJECTED) {
            throw new BusinessConflictException("Rejected payment cannot be confirmed");
        }

        LockedAccounts lockedAccounts = lockPaymentAccounts(payment);
        Account sourceAccount = lockedAccounts.sourceAccount();
        Account receiverAccount = lockedAccounts.receiverAccount();

        if (sourceAccount.getBalance().compareTo(payment.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient balance for payment confirmation");
        }

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(payment.getAmount()));
        receiverAccount.setBalance(receiverAccount.getBalance().add(payment.getAmount()));

        payment.setStatus(PaymentStatus.CONFIRMED);
        payment.setConfirmedAt(Instant.now());
        fraudService.evaluateOnConfirm(payment);

        auditService.record(
            AuditEventType.PAYMENT_CONFIRMED,
            principal.getUsername(),
            "PAYMENT",
            payment.getId(),
            AuditOutcome.SUCCESS,
            "Payment confirmed atomically"
        );
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getPaymentHistory(AppUserPrincipal principal, PaymentHistoryQuery query) {
        authorizationService.requirePaymentHistoryAccess(principal);
        Page<Payment> payments = findPaymentHistoryPage(principal, query);
        return new PagedResponse<>(
            payments.getContent().stream().map(paymentMapper::toResponse).toList(),
            payments.getNumber(),
            payments.getSize(),
            payments.getTotalElements(),
            payments.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentHistoryEntries(AppUserPrincipal principal, PaymentHistoryQuery query) {
        authorizationService.requirePaymentHistoryAccess(principal);
        return findPaymentHistoryPage(principal, query).getContent().stream().map(paymentMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId, AppUserPrincipal principal) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        authorizationService.requirePaymentReadAccess(payment, principal);
        return paymentMapper.toResponse(payment);
    }

    private void validateCreateRequest(
        Long currentUserId,
        Account sourceAccount,
        Account receiverAccount,
        CreatePaymentRequest request
    ) {
        if (request.sourceAccountId().equals(request.receiverAccountId())) {
            throw new BusinessConflictException("Self-transfer is not allowed");
        }
        if (receiverAccount.getOwner().getId().equals(currentUserId)) {
            throw new BusinessConflictException("Transfers to your own account are not allowed");
        }
        if (sourceAccount.getCurrency() != request.currency() || receiverAccount.getCurrency() != request.currency()) {
            throw new BusinessConflictException("Payment currency must match both account currencies");
        }
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessConflictException("Payment amount must be greater than zero");
        }
    }

    private LockedAccounts lockPaymentAccounts(Payment payment) {
        Long sourceId = payment.getOwnerAccount().getId();
        Long receiverId = payment.getReceiverAccount().getId();
        Long firstId = Math.min(sourceId, receiverId);
        Long secondId = Math.max(sourceId, receiverId);

        Account first = accountRepository.findByIdForUpdate(firstId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        Account second = accountRepository.findByIdForUpdate(secondId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        Account sourceAccount = first.getId().equals(sourceId) ? first : second;
        Account receiverAccount = first.getId().equals(receiverId) ? first : second;
        return new LockedAccounts(sourceAccount, receiverAccount);
    }

    private Page<Payment> findPaymentHistoryPage(AppUserPrincipal principal, PaymentHistoryQuery query) {
        int normalizedSize = Math.min(query.size(), Math.min(
            appProperties.getPayments().getMaxPageSize(),
            appProperties.getExports().getMaxRows()
        ));
        String normalizedReceiverUsername = inputNormalizationService.normalizeUsernameFilter(
            query.receiverUsername(),
            "receiverUsername"
        );
        return paymentRepository.findHistoryByOwnerAndFilters(
            principal.getId(),
            query.status(),
            normalizedReceiverUsername,
            PageRequest.of(query.page(), normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    private record LockedAccounts(Account sourceAccount, Account receiverAccount) {
    }
}
