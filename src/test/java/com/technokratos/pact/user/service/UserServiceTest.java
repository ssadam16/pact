package com.technokratos.pact.user.service;

import com.technokratos.pact.article.repository.ArticleLikeRepository;
import com.technokratos.pact.article.repository.ArticleRepository;
import com.technokratos.pact.article.repository.CommentRepository;
import com.technokratos.pact.file.service.AvatarService;
import com.technokratos.pact.user.dto.ProfileEditRequest;
import com.technokratos.pact.user.dto.ProfileStatsResponse;
import com.technokratos.pact.user.dto.UserProfileResponse;
import com.technokratos.pact.user.dto.UserShortProfileResponse;
import com.technokratos.pact.user.exception.UserNotFoundException;
import com.technokratos.pact.user.mapper.UserMapper;
import com.technokratos.pact.user.model.User;
import com.technokratos.pact.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AvatarService avatarService;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ArticleLikeRepository articleLikeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId).username("said").name("Said")
                .avatarFilename("a.png").hashPassword("hashed")
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();
    }

    @Test
    void getProfile_returnsProfileWithAvatar() {
        when(userRepository.findByUsername("said")).thenReturn(Optional.of(user));

        UserProfileResponse mapped = UserProfileResponse.builder().id(userId).username("said").build();

        when(userMapper.toUserProfileResponse(user)).thenReturn(mapped);
        when(avatarService.getAvatarUrl("a.png")).thenReturn("http://img/a.png");

        UserProfileResponse result = userService.getProfile("said");

        assertThat(result.getAvatarUrl()).isEqualTo("http://img/a.png");
    }

    @Test
    void getProfile_missing_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("ghost"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getShortProfile_byUsername_setsAvatar() {
        when(userRepository.findByUsername("said")).thenReturn(Optional.of(user));

        UserShortProfileResponse mapped = new UserShortProfileResponse();

        when(userMapper.toUserShortProfileResponse(user)).thenReturn(mapped);
        when(avatarService.getAvatarUrl("a.png")).thenReturn("url");

        UserShortProfileResponse result = userService.getShortProfile("said");

        assertThat(result.getAvatarUrl()).isEqualTo("url");
    }

    @Test
    void getShortProfile_byUsername_missing_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getShortProfile("ghost"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getShortProfile_byId_setsAvatar() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserShortProfileResponse mapped = new UserShortProfileResponse();

        when(userMapper.toUserShortProfileResponse(user)).thenReturn(mapped);
        when(avatarService.getAvatarUrl("a.png")).thenReturn("url");

        UserShortProfileResponse result = userService.getShortProfile(userId);

        assertThat(result.getAvatarUrl()).isEqualTo("url");
    }

    @Test
    void getShortProfile_byId_missing_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getShortProfile(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getStats_aggregatesCounts() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(articleRepository.countByAuthorId(userId)).thenReturn(3);
        when(commentRepository.countByAuthorId(userId)).thenReturn(8);
        when(articleLikeRepository.countByUserId(userId)).thenReturn(15);

        ProfileStatsResponse stats = userService.getStats(userId);

        assertThat(stats.getArticlesCount()).isEqualTo(3);
        assertThat(stats.getCommentsCount()).isEqualTo(8);
        assertThat(stats.getLikesCount()).isEqualTo(15);
        assertThat(stats.getDaysInCommunity()).isEqualTo(10);
    }

    @Test
    void getStats_missing_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getStats(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateProfile_nameOnly_saves() {
        ProfileEditRequest request = new ProfileEditRequest();
        request.setName("New Name");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.updateProfile(userId, request);

        assertThat(user.getName()).isEqualTo("New Name");
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.any());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_changePassword_encodesNew() {
        ProfileEditRequest request = new ProfileEditRequest();

        request.setName("Said");
        request.setCurrentPassword("oldPass1");
        request.setNewPassword("newPass1");
        request.setConfirmPassword("newPass1");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass1", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("newPass1")).thenReturn("newHash");

        userService.updateProfile(userId, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getHashPassword()).isEqualTo("newHash");
    }

    @Test
    void updateProfile_missingCurrentPassword_throws() {
        ProfileEditRequest request = new ProfileEditRequest();

        request.setName("Said");
        request.setNewPassword("newPass1");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("текущий пароль");
    }

    @Test
    void updateProfile_wrongCurrentPassword_throws() {
        ProfileEditRequest request = new ProfileEditRequest();

        request.setName("Said");
        request.setCurrentPassword("wrong");
        request.setNewPassword("newPass1");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Неверный");
    }

    @Test
    void updateProfile_shortNewPassword_throws() {
        ProfileEditRequest request = new ProfileEditRequest();

        request.setName("Said");
        request.setCurrentPassword("oldPass1");
        request.setNewPassword("123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass1", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не менее 6");
    }

    @Test
    void updateProfile_passwordsMismatch_throws() {
        ProfileEditRequest request = new ProfileEditRequest();

        request.setName("Said");
        request.setCurrentPassword("oldPass1");
        request.setNewPassword("newPass1");
        request.setConfirmPassword("other1");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass1", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не совпадают");
    }

    @Test
    void updateProfile_userMissing_throws() {
        ProfileEditRequest request = new ProfileEditRequest();
        request.setName("Said");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(UserNotFoundException.class);
    }
}
