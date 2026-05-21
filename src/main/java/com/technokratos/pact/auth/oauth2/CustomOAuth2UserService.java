package com.technokratos.pact.auth.oauth2;

import com.technokratos.pact.auth.oauth2.dto.OAuth2UserInfo;
import com.technokratos.pact.security.model.UserDetailsImpl;
import com.technokratos.pact.user.model.User;
import com.technokratos.pact.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);

        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        User.AuthProvider provider = User.AuthProvider.valueOf(registrationId.toUpperCase());

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                provider, oAuth2User.getAttributes());

        if (!StringUtils.hasText(userInfo.getEmail())) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(userInfo.getEmail());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();

            if (!provider.equals(user.getAuthProvider())) {
                throw new OAuth2AuthenticationException(
                        "User is already registered with %s".formatted(user.getAuthProvider())
                );
            }
            user = updateExistingUser(user, userInfo);
        } else {
            user = registerNewUser(userRequest, userInfo);
        }

        return UserDetailsImpl.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest userRequest, OAuth2UserInfo userInfo) {
        User user = User.builder()
                .authProvider(User.AuthProvider.valueOf(
                        userRequest.getClientRegistration().getRegistrationId().toUpperCase()))
                .providerId(userInfo.getId())
                .email(userInfo.getEmail())
                .username(userInfo.getEmail().split("@")[0])
                .isVerified(true)
                .isEnabled(true)
                .role(User.Role.USER)
                .build();

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo userInfo) {
        existingUser.setUsername(userInfo.getEmail().split("@")[0]);
        return userRepository.save(existingUser);
    }
}
