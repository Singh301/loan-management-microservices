package com.loanmanagement.auth.config;

import com.loanmanagement.auth.entity.User;
import com.loanmanagement.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createIfAbsent("admin", "admin@loan.com", "System Admin", User.Role.ADMIN);
        createIfAbsent("manager", "manager@loan.com", "Loan Manager", User.Role.MANAGER);
        createIfAbsent("customer1", "customer1@loan.com", "John Doe", User.Role.CUSTOMER);
    }

    private void createIfAbsent(String username, String email, String fullName, User.Role role) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = User.builder()
                    .username(username)
                    .email(email)
                    .passwordHash(passwordEncoder.encode("Password@123"))
                    .fullName(fullName)
                    .role(role)
                    .enabled(true)
                    .build();
            userRepository.save(user);
            log.info("Seeded user: {} / Password@123", username);
        }
    }
}
