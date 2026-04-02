package com.technokratos.pact.auth.oauth2.dto;

import java.util.Map;

public class YandexUserInfo extends OAuth2UserInfo {

    public YandexUserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getName() {
        return (String) attributes.get("real_name");
    }

    @Override
    public String getEmail() {
        return (String) ((Map<String, Object>) attributes.get("default_email")).get("email");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("default_avatar_id");
    }
}
