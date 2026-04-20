package org.aitu.vulnerabilitiesmvp.controller;

import jakarta.validation.Valid;
import org.aitu.vulnerabilitiesmvp.dto.auth.CsrfTokenResponse;
import org.aitu.vulnerabilitiesmvp.dto.auth.LoginRequest;
import org.aitu.vulnerabilitiesmvp.dto.auth.LoginResponse;
import org.aitu.vulnerabilitiesmvp.dto.auth.RegisterRequest;
import org.aitu.vulnerabilitiesmvp.dto.auth.RegisterResponse;
import org.aitu.vulnerabilitiesmvp.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(CsrfToken csrfToken) {
        return new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getParameterName(), csrfToken.getToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
