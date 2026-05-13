package com.technokratos.pact.chat.service;

import com.technokratos.pact.chat.dto.MediaResponse;
import com.technokratos.pact.chat.dto.MediaUploadResponse;
import com.technokratos.pact.chat.dto.MessageResponse;
import com.technokratos.pact.chat.dto.SendMessageRequest;
import com.technokratos.pact.chat.model.ChatMedia;
import com.technokratos.pact.chat.model.ChatMessage;
import com.technokratos.pact.chat.repository.ChatMediaRepository;
import com.technokratos.pact.file.dto.FileInfo;
import com.technokratos.pact.file.exception.FileUploadException;
import com.technokratos.pact.file.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMediaService {

    private final MinioService minioService;
    private final ChatMediaRepository mediaRepository;

    private static final int MAX_MEDIA_PER_MESSAGE = 10;
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    @Value("${minio.bucket-name}")
    private String bucket;

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
                validateFile(file);
                ChatMedia.MediaType mediaType = detectMediaType(file);

                FileInfo fileInfo = minioService.uploadFile(
                        file,
                        MinioService.Folders.CHAT_MEDIA + "/temp/" + userId
                );
                String fileUrl = minioService.getFileUrl(fileInfo.getPath());

                MediaUploadResponse response = MediaUploadResponse.builder()
                        .tempId(UUID.randomUUID().toString())
                        .filename(fileInfo.getStoredName())
                        .originalName(file.getOriginalFilename())
                        .fileUrl(fileUrl)
                        .fileSize(file.getSize())
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

    public List<ChatMedia> moveAndSaveMedia(List<SendMessageRequest.MediaUploadInfo> mediaInfos, ChatMessage message, UUID userId) {
        if (mediaInfos == null || mediaInfos.isEmpty()) {
            return new ArrayList<>();
        }

        List<ChatMedia> mediaList = new ArrayList<>();

        for (SendMessageRequest.MediaUploadInfo info : mediaInfos) {
            try {
                String tempPath = MinioService.Folders.CHAT_MEDIA + "/temp/" + userId + "/" + info.getFilename();
                String permanentPath = MinioService.Folders.CHAT_MEDIA + "/" + info.getMediaType().toLowerCase() + "/" + UUID.randomUUID() + "_" + info.getFilename();

                // Копируем файл из временной папки в постоянную
                minioService.copyObject(tempPath, permanentPath);

                // Получаем URL для постоянного файла
                String fileUrl = minioService.getFileUrl(permanentPath);

                ChatMedia media = ChatMedia.builder()
                        .mediaType(ChatMedia.MediaType.valueOf(info.getMediaType()))
                        .message(message)
                        .filename(info.getFilename())
                        .fileUrl(fileUrl)
                        .originalName(info.getOriginalName())
                        .fileSize(info.getFileSize())
                        .orderNum(info.getOrderNum())
                        .duration(info.getDuration())
                        .width(info.getWidth())
                        .height(info.getHeight())
                        .build();

                mediaList.add(mediaRepository.save(media));

                // Удаляем временный файл после успешного копирования
                minioService.deleteFile(tempPath);

            } catch (Exception e) {
                log.error("Failed to move media: {}", e.getMessage(), e);
            }
        }

        return mediaList;
    }

    public void cleanupTempMedia(UUID userId) {
        String tempFolder = MinioService.Folders.CHAT_MEDIA + "/temp/" + userId;
        try {
            List<FileInfo> files = minioService.listFiles(tempFolder);
            for (FileInfo file : files) {
                minioService.deleteFile(file.getPath());
            }
        } catch (Exception e) {
            log.error("Failed to cleanup temp media for user {}: {}", userId, e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileUploadException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileUploadException("File too large. Maximum size: 50MB");
        }
    }

    private ChatMedia.MediaType detectMediaType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            String filename = file.getOriginalFilename();
            if (filename != null) {
                String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
                return detectMediaTypeByExtension(ext);
            }
            return ChatMedia.MediaType.DOCUMENT;
        }
        if (contentType.startsWith("image/")) return ChatMedia.MediaType.IMAGE;
        if (contentType.startsWith("video/")) return ChatMedia.MediaType.VIDEO;
        if (contentType.startsWith("audio/")) {
            return ChatMedia.MediaType.AUDIO;
        }
        return detectMediaTypeByExtension(getExtension(file.getOriginalFilename()));
    }

    private ChatMedia.MediaType detectMediaTypeByExtension(String extension) {
        if (extension == null) return ChatMedia.MediaType.DOCUMENT;
        extension = extension.toLowerCase();
        if (extension.matches("jpg|jpeg|png|gif|webp|bmp|svg")) return ChatMedia.MediaType.IMAGE;
        if (extension.matches("mp4|webm|avi|mov|mkv")) return ChatMedia.MediaType.VIDEO;
        if (extension.matches("mp3|wav|ogg|m4a|flac")) return ChatMedia.MediaType.AUDIO;
        if (extension.matches("doc|docx|xls|xlsx|ppt|pptx|pdf|txt|java|py|js|html|css|json|xml|md"))
            return ChatMedia.MediaType.DOCUMENT;
        return ChatMedia.MediaType.OTHER;
    }

    private String getExtension(String filename) {
        if (filename == null) return null;
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) return null;
        return filename.substring(lastDot + 1);
    }

    public void addMediaToResponse(ChatMessage message, MessageResponse response) {
        List<ChatMedia> mediaList = mediaRepository.findByMessageIdOrderByOrderNumAsc(message.getId());
        response.setMediaList(mediaList.stream().map(media -> MediaResponse.builder()
                .id(media.getId())
                .fileUrl(media.getFileUrl())
                .mediaType(media.getMediaType())
                .filename(media.getFilename())
                .originalName(media.getOriginalName())
                .orderNum(media.getOrderNum())
                .fileSize(media.getFileSize())
                .duration(media.getDuration())
                .width(media.getWidth())
                .height(media.getHeight())
                .build()).toList());
    }
}