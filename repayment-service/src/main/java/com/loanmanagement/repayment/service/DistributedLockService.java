package com.loanmanagement.repayment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    private final JdbcTemplate jdbcTemplate;

    public boolean tryLock(String lockName) {
        Integer acquired = jdbcTemplate.queryForObject(
                "SELECT GET_LOCK(?, 0)", Integer.class, lockName);
        return Integer.valueOf(1).equals(acquired);
    }

    public void unlock(String lockName) {
        try {
            jdbcTemplate.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, lockName);
        } catch (Exception ex) {
            log.warn("Unable to release distributed lock {}", lockName, ex);
        }
    }
}
