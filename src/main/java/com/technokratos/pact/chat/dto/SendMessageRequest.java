package com.technokratos.pact.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class SendMessageRequest {

    @NotNull(message = "ID чата обязателен")
    private UUID chatId;

    @Size(max = 4096, message = "Сообщение не может превышать 4096 символов")
    private String content;

    private UUID replyToMessageId;

    private List<MediaUploadInfo> mediaList;

    @Data
    public static class MediaUploadInfo {
        private String filename;
        private String originalName;
        private String mediaType;
        private Integer orderNum;
        private Long duration;
        private Integer width;
        private Integer height;
    }
}