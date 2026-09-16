package com.loanmanagement.loan.dto;

import com.loanmanagement.loan.entity.Collateral;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CollateralDto {
    @NotNull
    private Long loanId;
    @NotBlank
    private String collateralType;
    private String description;
    @NotNull
    @DecimalMin("1.00")
    private BigDecimal estimatedValue;
    private String ownershipProof;

    @Data
    @Builder
    public static class Response {
        private Long id;
        private Long loanId;
        private String collateralType;
        private String description;
        private BigDecimal estimatedValue;
        private String ownershipProof;
        private LocalDateTime createdAt;

        public static Response from(Collateral c) {
            return Response.builder()
                    .id(c.getId())
                    .loanId(c.getLoanId())
                    .collateralType(c.getCollateralType())
                    .description(c.getDescription())
                    .estimatedValue(c.getEstimatedValue())
                    .ownershipProof(c.getOwnershipProof())
                    .createdAt(c.getCreatedAt())
                    .build();
        }
    }
}
