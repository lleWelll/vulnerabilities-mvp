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

    public AuthService(
        UserRepository userRepository,
        AccountRepository accountRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        AuthMapper authMapper,
        AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.authMapper = authMapper;
        this.auditService = auditService;
    }

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

    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username().trim(), request.password())
            );
            AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
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
                request.username().trim(),
                "AUTH",
                null,
                AuditOutcome.FAILURE,
                "Invalid credentials"
            );
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }
}
