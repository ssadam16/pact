package com.technokratos.pact.auth.service;

import com.technokratos.pact.auth.dto.RegisterRequest;
import com.technokratos.pact.user.model.User;
import com.technokratos.pact.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegisterRequest request) {
        userRepository.save(User.builder()
                .username(request.username())
                .name(request.name())
                .email(request.email())
                .hashPassword(passwordEncoder.encode(request.password()))
                .role(User.Role.USER)
                .authProvider(User.AuthProvider.LOCAL)
                .isEnabled(true)
                .build()
        );
        log.info("User (username={}) has registered", request.username());
    }
}
