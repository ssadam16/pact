package com.technokratos.pact.chat.dto;

import com.technokratos.pact.chat.model.ChatMedia;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class MediaResponse {
    private UUID id;
    private String fileUrl;
    private ChatMedia.MediaType mediaType;
    private String filename;
    private String originalName;
    private Integer orderNum;
    private Long fileSize;
    private Long duration;
    private Integer width;
    private Integer height;
}