package com.loanmanagement.auth.service;

import com.loanmanagement.auth.dto.CreateUserRequest;
import com.loanmanagement.auth.dto.LoginRequest;
import com.loanmanagement.auth.dto.LoginResponse;
import com.loanmanagement.auth.dto.RefreshTokenRequest;
import com.loanmanagement.auth.dto.UpdateUserRequest;
import com.loanmanagement.auth.dto.UserResponse;
import com.loanmanagement.auth.entity.RefreshToken;
import com.loanmanagement.auth.entity.User;
import com.loanmanagement.auth.repository.RefreshTokenRepository;
import com.loanmanagement.auth.repository.UserRepository;
import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.common.exception.ResourceNotFoundException;
import com.loanmanagement.common.security.JwtProperties;
import com.loanmanagement.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import com.loanmanagement.auth.security.TokenBlacklistService;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;
    private final TokenBlacklistService tokenBlacklistService;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;


    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new DomainException("Invalid username or password", HttpStatus.UNAUTHORIZED));

        if (!user.isEnabled()) {
            throw new DomainException("Account is disabled", HttpStatus.FORBIDDEN);
        }

        if (user.isAccountLocked()) {
            if (user.getLockTime() != null &&
                    user.getLockTime().plusMinutes(LOCK_DURATION_MINUTES).isAfter(LocalDateTime.now())) {
                throw new DomainException("Account is locked. Try again later.", HttpStatus.LOCKED);
            }
            user.setAccountLocked(false);
            user.setFailedAttempts(0);
            user.setLockTime(null);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLocked(true);
                user.setLockTime(LocalDateTime.now());
                log.warn("Account locked for user: {}", user.getUsername());
            }
            userRepository.save(user);
            throw new DomainException("Invalid username or password", HttpStatus.UNAUTHORIZED);
        }

        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        user.setLockTime(null);
        userRepository.save(user);

        List<String> roles = List.of("ROLE_" + user.getRole().name());
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getId(), roles);
        String refreshToken = createRefreshToken(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roles(roles)
                .expiresIn(jwtProperties.getExpiration() / 1000)
                .build();
    }

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new DomainException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (stored.isRevoked() || stored.isExpired()) {
            throw new DomainException("Refresh token expired or revoked", HttpStatus.UNAUTHORIZED);
        }

        User user = stored.getUser();
        if (!user.isEnabled()) {
            throw new DomainException("Account is disabled", HttpStatus.FORBIDDEN);
        }

        List<String> roles = List.of("ROLE_" + user.getRole().name());
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getId(), roles);

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        String newRefreshToken = createRefreshToken(user);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roles(roles)
                .expiresIn(jwtProperties.getExpiration() / 1000)
                .build();
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            tokenBlacklistService.blacklist(accessToken);
        }

        if (refreshToken != null) {
            refreshTokenRepository.findByToken(refreshToken).ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            });
        }
    }



    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return toResponse(findUser(id));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DomainException("Username already exists", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DomainException("Email already exists", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(request.getRole())
                .enabled(true)
                .build();
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUser(id);
        userRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DomainException("Email already exists", HttpStatus.CONFLICT);
            }
        });

        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setFullName(request.getFullName().trim());
        user.setRole(request.getRole());
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        user.setUpdatedAt(LocalDateTime.now());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findUser(id);
        user.setDeleted(true);
        user.setEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .accountLocked(user.isAccountLocked())
                .failedAttempts(user.getFailedAttempts())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private String createRefreshToken(User user) {
        String token = jwtTokenProvider.generateRefreshToken(user.getUsername());
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plus(Duration.ofMillis(jwtProperties.getRefreshExpiration())))
                .build();
        refreshTokenRepository.save(refreshToken);
        return token;
    }
}
