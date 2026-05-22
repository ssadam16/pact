package com.technokratos.pact.file.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.public-endpoint}")
    private String publicEndpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.region}")
    private String region;

    @Value("${minio.secure}")
    private boolean secure;

    @Value("${minio.bucket-name}")
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region(region)
                .build();
    }

    @Bean
    public MinioClient minioPublicClient() {
        return MinioClient.builder()
                .endpoint(publicEndpoint)
                .credentials(accessKey, secretKey)
                .region(region)
                .build();
    }

    @Bean
    public MinioProperties minioProperties() {
        return new MinioProperties(endpoint, publicEndpoint, accessKey, secretKey,
                region, secure, bucket);
    }

    public record MinioProperties(
            String endpoint,
            String publicEndpoint,
            String accessKey,
            String secretKey,
            String region,
            boolean secure,
            String bucket
    ) {}
}