package com.loanmanagement.document.service;

import com.loanmanagement.common.dto.ApiResponse;
import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.document.client.CustomerClient;
import com.loanmanagement.document.client.LoanClient;
import com.loanmanagement.document.entity.Document;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentOwnershipService {

    private final CustomerClient customerClient;
    private final LoanClient loanClient;

    public Long currentCustomerId(Authentication authentication) {
        if (!isCustomer(authentication)) {
            return null;
        }

        try {
            ApiResponse<Map<String, Object>> response = customerClient.getCurrentCustomer();
            if (response == null || response.getData() == null || response.getData().get("id") == null) {
                throw denied();
            }
            return Long.valueOf(response.getData().get("id").toString());
        } catch (FeignException.Forbidden | FeignException.NotFound ex) {
            throw denied();
        }
    }

    public Long resolveUploadCustomerId(
            Long loanId, Long customerId, Authentication authentication) {
        Long currentCustomerId = currentCustomerId(authentication);
        if (currentCustomerId == null) {
            return customerId;
        }

        if (customerId != null && !currentCustomerId.equals(customerId)) {
            throw denied();
        }

        if (loanId != null) {
            validateLoanAccess(loanId);
        } else if (customerId == null) {
            throw new DomainException(
                    "Customer uploads must be associated with a loan or customer",
                    HttpStatus.BAD_REQUEST,
                    "DOCUMENT_OWNER_REQUIRED");
        }

        return currentCustomerId;
    }

    public void validateLoanAccess(Long loanId, Authentication authentication) {
        if (!isCustomer(authentication)) {
            return;
        }
        validateLoanAccess(loanId);
    }

    private void validateLoanAccess(Long loanId) {
        try {
            ApiResponse<Map<String, Object>> response = loanClient.getById(loanId);
            if (response == null || response.getData() == null) {
                throw denied();
            }
        } catch (FeignException.Forbidden | FeignException.NotFound ex) {
            throw denied();
        }
    }

    public void validateDocumentAccess(Document document, Authentication authentication) {
        if (!isCustomer(authentication)) {
            return;
        }

        if (document.getLoanId() != null) {
            validateLoanAccess(document.getLoanId());
            return;
        }

        Long currentCustomerId = currentCustomerId(authentication);
        if (currentCustomerId == null || !currentCustomerId.equals(document.getCustomerId())) {
            throw denied();
        }
    }

    private boolean isCustomer(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
    }

    private DomainException denied() {
        return new DomainException(
                "Customers can access only their own documents",
                HttpStatus.FORBIDDEN,
                "DOCUMENT_OWNERSHIP_DENIED");
    }
}
