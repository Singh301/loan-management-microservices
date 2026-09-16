package com.loanmanagement.repayment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public <T> T executeWithLock(String lockName, Supplier<T> task) {
        try {
            jdbcTemplate.queryForObject(
                    "SELECT lock_name FROM scheduler_locks WHERE lock_name = ? FOR UPDATE NOWAIT",
                    String.class,
                    lockName);
            return task.get();
        } catch (CannotAcquireLockException ex) {
            log.debug("Another repayment-service instance owns scheduler lock {}", lockName);
            return null;
        }
    }
}
