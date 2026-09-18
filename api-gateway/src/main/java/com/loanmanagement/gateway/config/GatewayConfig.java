package com.loanmanagement.gateway.config;

import com.loanmanagement.common.security.JwtTokenProvider;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class GatewayConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(clientIp(exchange));
    }

    @Bean
    @Primary
    KeyResolver userOrIpKeyResolver(JwtTokenProvider jwtTokenProvider) {
        return exchange -> {
            String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7);
                if (jwtTokenProvider.validateToken(token)) {
                    try {
                        Long userId = jwtTokenProvider.getUserId(token);
                        if (userId != null) {
                            return Mono.just("user:" + userId);
                        }
                    } catch (Exception ignored) {
                        // Fall back to client IP for malformed or incompatible tokens.
                    }
                }
            }
            return Mono.just("ip:" + clientIp(exchange));
        };
    }

    private String clientIp(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null) {
            return "unknown";
        }

        if (remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        String hostString = remoteAddress.getHostString();
        return StringUtils.hasText(hostString) ? hostString : "unknown";
    }
}
