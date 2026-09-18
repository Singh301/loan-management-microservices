package com.loanmanagement.document.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalDocumentStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldStoreAndLoadDocument() throws Exception {
        LocalDocumentStorage storage = new LocalDocumentStorage(tempDir.toString());
        byte[] content = "document-content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "statement.pdf", "application/pdf", content);

        String key = storage.store(file, "statement.pdf");

        assertTrue(Files.exists(tempDir.resolve(key)));
        try (var inputStream = storage.load(key).getInputStream()) {
            assertArrayEquals(content, inputStream.readAllBytes());
        }

        storage.delete(key);
        assertTrue(Files.notExists(tempDir.resolve(key)));
    }
}
