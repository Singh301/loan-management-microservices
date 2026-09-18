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
        if (file == null) {
            throw new IOException("Uploaded file must not be null");
        }
        Files.createDirectories(baseDirectory);

        String storedName = UUID.randomUUID() + extension(safeOriginalName);
        Path target = baseDirectory.resolve(storedName).normalize();
        Path parent = target.getParent();
        if (!baseDirectory.equals(parent)) {
            throw new IOException("Invalid local storage path");
        }

        var inputStream = file.getInputStream();
        if (inputStream == null) {
            throw new IOException("Unable to read uploaded file");
        }
        try (inputStream) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return storedName;
    }

    private String extension(String safeOriginalName) {
        int dot = safeOriginalName.lastIndexOf('.');
        if (dot < 0 || dot == safeOriginalName.length() - 1) {
            return "";
        }
        String extension = safeOriginalName.substring(dot).toLowerCase(java.util.Locale.ROOT);
        return extension.length() <= 10 && extension.matches("\\.[a-z0-9]+")
                ? extension
                : "";
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
