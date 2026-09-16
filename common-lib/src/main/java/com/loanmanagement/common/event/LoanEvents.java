package com.loanmanagement.common.event;

public final class LoanEvents {
    public static final String LOAN_APPLIED = "loan.applied";
    public static final String LOAN_APPROVED = "loan.approved";
    public static final String LOAN_REJECTED = "loan.rejected";
    public static final String LOAN_DISBURSED = "loan.disbursed";
    public static final String LOAN_STATUS_CHANGED = "loan.status.changed";
    public static final String LOAN_OVERDUE = "loan.overdue";
    public static final String LOAN_NPA = "loan.npa";
    public static final String LOAN_CLOSED = "loan.closed";
    public static final String LOAN_FORECLOSED = "loan.foreclosed";

    private LoanEvents() {}
}
