package com.loanmanagement.auth.service;

import com.loanmanagement.auth.dto.LoginResponse;
import com.loanmanagement.auth.dto.RefreshTokenRequest;
import com.loanmanagement.auth.entity.RefreshToken;
import com.loanmanagement.auth.entity.User;
import com.loanmanagement.auth.repository.RefreshTokenRepository;
import com.loanmanagement.auth.security.TokenBlacklistService;
import com.loanmanagement.common.security.JwtProperties;
import com.loanmanagement.common.security.JwtTokenProvider;
import com.loanmanagement.common.security.TokenHash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTokenTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private com.loanmanagement.auth.repository.UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRotateRefreshTokenUsingStoredHash() {
        String rawRefreshToken = "refresh-token-123";
        String newRefreshToken = "new-refresh-token-456";

        User user = User.builder()
                .id(1L)
                .username("sudhanshu")
                .role(User.Role.ADMIN)
                .enabled(true)
                .build();

        RefreshToken stored = RefreshToken.builder()
                .id(10L)
                .tokenHash(TokenHash.sha256(rawRefreshToken))
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(rawRefreshToken);

        when(jwtTokenProvider.validateRefreshToken(rawRefreshToken)).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(TokenHash.sha256(rawRefreshToken)))
                .thenReturn(Optional.of(stored));
        when(jwtTokenProvider.generateAccessToken("sudhanshu", 1L, java.util.List.of("ROLE_ADMIN")))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken("sudhanshu"))
                .thenReturn(newRefreshToken);
        when(jwtProperties.getRefreshExpiration()).thenReturn(604800000L);
        when(jwtProperties.getExpiration()).thenReturn(900000L);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = authService.refreshToken(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals(newRefreshToken, response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(1L, response.getUserId());
        assertEquals(java.util.List.of("ROLE_ADMIN"), response.getRoles());

        assertEquals(true, stored.isRevoked());
        verify(refreshTokenRepository).findByTokenHash(eq(TokenHash.sha256(rawRefreshToken)));
        verify(refreshTokenRepository, atLeast(2)).save(any(RefreshToken.class));
    }
}
