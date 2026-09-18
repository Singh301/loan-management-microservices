package com.loanmanagement.document.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "document.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalDocumentStorage implements DocumentStorage {

    private final Path baseDirectory;

    public LocalDocumentStorage(
            @Value("${document.storage.local.upload-dir:${file.upload-dir:./uploads}}") String uploadDir) {
        this.baseDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file, String safeOriginalName) throws IOException {
        Files.createDirectories(baseDirectory);

        String storedName = UUID.randomUUID() + "_" + safeOriginalName;
        Path target = baseDirectory.resolve(storedName).normalize();
        if (!target.getParent().equals(baseDirectory)) {
            throw new IOException("Invalid local storage path");
        }

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return storedName;
    }

    @Override
    public Resource load(String storageKey) throws IOException {
        Path path = baseDirectory.resolve(storageKey).normalize();
        if (!path.startsWith(baseDirectory)) {
            throw new IOException("Invalid local storage path");
        }

        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IOException("Stored document is not readable");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new IOException("Invalid local storage path", ex);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Path path = baseDirectory.resolve(storageKey).normalize();
        if (!path.startsWith(baseDirectory)) {
            throw new IOException("Invalid local storage path");
        }
        Files.deleteIfExists(path);
    }
}
