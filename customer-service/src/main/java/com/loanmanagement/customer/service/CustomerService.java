package com.loanmanagement.customer.service;

import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.common.exception.ResourceNotFoundException;
import com.loanmanagement.customer.dto.CustomerRequestDto;
import com.loanmanagement.customer.dto.CustomerResponseDto;
import com.loanmanagement.customer.entity.Customer;
import com.loanmanagement.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponseDto create(CustomerRequestDto request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DomainException("Customer with email already exists", HttpStatus.CONFLICT);
        }
        if (request.getPanNumber() != null && customerRepository.existsByPanNumber(request.getPanNumber())) {
            throw new DomainException("PAN already registered", HttpStatus.CONFLICT);
        }

        Customer customer = Customer.builder()
                .userId(request.getUserId())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .panNumber(request.getPanNumber())
                .aadhaarNumber(request.getAadhaarNumber())
                .build();

        return CustomerResponseDto.from(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public CustomerResponseDto getById(Long id) {
        return CustomerResponseDto.from(find(id));
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponseDto> getAll(Pageable pageable) {
        return customerRepository.findAll(pageable).map(CustomerResponseDto::from);
    }

    @Transactional
    public CustomerResponseDto update(Long id, CustomerRequestDto request) {
        Customer customer = find(id);
        customer.setFullName(request.getFullName());
        customer.setPhone(request.getPhone());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setState(request.getState());
        customer.setPincode(request.getPincode());
        if (request.getPanNumber() != null) customer.setPanNumber(request.getPanNumber());
        if (request.getAadhaarNumber() != null) customer.setAadhaarNumber(request.getAadhaarNumber());
        return CustomerResponseDto.from(customerRepository.save(customer));
    }

    @Transactional
    public void softDelete(Long id) {
        Customer customer = find(id);
        customer.setDeleted(true);
        customer.setDeletedAt(java.time.LocalDateTime.now());
        customerRepository.save(customer);
    }

    private Customer find(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }
}
