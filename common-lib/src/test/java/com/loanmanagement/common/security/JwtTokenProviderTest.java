package com.loanmanagement.common.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private static final String VALID_SECRET = "01234567890123456789012345678901";

    @Test
    void shouldRejectMissingSecret() {
        JwtProperties properties = new JwtProperties();
        JwtTokenProvider provider = new JwtTokenProvider(properties);

        assertThrows(IllegalStateException.class, provider::validateSecret);
    }

    @Test
    void shouldRejectShortSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("too-short");
        JwtTokenProvider provider = new JwtTokenProvider(properties);

        assertThrows(IllegalStateException.class, provider::validateSecret);
    }

    @Test
    void shouldGenerateAndValidateAccessToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(VALID_SECRET);

        JwtTokenProvider provider = new JwtTokenProvider(properties);
        provider.validateSecret();

        String token = provider.generateAccessToken("sudhanshu", 1L, List.of("ROLE_ADMIN"));

        assertTrue(provider.validateAccessToken(token));
        assertEquals("sudhanshu", provider.getUsername(token));
        assertEquals(1L, provider.getUserId(token));
        assertEquals(List.of("ROLE_ADMIN"), provider.getRoles(token));
    }
}
