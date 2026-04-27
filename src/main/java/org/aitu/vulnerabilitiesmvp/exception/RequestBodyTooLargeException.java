package org.aitu.vulnerabilitiesmvp.exception;

public class RequestBodyTooLargeException extends RuntimeException {

    public RequestBodyTooLargeException(String message) {
        super(message);
    }
}
