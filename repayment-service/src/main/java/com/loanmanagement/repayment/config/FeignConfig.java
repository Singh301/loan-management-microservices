package com.loanmanagement.repayment.config;

import com.loanmanagement.common.constants.ApiConstants;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@RequiredArgsConstructor
public class FeignConfig {

    @Bean
    public RequestInterceptor authorizationAndRequestIdForwardingInterceptor() {
        return template -> {
            if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
                return;
            }

            var request = attributes.getRequest();
            String authorization = request.getHeader("Authorization");
            if (StringUtils.hasText(authorization)) {
                template.header("Authorization", authorization);
            }

            String requestId = request.getHeader(ApiConstants.HEADER_CORRELATION_ID);
            if (StringUtils.hasText(requestId)) {
                template.header(ApiConstants.HEADER_CORRELATION_ID, requestId);
            }
        };
    }
}
