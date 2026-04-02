package com.technokratos.pact.auth.oauth2;

import com.technokratos.pact.auth.oauth2.dto.*;
import com.technokratos.pact.user.model.User;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(User.AuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> new GoogleUserInfo(attributes);
            case GITHUB -> new GithubUserInfo(attributes);
            case VK -> new VkUserInfo(attributes);
            case YANDEX -> new YandexUserInfo(attributes);
            default ->
                    throw new OAuth2AuthenticationException("Login with %s is not supported yet".formatted(provider));
        };
    }
}
