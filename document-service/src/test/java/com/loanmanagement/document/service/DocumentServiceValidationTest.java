package com.loanmanagement.document.service;

import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.document.entity.Document;
import com.loanmanagement.document.repository.DocumentRepository;
import com.loanmanagement.document.storage.DocumentStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceValidationTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentStorage documentStorage;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void shouldAcceptValidPdfSignature() throws Exception {
        byte[] pdf = "%PDF-1.7 test".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "statement.pdf", "application/pdf", pdf);

        when(documentStorage.store(any(), any())).thenReturn("key-1");
        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Document result = documentService.upload(
                file, 10L, 20L, "STATEMENT", "customer");

        assertEquals("key-1", result.getStoragePath());
        verify(documentStorage).store(any(), eq("statement.pdf"));
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void shouldRejectMimeTypeSpoofing() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "statement.pdf", "application/pdf", "not-a-pdf".getBytes());

        DomainException ex = assertThrows(
                DomainException.class,
                () -> documentService.upload(file, 10L, 20L, "STATEMENT", "customer"));

        assertEquals("DOMAIN_ERROR", ex.getErrorCode());
        verifyNoInteractions(documentStorage, documentRepository);
    }
}
