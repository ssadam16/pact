package com.technokratos.pact.auth.service;

import com.technokratos.pact.auth.dto.RegisterRequest;
import com.technokratos.pact.user.model.User;
import com.technokratos.pact.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_savesUser_withEncodedPasswordAndDefaults() {
        RegisterRequest request = new RegisterRequest("said", "Said", "said@mail.com", "Passw0rd");
        when(passwordEncoder.encode("Passw0rd")).thenReturn("encoded");

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getUsername()).isEqualTo("said");
        assertThat(saved.getEmail()).isEqualTo("said@mail.com");
        assertThat(saved.getHashPassword()).isEqualTo("encoded");
        assertThat(saved.getRole()).isEqualTo(User.Role.USER);
        assertThat(saved.getAuthProvider()).isEqualTo(User.AuthProvider.LOCAL);
        assertThat(saved.isEnabled()).isTrue();
    }
}
