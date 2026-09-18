package com.loanmanagement.customer.dto;

import com.loanmanagement.customer.entity.Customer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerResponseDtoTest {

    @Test
    void shouldMaskPanAndAadhaarInApiResponse() {
        Customer customer = Customer.builder()
                .id(1L)
                .userId(10L)
                .fullName("Test Customer")
                .email("test@example.com")
                .phone("9999999999")
                .panNumber("ABCDE1234F")
                .aadhaarNumber("1234 5678 9012")
                .build();

        CustomerResponseDto response = CustomerResponseDto.from(customer);

        assertEquals("ABCDE****F", response.getPanNumber());
        assertEquals("XXXX XXXX 9012", response.getAadhaarNumber());
    }
}
