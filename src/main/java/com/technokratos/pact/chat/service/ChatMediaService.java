package com.technokratos.pact.chat.service;

import com.technokratos.pact.chat.dto.MediaUploadResponse;
import com.technokratos.pact.chat.dto.SendMessageRequest;
import com.technokratos.pact.chat.model.ChatMedia;
import com.technokratos.pact.chat.model.ChatMessage;
import com.technokratos.pact.chat.repository.ChatMediaRepository;
import com.technokratos.pact.file.dto.FileInfo;
import com.technokratos.pact.file.exception.FileUploadException;
import com.technokratos.pact.file.service.MinioService;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMediaService {

    private final MinioService minioService;
    private final ChatMediaRepository mediaRepository;

    private static final int MAX_MEDIA_PER_MESSAGE = 10;

    private final Map<String, List<SendMessageRequest.MediaUploadInfo>> tempMediaStorage = new ConcurrentHashMap<>();

    public List<MediaUploadResponse> uploadTempMedia(List<MultipartFile> files, UUID userId) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        if (files.size() > MAX_MEDIA_PER_MESSAGE) {
            throw new IllegalArgumentException("Cannot load more than " + MAX_MEDIA_PER_MESSAGE + " files");
        }

        List<MediaUploadResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                ChatMedia.MediaType mediaType = detectMediaType(file);

                FileInfo fileInfo = minioService.uploadFile(
                        file,
                        MinioService.Folders.CHAT_MEDIA + "/temp"
                );
                String fileUrl = minioService.getFileUrl(fileInfo.getPath());

                MediaUploadResponse response = MediaUploadResponse.builder()
                        .tempId(UUID.randomUUID().toString())
                        .filename(fileInfo.getStoredName())
                        .originalName(file.getOriginalFilename())
                        .fileUrl(fileUrl)
                        .mediaType(mediaType)
                        .build();

                responses.add(response);

            } catch (Exception e) {
                log.error("Failed to upload media: {}", e.getMessage());
                throw new FileUploadException("Failed to upload media", e);
            }
        }

        return responses;
    }

    public List<ChatMedia> uploadMedia(List<MultipartFile> files, ChatMessage message) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        if (files.size() > MAX_MEDIA_PER_MESSAGE) {
            log.warn("Too many media files");
            throw new IllegalArgumentException("Cannot load more media than %s".formatted(MAX_MEDIA_PER_MESSAGE));
        }

        List<ChatMedia> mediaList = new ArrayList<>();
        int order = 0;

        for (MultipartFile file : files) {
            try {
                ChatMedia.MediaType mediaType = detectMediaType(file);

                FileInfo fileInfo = minioService.uploadFile(
                        file,
                        "%s/%s".formatted(MinioService.Folders.CHAT_MEDIA, mediaType.name().toLowerCase())
                );
                String fileUrl = minioService.getFileUrl(fileInfo.getPath());

                ChatMedia media = ChatMedia.builder()
                        .mediaType(mediaType)
                        .message(message)
                        .filename(fileInfo.getStoredName())
                        .fileUrl(fileUrl)
                        .orderNum(order++)
                        .build();

//                if (mediaType == ChatMedia.MediaType.VIDEO && file.getContentType() != null) {
//                    media.setDuration(extractVideoDuration(file));
//                }

                mediaList.add(mediaRepository.save(media));
            } catch (Exception e) {
                log.error("Failed to upload media: {}", e.getMessage());
                throw new FileUploadException("Failed to upload media", e);
            }
        }
        return mediaList;
    }

    private ChatMedia.MediaType detectMediaType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) return ChatMedia.MediaType.DOCUMENT;
        if (contentType.startsWith("image/")) return ChatMedia.MediaType.IMAGE;
        if (contentType.startsWith("video/")) return ChatMedia.MediaType.VIDEO;
        if (contentType.startsWith("audio/")) return ChatMedia.MediaType.AUDIO;
        return ChatMedia.MediaType.DOCUMENT;
    }

    private Integer extractVideoDuration(MultipartFile file) {
        // Потом использовать библиотеку типа mp4parser или ffmpeg
        return null;
    }
}
