package com.technokratos.pact.user.service;

import com.technokratos.pact.file.service.AvatarService;
import com.technokratos.pact.file.service.MinioService;
import com.technokratos.pact.user.dto.UserShortProfileResponse;
import com.technokratos.pact.user.exception.UserNotFoundException;
import com.technokratos.pact.user.dto.UserProfileResponse;
import com.technokratos.pact.user.mapper.UserMapper;
import com.technokratos.pact.user.model.User;
import com.technokratos.pact.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AvatarService avatarService;

    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> UserNotFoundException.byUsername(username));

        UserProfileResponse profile = userMapper.toUserProfileResponse(user);
        profile.setAvatarUrl(avatarService.getAvatarUrl(user.getAvatarFilename()));

        log.info("Returning user profile (ID={}, username={})", profile.getId(), profile.getUsername());

        return profile;
    }

    public UserShortProfileResponse getShortProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> UserNotFoundException.byUsername(username));

        UserShortProfileResponse profile = userMapper.toUserShortProfileResponse(user);
        profile.setAvatarUrl(avatarService.getAvatarUrl(user.getAvatarFilename()));

        log.info("Returning short user profile (ID={}, username={})", profile.getId(), profile.getUsername());

        return profile;
    }

    public UserShortProfileResponse getShortProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId));

        UserShortProfileResponse profile = userMapper.toUserShortProfileResponse(user);
        profile.setAvatarUrl(avatarService.getAvatarUrl(user.getAvatarFilename()));

        log.info("Returning short user profile (ID={}, username={})", profile.getId(), profile.getUsername());

        return profile;
    }

}
