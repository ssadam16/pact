package com.technokratos.pact.file.dto;

public record FileUploadResponse(
        String originalName,
        String storedName,
        String path,
        Long size,
        String contentType,
        String url
) {}
