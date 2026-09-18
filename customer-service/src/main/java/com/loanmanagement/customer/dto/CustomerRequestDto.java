package com.loanmanagement.customer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CustomerRequestDto {
    @Positive
    private Long userId;

    @NotBlank
    @Size(max = 200)
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    private LocalDate dateOfBirth;
    private String address;
    private String city;
    private String state;
    private String pincode;

    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Invalid PAN format")
    private String panNumber;

    private String aadhaarNumber;
}
