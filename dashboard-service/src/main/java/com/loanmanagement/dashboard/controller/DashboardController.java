package com.loanmanagement.dashboard.controller;

import com.loanmanagement.common.dto.ApiResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring dependency injection intentionally retains the managed RestTemplate bean.")
public class DashboardController {

    private final RestTemplate restTemplate;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Dashboard aggregate");
        data.put("status", "OK");
        data.put("generatedAt", java.time.Instant.now());
        data.put("loanStatistics", getLoanStatistics(auth));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/monthly-report")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> monthlyReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        LocalDate now = LocalDate.now();
        int reportYear = year != null ? year : now.getYear();
        int reportMonth = month != null ? month : now.getMonthValue();
        return ResponseEntity.ok(ApiResponse.success(getMonthlyReport(reportYear, reportMonth, auth)));
    }

    @GetMapping("/loan-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loanSummary(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return summary(auth);
    }

    private Object getLoanStatistics(String auth) {
        try {
            return restTemplate.exchange(
                    "http://loan-service/api/v1/loans/statistics",
                    HttpMethod.GET,
                    requestEntity(auth),
                    Map.class).getBody();
        } catch (Exception e) {
            return "unavailable";
        }
    }

    private Map<String, Object> getMonthlyReport(int year, int month, String auth) {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://loan-service/api/v1/loans/reports/monthly?year={year}&month={month}",
                    HttpMethod.GET,
                    requestEntity(auth),
                    Map.class,
                    year, month);
            return response.getBody();
        } catch (Exception e) {
            Map<String, Object> unavailable = new HashMap<>();
            unavailable.put("year", year);
            unavailable.put("month", month);
            unavailable.put("status", "unavailable");
            return unavailable;
        }
    }

    private HttpEntity<Void> requestEntity(String auth) {
        HttpHeaders headers = new HttpHeaders();
        if (auth != null) {
            headers.set("Authorization", auth);
        }
        return new HttpEntity<>(headers);
    }
}
