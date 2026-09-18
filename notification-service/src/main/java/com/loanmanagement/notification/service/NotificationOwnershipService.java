package com.loanmanagement.notification.service;

import com.loanmanagement.common.dto.ApiResponse;
import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.notification.client.CustomerClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationOwnershipService {

    private final CustomerClient customerClient;

    public Long currentCustomerId(Authentication authentication) {
        if (authentication == null || !isCustomer(authentication)) {
            return null;
        }

        try {
            ApiResponse<Map<String, Object>> response = customerClient.getCurrentCustomer();
            if (response == null || response.getData() == null || response.getData().get("id") == null) {
                throw denied();
            }
            Object id = response.getData().get("id");
            return Long.valueOf(id.toString());
        } catch (FeignException.Forbidden | FeignException.NotFound ex) {
            throw denied();
        }
    }

    public void validateCustomerAccess(Long customerId, Authentication authentication) {
        Long current = currentCustomerId(authentication);
        if (current != null && !current.equals(customerId)) {
            throw denied();
        }
    }

    private boolean isCustomer(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
    }

    private DomainException denied() {
        return new DomainException(
                "Customers can access only their own notifications",
                HttpStatus.FORBIDDEN,
                "NOTIFICATION_OWNERSHIP_DENIED");
    }
}
