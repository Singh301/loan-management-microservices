package com.loanmanagement.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenHashTest {

    @Test
    void shouldReturnStableSha256Hash() {
        assertEquals(
                "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                TokenHash.sha256("test"));
    }

    @Test
    void shouldRejectBlankToken() {
        assertThrows(IllegalArgumentException.class, () -> TokenHash.sha256(" "));
    }
}
