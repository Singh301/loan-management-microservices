package com.loanmanagement.auth.repository;

import com.loanmanagement.auth.entity.RefreshToken;
import com.loanmanagement.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
 
    @Modifying
    int deleteByExpiryDateBefore(LocalDateTime expiryDate);

    @Modifying
    void deleteByUser(User user);
}
