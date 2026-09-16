package com.loanmanagement.loan.dto;

import com.loanmanagement.loan.entity.LoanProduct;
import com.loanmanagement.loan.entity.LoanType;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanProductDto {
    @NotBlank
    private String productCode;
    @NotBlank
    private String productName;
    @NotNull
    private LoanType loanType;
    @NotNull
    @DecimalMin("0.1")
    private BigDecimal interestRate;
    @NotNull
    @Min(1)
    private Integer minTenureMonths;
    @NotNull
    @Min(1)
    private Integer maxTenureMonths;
    @NotNull
    @DecimalMin("1000")
    private BigDecimal minAmount;
    @NotNull
    private BigDecimal maxAmount;
    private BigDecimal processingFeePercent;
    private BigDecimal lateFeeAmount;
    private Boolean active = true;

    @Data
    @Builder
    public static class Response {
        private Long id;
        private String productCode;
        private String productName;
        private LoanType loanType;
        private BigDecimal interestRate;
        private Integer minTenureMonths;
        private Integer maxTenureMonths;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private BigDecimal processingFeePercent;
        private BigDecimal lateFeeAmount;
        private Boolean active;

        public static Response from(LoanProduct p) {
            return Response.builder()
                    .id(p.getId())
                    .productCode(p.getProductCode())
                    .productName(p.getProductName())
                    .loanType(p.getLoanType())
                    .interestRate(p.getInterestRate())
                    .minTenureMonths(p.getMinTenureMonths())
                    .maxTenureMonths(p.getMaxTenureMonths())
                    .minAmount(p.getMinAmount())
                    .maxAmount(p.getMaxAmount())
                    .processingFeePercent(p.getProcessingFeePercent())
                    .lateFeeAmount(p.getLateFeeAmount())
                    .active(p.getActive())
                    .build();
        }
    }
}
