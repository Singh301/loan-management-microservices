package com.loanmanagement.repayment.scheduler;

import com.loanmanagement.repayment.service.EmiScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OverdueScheduler {

    private final EmiScheduleService emiScheduleService;

    @Scheduled(cron = "0 0 1 * * *") // every day at 1 AM
    public void markOverdueEmis() {
        log.info("Running overdue EMI scheduler");
        emiScheduleService.markOverdue();
    }
}
