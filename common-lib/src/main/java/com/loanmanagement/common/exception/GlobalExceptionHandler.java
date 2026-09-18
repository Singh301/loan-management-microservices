package com.loanmanagement.common.exception;

import com.loanmanagement.common.constants.ApiConstants;
import com.loanmanagement.common.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(
            DomainException ex,
            @RequestHeader(value = ApiConstants.HEADER_CORRELATION_ID, required = false) String correlationId) {
        return error(ex.getStatus(), ex.getMessage(), correlationId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            Exception ex,
            @RequestHeader(value = ApiConstants.HEADER_CORRELATION_ID, required = false) String correlationId) {

        String message = "Request validation failed";
        if (ex instanceof MethodArgumentNotValidException validation) {
            message = validation.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .collect(Collectors.joining(", "));
        }

        if (message.isBlank()) {
            message = "Request validation failed";
        }
        return error(HttpStatus.BAD_REQUEST, message, correlationId);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex,
            @RequestHeader(value = ApiConstants.HEADER_CORRELATION_ID, required = false) String correlationId) {
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", correlationId);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            @RequestHeader(value = ApiConstants.HEADER_CORRELATION_ID, required = false) String correlationId) {
        return error(HttpStatus.BAD_REQUEST, "Request body is invalid or malformed", correlationId);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception ex,
            @RequestHeader(value = ApiConstants.HEADER_CORRELATION_ID, required = false) String correlationId) {
        log.error("Unexpected API error [requestId={}]", correlationId, ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", correlationId);
    }

    private ResponseEntity<ApiResponse<Void>> error(
            HttpStatus status, String message, String correlationId) {
        return ResponseEntity.status(status)
                .header(ApiConstants.HEADER_CORRELATION_ID, correlationId == null ? "" : correlationId)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .message(message)
                        .timestamp(java.time.Instant.now())
                        .correlationId(correlationId)
                        .build());
    }
}
