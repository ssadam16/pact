package com.technokratos.pact.file.dto;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder(toBuilder = true)
public class FileInfo {
    private String originalName;
    private String storedName;
    private String path;
    private Long size;
    private String contentType;
    private String extension;
    private ZonedDateTime uploadedAt;
    private ZonedDateTime lastModified;
    private String url;
}