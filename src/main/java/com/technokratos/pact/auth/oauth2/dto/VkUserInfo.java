package com.technokratos.pact.auth.oauth2.dto;

import java.util.List;
import java.util.Map;

public class VkUserInfo extends OAuth2UserInfo {

    public VkUserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        List<Map<String, Object>> response = (List<Map<String, Object>>) attributes.get("response");
        if (response != null && !response.isEmpty()) {
            return String.valueOf(response.get(0).get("id"));
        }
        return null;
    }

    @Override
    public String getName() {
        List<Map<String, Object>> response = (List<Map<String, Object>>) attributes.get("response");
        if (response != null && !response.isEmpty()) {
            Map<String, Object> userData = response.get(0);
            String firstName = (String) userData.get("first_name");
            String lastName = (String) userData.get("last_name");
            return firstName + " " + lastName;
        }
        return null;
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        List<Map<String, Object>> response = (List<Map<String, Object>>) attributes.get("response");
        if (response != null && !response.isEmpty()) {
            return (String) response.getFirst().get("photo_200");
        }
        return null;
    }
}
