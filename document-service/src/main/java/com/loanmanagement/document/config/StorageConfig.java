package com.loanmanagement.document.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "document.storage.type", havingValue = "s3")
    public S3Client s3Client(
            @Value("${document.storage.s3.region}") String region,
            @Value("${document.storage.s3.endpoint:}") String endpoint,
            @Value("${document.storage.s3.path-style-access:false}") boolean pathStyleAccess,
            @Value("${document.storage.s3.bucket:}") String bucket) {
        if (bucket.isBlank()) {
            throw new IllegalStateException("DOCUMENT_S3_BUCKET must be configured when document.storage.type=s3");
        }

        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyleAccess)
                        .build());

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }
}
