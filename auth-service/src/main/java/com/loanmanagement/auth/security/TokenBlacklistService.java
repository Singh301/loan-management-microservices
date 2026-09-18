package com.loanmanagement.auth.security;

import com.loanmanagement.common.security.JwtProperties;
import com.loanmanagement.common.security.TokenHash;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring injects and manages the Redis template and JWT properties as shared application components.")
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    public void blacklist(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + TokenHash.sha256(token),
                "1",
                jwtProperties.getExpiration(),
                TimeUnit.MILLISECONDS
        );
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        return Boolean.TRUE.equals(
                redisTemplate.hasKey(BLACKLIST_PREFIX + TokenHash.sha256(token))
        );
    }
}
