package com.loanmanagement.customer.dto;

import com.loanmanagement.customer.entity.Customer;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CustomerResponseDto {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String panNumber;
    private String aadhaarNumber;
    private Customer.KycStatus kycStatus;
    private LocalDateTime createdAt;

    private static String maskPan(String pan) {
        if (pan == null || pan.isBlank()) {
            return pan;
        }
        String normalized = pan.trim();
        if (normalized.length() != 10) {
            return "**********";
        }
        return normalized.substring(0, 5) + "****" + normalized.charAt(9);
    }

    private static String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.isBlank()) {
            return aadhaar;
        }
        String normalized = aadhaar.replaceAll("\\s+", "");
        if (normalized.length() <= 4) {
            return "********";
        }
        return "XXXX XXXX " + normalized.substring(normalized.length() - 4);
    }

    public static CustomerResponseDto from(Customer c) {
        return CustomerResponseDto.builder()
                .id(c.getId())
                .userId(c.getUserId())
                .fullName(c.getFullName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .dateOfBirth(c.getDateOfBirth())
                .address(c.getAddress())
                .city(c.getCity())
                .state(c.getState())
                .pincode(c.getPincode())
                .panNumber(maskPan(c.getPanNumber()))
                .aadhaarNumber(maskAadhaar(c.getAadhaarNumber()))
                .kycStatus(c.getKycStatus())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
