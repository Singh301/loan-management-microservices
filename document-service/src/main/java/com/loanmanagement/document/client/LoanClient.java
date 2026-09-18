package com.loanmanagement.document.client;

import com.loanmanagement.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "loan-service", path = "/api/v1/loans")
public interface LoanClient {

    @GetMapping("/{loanId}")
    ApiResponse<Map<String, Object>> getById(@PathVariable("loanId") Long loanId);
}
