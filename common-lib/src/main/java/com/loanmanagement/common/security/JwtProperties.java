package com.loanmanagement.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long expiration = 900000;          // 15 min
    private long refreshExpiration = 604800000; // 7 days
}
