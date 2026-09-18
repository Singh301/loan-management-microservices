package com.loanmanagement.document.client;

import com.loanmanagement.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "customer-service", path = "/api/v1/customers")
public interface CustomerClient {

    @GetMapping("/me")
    ApiResponse<Map<String, Object>> getCurrentCustomer();
}
