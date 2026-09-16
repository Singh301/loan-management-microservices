package com.loanmanagement.common.security;

import com.loanmanagement.common.constants.ApiConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Propagates a request correlation id through the service and MDC.
 * The API gateway is responsible for creating the id when the client does not provide one.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String MDC_KEY = "correlationId";
    private static final int MAX_CORRELATION_ID_LENGTH = 100;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(ApiConstants.HEADER_CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()
                || correlationId.length() > MAX_CORRELATION_ID_LENGTH) {
            correlationId = UUID.randomUUID().toString();
        }

        response.setHeader(ApiConstants.HEADER_CORRELATION_ID, correlationId);
        MDC.put(MDC_KEY, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
