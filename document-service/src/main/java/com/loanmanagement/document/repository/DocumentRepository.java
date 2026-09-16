package com.loanmanagement.document.repository;

import com.loanmanagement.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByLoanId(Long loanId);
    List<Document> findByCustomerId(Long customerId);
}
