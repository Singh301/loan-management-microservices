package com.loanmanagement.repayment.scheduler;

import com.loanmanagement.repayment.service.DistributedLockService;
import com.loanmanagement.repayment.service.EmiScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OverdueScheduler {

    private static final String LOCK_NAME = "repayment-overdue-scheduler";

    private final EmiScheduleService emiScheduleService;
    private final DistributedLockService distributedLockService;

    @Scheduled(cron = "${repayment.scheduler.overdue-cron:0 0 1 * * *}")
    public void markOverdueEmis() {
        if (!distributedLockService.tryLock(LOCK_NAME)) {
            log.debug("Another repayment-service instance owns the overdue scheduler lock");
            return;
        }
        try {
            log.info("Running overdue EMI scheduler");
            emiScheduleService.markOverdue();
        } finally {
            distributedLockService.unlock(LOCK_NAME);
        }
    }
}
