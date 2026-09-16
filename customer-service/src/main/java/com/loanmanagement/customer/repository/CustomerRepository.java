package com.loanmanagement.customer.repository;

import com.loanmanagement.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByUserId(Long userId);
    boolean existsByEmail(String email);
    boolean existsByPanNumber(String panNumber);
}
