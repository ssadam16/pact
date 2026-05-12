package com.technokratos.pact.chat.dto;

import com.technokratos.pact.chat.model.ChatMedia;
import lombok.Builder;

public record MediaUploadResponse(
        String tempId,
        String filename,
        String originalName,
        String fileUrl,
        ChatMedia.MediaType mediaType) {
    @Builder
    public MediaUploadResponse {}
}
