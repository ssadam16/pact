package com.technokratos.pact.file.service;

import com.technokratos.pact.file.dto.FileInfo;
import com.technokratos.pact.user.model.User;
import com.technokratos.pact.user.exception.UserNotFoundException;
import com.technokratos.pact.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvatarService {

    private final UserRepository userRepository;
    private final MinioService minioService;

    @Transactional
    public void uploadAvatar(UUID userId, MultipartFile avatarFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId));

        deleteOldAvatar(user);

        FileInfo fileInfo = minioService.uploadFile(avatarFile, MinioService.Folders.AVATARS);
        user.setAvatarFilename(fileInfo.getStoredName());
        userRepository.save(user);

        log.info("Avatar uploaded for user {}: {}", userId, fileInfo.getStoredName());
    }

    @Transactional
    @Deprecated
    public void deleteUserAvatar(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId));

        deleteOldAvatar(user);

        user.setAvatarFilename(null);
        userRepository.save(user);

        log.info("Avatar deleted for user {}", userId);
    }

    private void deleteOldAvatar(User user) {
        if (user.getAvatarFilename() != null) {
            try {
                String filePath = MinioService.Folders.AVATARS + "/" + user.getAvatarFilename();
                minioService.deleteFile(filePath);
                log.debug("Old image deleted: {}", user.getAvatarFilename());
            } catch (Exception e) {
                log.warn("Failed to delete old image for user {}: {}",
                        user.getId(), e.getMessage());
            }
        }
    }

    public void deleteAvatar(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId));

        if (user.getAvatarFilename() != null) {
            String oldAvatarPath = MinioService.Folders.AVATARS + "/" + user.getAvatarFilename();
            minioService.deleteFile(oldAvatarPath);

            user.setAvatarFilename(null);
            userRepository.save(user);

            log.info("Avatar deleted for user {}", userId);
        }
    }

    public String getAvatarUrl(String filename) {
        return minioService.getFileUrl(filename, MinioService.Folders.AVATARS);
    }
}
