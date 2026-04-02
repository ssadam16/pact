package com.technokratos.pact.file.dto;

import java.util.List;

public record MultiFileUploadResponse(
        List<FileUploadResponse> files,
        int total,
        int success,
        int failed
) {}
