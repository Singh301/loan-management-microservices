package com.loanmanagement.auth.security;

import com.loanmanagement.common.security.JwtProperties;
import com.loanmanagement.common.security.TokenHash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void shouldUseHashedKeyForBlacklistWriteAndLookup() {
        JwtProperties properties = new JwtProperties();
        properties.setExpiration(900000);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        TokenBlacklistService service = new TokenBlacklistService(redisTemplate, properties);

        String token = "access-token-sensitive-value";
        String expectedKey = "token:blacklist:" + TokenHash.sha256(token);

        service.blacklist(token);
        boolean blacklisted = service.isBlacklisted(token);

        verify(valueOperations).set(expectedKey, "1", 900000, TimeUnit.MILLISECONDS);
        verify(redisTemplate).hasKey(expectedKey);
        assertEquals(true, blacklisted);
    }
}
