package com.loanmanagement.loan.service;

import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.common.exception.ResourceNotFoundException;
import com.loanmanagement.loan.dto.LoanProductDto;
import com.loanmanagement.loan.entity.LoanProduct;
import com.loanmanagement.loan.entity.LoanType;
import com.loanmanagement.loan.repository.LoanProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanProductService {
    private final LoanProductRepository repository;

    @Transactional
    @CacheEvict(cacheNames = {"loanProducts", "loanProductsByType"}, allEntries = true)
    public LoanProductDto.Response create(LoanProductDto dto) {
        if (repository.existsByProductCode(dto.getProductCode())) {
            throw new DomainException(
                    "Product code already exists",
                    HttpStatus.CONFLICT);
        }

        LoanProduct p = LoanProduct.builder()
                .productCode(dto.getProductCode())
                .productName(dto.getProductName())
                .loanType(dto.getLoanType())
                .interestRate(dto.getInterestRate())
                .minTenureMonths(dto.getMinTenureMonths())
                .maxTenureMonths(dto.getMaxTenureMonths())
                .minAmount(dto.getMinAmount())
                .maxAmount(dto.getMaxAmount())
                .processingFeePercent(dto.getProcessingFeePercent())
                .lateFeeAmount(dto.getLateFeeAmount())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
        return LoanProductDto.Response.from(repository.save(p));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "loanProducts", key = "'active'")
    public List<LoanProductDto.Response> listActive() {
        return repository.findByActiveTrue()
                .stream()
                .map(LoanProductDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "loanProductsByType", key = "#type.name()")
    public List<LoanProductDto.Response> byType(LoanType type) {
        return repository.findByLoanTypeAndActiveTrue(type)
                .stream()
                .map(LoanProductDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "loanProducts", key = "#id")
    public LoanProductDto.Response get(Long id) {
        return LoanProductDto.Response.from(
                repository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("LoanProduct", id)));
    }

    @Transactional
    @CacheEvict(cacheNames = {"loanProducts", "loanProductsByType"}, allEntries = true)
    public LoanProductDto.Response update(Long id, LoanProductDto dto) {
        LoanProduct p = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LoanProduct", id));

        p.setProductName(dto.getProductName());
        p.setInterestRate(dto.getInterestRate());
        p.setMinTenureMonths(dto.getMinTenureMonths());
        p.setMaxTenureMonths(dto.getMaxTenureMonths());
        p.setMinAmount(dto.getMinAmount());
        p.setMaxAmount(dto.getMaxAmount());
        p.setProcessingFeePercent(dto.getProcessingFeePercent());
        p.setLateFeeAmount(dto.getLateFeeAmount());
        if (dto.getActive() != null) {
            p.setActive(dto.getActive());
        }
        return LoanProductDto.Response.from(repository.save(p));
    }

    @Transactional
    @CacheEvict(cacheNames = {"loanProducts", "loanProductsByType"}, allEntries = true)
    public void deactivate(Long id) {
        LoanProduct p = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LoanProduct", id));
        p.setActive(false);
        repository.save(p);
    }
}
