package com.loanmanagement.repayment.service;

import com.loanmanagement.common.dto.ApiResponse;
import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.repayment.client.LoanClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoanOwnershipService {

    private final LoanClient loanClient;

    public void validateCustomerAccess(Long loanId, Authentication authentication) {
        if (authentication == null || !hasRole(authentication, "CUSTOMER")) {
            return;
        }

        try {
            ApiResponse<Map<String, Object>> response = loanClient.getById(loanId);
            if (response == null || response.getData() == null) {
                throw denied();
            }
        } catch (FeignException.Forbidden | FeignException.NotFound ex) {
            throw denied();
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private DomainException denied() {
        return new DomainException(
                "Customers can access only their own loan data",
                HttpStatus.FORBIDDEN,
                "LOAN_OWNERSHIP_DENIED");
    }
}
