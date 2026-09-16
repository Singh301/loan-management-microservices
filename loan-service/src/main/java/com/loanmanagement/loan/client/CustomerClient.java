package com.loanmanagement.loan.client;

import com.loanmanagement.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "customer-service", path = "/api/v1/customers")
public interface CustomerClient {

    @GetMapping("/{id}")
    ApiResponse<Map<String, Object>> getById(@PathVariable("id") Long id);
}
