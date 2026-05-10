package org.aitu.vulnerabilitiesmvp.service;

import java.math.BigDecimal;
import org.aitu.vulnerabilitiesmvp.dto.auth.LoginRequest;
import org.aitu.vulnerabilitiesmvp.dto.auth.LoginResponse;
import org.aitu.vulnerabilitiesmvp.dto.auth.RegisterRequest;
import org.aitu.vulnerabilitiesmvp.dto.auth.RegisterResponse;
import org.aitu.vulnerabilitiesmvp.entity.Account;
import org.aitu.vulnerabilitiesmvp.entity.User;
import org.aitu.vulnerabilitiesmvp.enums.AuditEventType;
import org.aitu.vulnerabilitiesmvp.enums.AuditOutcome;
import org.aitu.vulnerabilitiesmvp.enums.CurrencyCode;
import org.aitu.vulnerabilitiesmvp.enums.Role;
import org.aitu.vulnerabilitiesmvp.exception.AuthenticationRateLimitException;
import org.aitu.vulnerabilitiesmvp.exception.DuplicateResourceException;
import org.aitu.vulnerabilitiesmvp.exception.InvalidCredentialsException;
import org.aitu.vulnerabilitiesmvp.mapper.AuthMapper;
import org.aitu.vulnerabilitiesmvp.repository.AccountRepository;
import org.aitu.vulnerabilitiesmvp.repository.UserRepository;
import org.aitu.vulnerabilitiesmvp.security.AppUserPrincipal;
import org.aitu.vulnerabilitiesmvp.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthMapper authMapper;
    private final AuditService auditService;
    private final InputNormalizationService inputNormalizationService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(
        UserRepository userRepository,
        AccountRepository accountRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        AuthMapper authMapper,
        AuditService auditService,
        InputNormalizationService inputNormalizationService,
        LoginAttemptService loginAttemptService
    ) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.authMapper = authMapper;
        this.auditService = auditService;
        this.inputNormalizationService = inputNormalizationService;
        this.loginAttemptService = loginAttemptService;
    }

    /* normalize
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username is already taken");
        }

        User user = new User();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.CLIENT);
        user.setEnabled(true);
        User savedUser = userRepository.save(user);
     */

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedUsername = inputNormalizationService.normalizeUsername(request.username(), "username");
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new DuplicateResourceException("Username is already taken");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.CLIENT);
        user.setEnabled(true);
        User savedUser = userRepository.save(user);

        Account account = new Account();
        account.setOwner(savedUser);
        account.setCurrency(CurrencyCode.KZT);
        account.setBalance(BigDecimal.ZERO.setScale(2));
        Account savedAccount = accountRepository.save(account);

        auditService.record(
            AuditEventType.USER_REGISTERED,
            savedUser.getUsername(),
            "USER",
            savedUser.getId(),
            AuditOutcome.SUCCESS,
            "Default KZT account created"
        );

        return authMapper.toRegisterResponse(savedUser, savedAccount);
    }

    /* normalize
    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username().trim(), request.password())
            );
     */

    public LoginResponse login(LoginRequest request) {
        String normalizedUsername = inputNormalizationService.normalizeUsername(request.username(), "username");
        // OWASP-10: Authentication Failures - раньше endpoint login принимал неограниченное число попыток.
        // Исправление: до проверки пароля блокируем principal после серии неудачных попыток.
        if (loginAttemptService.isBlocked(normalizedUsername)) {
            auditService.record(
                AuditEventType.LOGIN_FAILED,
                normalizedUsername,
                "AUTH",
                null,
                AuditOutcome.FAILURE,
                "Login throttled"
            );
            throw new AuthenticationRateLimitException("Too many failed login attempts. Try again later.");
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedUsername, request.password())
            );
            AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
            loginAttemptService.recordSuccess(normalizedUsername);
            auditService.record(
                AuditEventType.LOGIN_SUCCEEDED,
                principal.getUsername(),
                "USER",
                principal.getId(),
                AuditOutcome.SUCCESS,
                "Successful login"
            );
            return new LoginResponse(jwtService.generateToken(principal), "Bearer", jwtService.getExpirationSeconds(), principal.getRole());
        } catch (BadCredentialsException | DisabledException ex) {
            auditService.record(
                AuditEventType.LOGIN_FAILED,
                normalizedUsername,
                "AUTH",
                null,
                AuditOutcome.FAILURE,
                "Invalid credentials"
            );
            loginAttemptService.recordFailure(normalizedUsername);
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }
}
