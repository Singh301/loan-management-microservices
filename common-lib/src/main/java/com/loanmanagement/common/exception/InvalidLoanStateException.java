package com.loanmanagement.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidLoanStateException extends DomainException {
    public InvalidLoanStateException(String currentStatus, String targetStatus) {
        super("Illegal transition from " + currentStatus + " to " + targetStatus,
                HttpStatus.CONFLICT, "INVALID_LOAN_STATE");
    }

    public InvalidLoanStateException(String message) {
        super(message, HttpStatus.CONFLICT, "INVALID_LOAN_STATE");
    }
}
