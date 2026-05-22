package com.technokratos.pact.chat.service;

import com.technokratos.pact.chat.dto.MediaUploadResponse;
import com.technokratos.pact.chat.dto.MessageResponse;
import com.technokratos.pact.chat.dto.SendMessageRequest;
import com.technokratos.pact.chat.model.ChatMedia;
import com.technokratos.pact.chat.model.ChatMessage;
import com.technokratos.pact.chat.repository.ChatMediaRepository;
import com.technokratos.pact.file.dto.FileInfo;
import com.technokratos.pact.file.exception.FileUploadException;
import com.technokratos.pact.file.service.MinioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMediaServiceTest {

    @Mock
    private MinioService minioService;

    @Mock
    private ChatMediaRepository mediaRepository;

    @InjectMocks
    private ChatMediaService chatMediaService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    private MultipartFile imageFile() {
        MultipartFile file = Mockito.mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1000L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("pic.png");

        return file;
    }

    @Test
    void uploadTempMedia_null_returnsEmpty() {
        assertThat(chatMediaService.uploadTempMedia(null, userId)).isEmpty();
    }

    @Test
    void uploadTempMedia_tooMany_throws() {
        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            files.add(Mockito.mock(MultipartFile.class));
        }

        assertThatThrownBy(() -> chatMediaService.uploadTempMedia(files, userId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadTempMedia_image_detectsImageType() {
        MultipartFile file = imageFile();

        when(minioService.uploadChatFile(any(), anyString()))
                .thenReturn(FileInfo.builder().storedName("pic.png").path("chat-media/temp/x/pic.png").build());
        when(minioService.getFileUrl(anyString())).thenReturn("http://img/pic.png");

        List<MediaUploadResponse> result = chatMediaService.uploadTempMedia(List.of(file), userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).mediaType()).isEqualTo(ChatMedia.MediaType.IMAGE);
    }

    @Test
    void uploadTempMedia_voiceKind_setsVoiceMessageType() {
        MultipartFile file = Mockito.mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(500L);
        when(file.getOriginalFilename()).thenReturn("voice.ogg");
        when(minioService.uploadChatFile(any(), anyString()))
                .thenReturn(FileInfo.builder().storedName("voice.ogg").path("p").build());
        when(minioService.getFileUrl(anyString())).thenReturn("url");

        List<MediaUploadResponse> result = chatMediaService.uploadTempMedia(List.of(file), userId, "voice");

        assertThat(result.get(0).mediaType()).isEqualTo(ChatMedia.MediaType.VOICE_MESSAGE);
    }

    @Test
    void uploadTempMedia_emptyFile_throwsUploadException() {
        MultipartFile file = Mockito.mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> chatMediaService.uploadTempMedia(List.of(file), userId))
                .isInstanceOf(FileUploadException.class);
    }

    @Test
    void moveAndSaveMedia_copiesAndPersists() {
        SendMessageRequest.MediaUploadInfo info = new SendMessageRequest.MediaUploadInfo();

        info.setFilename("pic.png");
        info.setMediaType("IMAGE");
        info.setOriginalName("pic.png");
        info.setOrderNum(0);

        when(minioService.getFileUrl(anyString())).thenReturn("http://img/pic.png");
        when(mediaRepository.save(any(ChatMedia.class))).thenAnswer(i -> i.getArgument(0));

        ChatMessage message = ChatMessage.builder().id(UUID.randomUUID()).build();
        List<ChatMedia> result = chatMediaService.moveAndSaveMedia(List.of(info), message, userId);

        assertThat(result).hasSize(1);

        verify(minioService).copyObject(anyString(), anyString());
        verify(minioService).deleteFile(anyString());
    }

    @Test
    void moveAndSaveMedia_empty_returnsEmpty() {
        ChatMessage message = ChatMessage.builder().id(UUID.randomUUID()).build();

        assertThat(chatMediaService.moveAndSaveMedia(null, message, userId)).isEmpty();
    }

    @Test
    void cleanupTempMedia_deletesEachTempFile() {
        when(minioService.listFiles(anyString())).thenReturn(List.of(
                FileInfo.builder().path("chat-media/temp/x/a.png").build(),
                FileInfo.builder().path("chat-media/temp/x/b.png").build()
        ));

        chatMediaService.cleanupTempMedia(userId);

        verify(minioService).deleteFile("chat-media/temp/x/a.png");
        verify(minioService).deleteFile("chat-media/temp/x/b.png");
    }

    @Test
    void addMediaToResponse_mapsMediaList() {
        ChatMessage message = ChatMessage.builder().id(UUID.randomUUID()).build();
        ChatMedia media = ChatMedia.builder()
                .id(UUID.randomUUID()).fileUrl("url").mediaType(ChatMedia.MediaType.IMAGE)
                .filename("a.png").orderNum(0).build();

        when(mediaRepository.findByMessageIdOrderByOrderNumAsc(message.getId())).thenReturn(List.of(media));

        MessageResponse response = new MessageResponse();
        chatMediaService.addMediaToResponse(message, response);

        assertThat(response.getMediaList()).hasSize(1);
        assertThat(response.getMediaList().get(0).getFileUrl()).isEqualTo("url");
    }
}
