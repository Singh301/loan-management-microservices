package com.loanmanagement.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DomainException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public DomainException(String message) {
        this(message, HttpStatus.BAD_REQUEST, "DOMAIN_ERROR");
    }

    public DomainException(String message, HttpStatus status) {
        this(message, status, "DOMAIN_ERROR");
    }

    public DomainException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
