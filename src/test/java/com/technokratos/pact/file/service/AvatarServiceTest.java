package com.technokratos.pact.file.service;

import com.technokratos.pact.file.dto.FileInfo;
import com.technokratos.pact.user.exception.UserNotFoundException;
import com.technokratos.pact.user.model.User;
import com.technokratos.pact.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MinioService minioService;

    @InjectMocks
    private AvatarService avatarService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).username("said").build();
    }

    @Test
    void uploadAvatar_deletesOld_andSavesNew() {
        user.setAvatarFilename("old.png");
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(minioService.uploadFile(eq(file), eq(MinioService.Folders.AVATARS)))
                .thenReturn(FileInfo.builder().storedName("new.png").build());

        avatarService.uploadAvatar(userId, file);

        verify(minioService).deleteFile(MinioService.Folders.AVATARS + "/old.png");
        assertThat(user.getAvatarFilename()).isEqualTo("new.png");
        verify(userRepository).save(user);
    }

    @Test
    void uploadAvatar_noOldAvatar_skipsDelete() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(minioService.uploadFile(any(), any()))
                .thenReturn(FileInfo.builder().storedName("new.png").build());

        avatarService.uploadAvatar(userId, file);

        verify(minioService, never()).deleteFile(any());
    }

    @Test
    void uploadAvatar_userMissing_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> avatarService.uploadAvatar(userId, null))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void deleteAvatar_removesFile_andClearsField() {
        user.setAvatarFilename("pic.png");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        avatarService.deleteAvatar(userId);

        verify(minioService).deleteFile(MinioService.Folders.AVATARS + "/pic.png");
        assertThat(user.getAvatarFilename()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void deleteAvatar_noAvatar_doesNothing() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        avatarService.deleteAvatar(userId);

        verify(minioService, never()).deleteFile(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUserAvatar_clearsFilename() {
        user.setAvatarFilename("pic.png");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        avatarService.deleteUserAvatar(userId);

        verify(minioService).deleteFile(MinioService.Folders.AVATARS + "/pic.png");
        assertThat(user.getAvatarFilename()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void deleteUserAvatar_swallowsMinioError() {
        user.setAvatarFilename("pic.png");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        org.mockito.Mockito.doThrow(new RuntimeException("minio down"))
                .when(minioService).deleteFile(any());

        avatarService.deleteUserAvatar(userId);

        assertThat(user.getAvatarFilename()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void getAvatarUrl_delegatesToMinio() {
        when(minioService.getFileUrl("a.png", MinioService.Folders.AVATARS)).thenReturn("http://img/a.png");

        assertThat(avatarService.getAvatarUrl("a.png")).isEqualTo("http://img/a.png");
    }
}
