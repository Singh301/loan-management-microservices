package com.loanmanagement.loan.service;

import com.loanmanagement.common.exception.ResourceNotFoundException;
import com.loanmanagement.loan.dto.CollateralDto;
import com.loanmanagement.loan.entity.Collateral;
import com.loanmanagement.loan.repository.CollateralRepository;
import com.loanmanagement.loan.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollateralService {

    private final CollateralRepository collateralRepository;
    private final LoanRepository loanRepository;

    @Transactional
    public CollateralDto.Response add(CollateralDto dto) {
        if (!loanRepository.existsById(dto.getLoanId())) {
            throw new ResourceNotFoundException("Loan", dto.getLoanId());
        }
        Collateral c = Collateral.builder()
                .loanId(dto.getLoanId())
                .collateralType(dto.getCollateralType())
                .description(dto.getDescription())
                .estimatedValue(dto.getEstimatedValue())
                .ownershipProof(dto.getOwnershipProof())
                .build();
        return CollateralDto.Response.from(collateralRepository.save(c));
    }

    @Transactional(readOnly = true)
    public List<CollateralDto.Response> byLoan(Long loanId) {
        return collateralRepository.findByLoanId(loanId).stream()
                .map(CollateralDto.Response::from).toList();
    }

    @Transactional
    public void delete(Long id) {
        if (!collateralRepository.existsById(id)) {
            throw new ResourceNotFoundException("Collateral", id);
        }
        collateralRepository.deleteById(id);
    }
}
