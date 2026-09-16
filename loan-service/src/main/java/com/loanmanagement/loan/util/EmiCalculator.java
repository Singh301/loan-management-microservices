package com.loanmanagement.loan.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class EmiCalculator {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    /**
     * EMI = P * r * (1+r)^n / ((1+r)^n - 1)
     * where r = monthly interest rate, n = tenure in months
     */
    public BigDecimal calculate(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        if (tenureMonths <= 0 || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(1200), MC); // annual % → monthly decimal

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }

        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusR.pow(tenureMonths, MC);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(power);
        BigDecimal denominator = power.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
