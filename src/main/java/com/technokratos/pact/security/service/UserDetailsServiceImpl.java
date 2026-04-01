package com.technokratos.pact.security.service;

import com.technokratos.pact.security.exception.DisabledAccountException;
import com.technokratos.pact.security.model.UserDetailsImpl;
import com.technokratos.pact.user.model.User;
import com.technokratos.pact.user.exception.UserNotFoundException;
import com.technokratos.pact.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @NotNull
    @Override
    @Transactional
    public UserDetails loadUserByUsername(@NotNull String username) throws UsernameNotFoundException, DisabledAccountException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> UserNotFoundException.byUsername(username));

        if (!user.isEnabled()) {
            throw DisabledAccountException.byUsername(username);
        }

        return UserDetailsImpl.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getHashPassword())
                .authorities(Arrays.stream(User.Role.values())
                        .map(r -> new SimpleGrantedAuthority(r.toString()))
                        .toList()
                )
                .build();
    }
}
