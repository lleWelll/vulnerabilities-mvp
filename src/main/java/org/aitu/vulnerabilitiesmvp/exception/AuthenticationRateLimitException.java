package org.aitu.vulnerabilitiesmvp.exception;

public class AuthenticationRateLimitException extends RuntimeException {

    public AuthenticationRateLimitException(String message) {
        super(message);
    }
}
