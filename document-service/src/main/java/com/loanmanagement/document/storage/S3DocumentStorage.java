package com.loanmanagement.document.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;

@Component
@ConditionalOnProperty(name = "document.storage.type", havingValue = "s3")
public class S3DocumentStorage implements DocumentStorage {

    private final S3Client s3Client;
    private final String bucket;
    private final String keyPrefix;

    public S3DocumentStorage(
            S3Client s3Client,
            @Value("${document.storage.s3.bucket}") String bucket,
            @Value("${document.storage.s3.key-prefix:documents/}") String keyPrefix) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.keyPrefix = keyPrefix.endsWith("/") ? keyPrefix : keyPrefix + "/";
    }

    @Override
    public String store(MultipartFile file, String safeOriginalName) throws IOException {
        String key = keyPrefix + java.util.UUID.randomUUID() + extension(safeOriginalName);

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
            return key;
        } catch (RuntimeException ex) {
            throw new IOException("Failed to store document in S3", ex);
        }
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
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build();
            return new InputStreamResource(s3Client.getObject(request));
        } catch (RuntimeException ex) {
            throw new IOException("Failed to read document from S3", ex);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build());
        } catch (RuntimeException ex) {
            throw new IOException("Failed to delete document from S3", ex);
        }
    }
}
