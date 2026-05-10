package org.aitu.vulnerabilitiesmvp.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleValidation(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(error.getField(), error.getDefaultMessage());
        }
        return buildError(HttpStatus.BAD_REQUEST, "Request validation failed", request.getRequestURI(), validationErrors);
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleBadRequest(
        Exception ex,
        HttpServletRequest request) {
        String message = resolveBadRequestMessage(ex);
        log.warn("Bad request at path={} message={}", request.getRequestURI(), message);
        return buildError(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(RequestBodyTooLargeException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handlePayloadTooLarge(
        RequestBodyTooLargeException ex,
        HttpServletRequest request
    ) {
        log.warn("Payload too large at path={}", request.getRequestURI());
        return buildError(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(InvalidInputException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleInvalidInput(
        InvalidInputException ex,
        HttpServletRequest request
    ) {
        log.warn("Invalid input at path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleDuplicate(
        DuplicateResourceException ex,
        HttpServletRequest request
    ) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleNotFound(
        ResourceNotFoundException ex,
        HttpServletRequest request
    ) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
        InvalidCredentialsException ex,
        HttpServletRequest request
    ) {
        return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(AuthenticationRateLimitException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleAuthenticationRateLimit(
        AuthenticationRateLimitException ex,
        HttpServletRequest request
    ) {
        // OWASP-10: Authentication Failures - throttling должен возвращать контролируемый 429,
        // а не сливаться с generic 500/400 обработкой исключений.
        log.warn("Authentication throttled at path={}", request.getRequestURI());
        return buildError(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleForbidden(
        ForbiddenOperationException ex,
        HttpServletRequest request
    ) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler({BusinessConflictException.class, InsufficientFundsException.class})
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleBusinessConflict(
        RuntimeException ex,
        HttpServletRequest request
    ) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleDataIntegrity(
//        DataIntegrityViolationException ex,
        HttpServletRequest request
    ) {
        log.warn("Data integrity violation at path={}", request.getRequestURI());
        return buildError(HttpStatus.CONFLICT, "Request could not be completed", request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleUnexpected(
        Exception ex,
        HttpServletRequest request
    ) {
        log.error("Unexpected error at path={}", request.getRequestURI(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request.getRequestURI(), Map.of());
    }

    private org.springframework.http.ResponseEntity<ApiErrorResponse> buildError(
        HttpStatus status,
        String message,
        String path,
        Map<String, String> validationErrors
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            path,
            validationErrors
        );
        return org.springframework.http.ResponseEntity.status(status).body(body);
    }

    private String resolveBadRequestMessage(Exception ex) {
        Throwable cause = mostSpecificCause(ex);
        if (cause instanceof StreamConstraintsException) {
            return "Request JSON exceeds parsing safety limits";
        }
        if (cause instanceof JsonParseException) {
            return "Malformed JSON request body";
        }
        if (cause instanceof JsonMappingException) {
            return "Malformed or invalid request";
        }
        return "Malformed or invalid request";
    }

    private Throwable mostSpecificCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
