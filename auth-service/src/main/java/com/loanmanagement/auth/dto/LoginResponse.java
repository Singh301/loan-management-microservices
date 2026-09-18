package com.loanmanagement.auth.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The builder temporarily stores the roles input; the completed DTO defensively copies it.")
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long userId;
    private String username;
    private String fullName;
    private List<String> roles = List.of();
    private long expiresIn;

    @Builder
    public LoginResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            Long userId,
            String username,
            String fullName,
            List<String> roles,
            long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.expiresIn = expiresIn;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
