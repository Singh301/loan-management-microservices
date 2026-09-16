package com.loanmanagement.loan.domain;

import com.loanmanagement.common.exception.InvalidLoanStateException;
import com.loanmanagement.loan.entity.LoanStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Explicit state machine – prevents illegal transitions.
 * Terminal states: REJECTED, CLOSED, WRITTEN_OFF
 */
@Component
public class LoanStateMachine {

    private final Map<LoanStatus, Set<LoanStatus>> transitions = new EnumMap<>(LoanStatus.class);

    public LoanStateMachine() {
        transitions.put(LoanStatus.PENDING, EnumSet.of(LoanStatus.APPROVED, LoanStatus.REJECTED));
        transitions.put(LoanStatus.APPROVED, EnumSet.of(LoanStatus.DISBURSED, LoanStatus.REJECTED));
        transitions.put(LoanStatus.DISBURSED, EnumSet.of(LoanStatus.ACTIVE));
        transitions.put(LoanStatus.ACTIVE, EnumSet.of(LoanStatus.OVERDUE, LoanStatus.CLOSED, LoanStatus.WRITTEN_OFF));
        transitions.put(LoanStatus.OVERDUE, EnumSet.of(LoanStatus.NPA, LoanStatus.ACTIVE, LoanStatus.CLOSED, LoanStatus.WRITTEN_OFF));
        transitions.put(LoanStatus.NPA, EnumSet.of(LoanStatus.WRITTEN_OFF, LoanStatus.CLOSED));
        // Terminal
        transitions.put(LoanStatus.REJECTED, EnumSet.noneOf(LoanStatus.class));
        transitions.put(LoanStatus.CLOSED, EnumSet.noneOf(LoanStatus.class));
        transitions.put(LoanStatus.WRITTEN_OFF, EnumSet.noneOf(LoanStatus.class));
    }

    public void validateTransition(LoanStatus current, LoanStatus target) {
        Set<LoanStatus> allowed = transitions.getOrDefault(current, EnumSet.noneOf(LoanStatus.class));
        if (!allowed.contains(target)) {
            throw new InvalidLoanStateException(current.name(), target.name());
        }
    }

    public boolean canTransition(LoanStatus current, LoanStatus target) {
        return transitions.getOrDefault(current, EnumSet.noneOf(LoanStatus.class)).contains(target);
    }
}
