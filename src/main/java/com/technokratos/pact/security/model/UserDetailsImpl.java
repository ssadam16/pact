package com.technokratos.pact.security.model;

import com.technokratos.pact.user.model.User;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

@Data
@Builder(toBuilder = true)
public class UserDetailsImpl implements UserDetails, OAuth2User {

    private UUID id;
    private String username;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;


    @NotNull
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @NotNull
    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public @NonNull String getName() {
        return String.valueOf(id);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static UserDetailsImpl create(User user) {
        return UserDetailsImpl.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getHashPassword())
                .authorities(List.of(new SimpleGrantedAuthority(user.getRole().name())))
                .build();
    }

    public static UserDetailsImpl create(User user, Map<String, Object> attributes) {
        UserDetailsImpl userDetails = UserDetailsImpl.create(user);
        userDetails.setAttributes(attributes);
        return userDetails;
    }
}
