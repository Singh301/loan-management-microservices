package com.loanmanagement.auth.dto;

import com.loanmanagement.auth.entity.User;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class UserResponse {
    Long id;
    String username;
    String email;
    String fullName;
    User.Role role;
    boolean enabled;
    boolean accountLocked;
    int failedAttempts;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
