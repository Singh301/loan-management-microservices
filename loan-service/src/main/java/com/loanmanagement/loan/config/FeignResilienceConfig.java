package com.loanmanagement.loan.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@RequiredArgsConstructor
public class FeignResilienceConfig {

    @Bean
    public RequestInterceptor authorizationForwardingInterceptor() {
        return template -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();
            String authorization = request.getHeader("Authorization");
            if (StringUtils.hasText(authorization)) {
                template.header("Authorization", authorization);
            }
            String correlationId = request.getHeader("X-Correlation-Id");
            if (StringUtils.hasText(correlationId)) {
                template.header("X-Correlation-Id", correlationId);
            }
        };
    }
}
