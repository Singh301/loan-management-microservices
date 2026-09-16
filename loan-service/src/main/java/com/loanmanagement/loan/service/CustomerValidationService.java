package com.loanmanagement.loan.service;

import com.loanmanagement.common.dto.ApiResponse;
import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.common.exception.ResourceNotFoundException;
import com.loanmanagement.loan.client.CustomerClient;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerValidationService {

    private final CustomerClient customerClient;

    @Retry(name = "customerService")
    @CircuitBreaker(name = "customerService", fallbackMethod = "validateFallback")
    public void validate(Long customerId) {
        if (customerId == null) {
            throw new DomainException("Customer ID is required");
        }
        try {
            ApiResponse<Map<String, Object>> response = customerClient.getById(customerId);
            if (response == null || response.getData() == null) {
                throw new ResourceNotFoundException("Customer", customerId);
            }
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Customer", customerId);
        }
    }

    private void validateFallback(Long customerId, Throwable cause) {
        log.error("Customer service unavailable while validating customer {}", customerId, cause);
        throw new DomainException("Customer service is temporarily unavailable. Please try again later.");
    }
}
