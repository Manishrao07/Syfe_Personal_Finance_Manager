package com.syfe.financemanager.service;

import com.syfe.financemanager.dto.request.RegisterRequest;
import com.syfe.financemanager.dto.response.RegisterResponse;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.exception.ConflictException;
import com.syfe.financemanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest();
        request.setUsername("user@example.com");
        request.setPassword("password123");
        request.setFullName("John Doe");
        request.setPhoneNumber("+1234567890");
    }

    @Test
    void register_savesNewUser_whenUsernameIsFree() {
        when(userRepository.existsByUsername("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        RegisterResponse response = authService.register(request);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getMessage()).isEqualTo("User registered successfully");
        verify(userRepository).save(argThat(u -> u.getPassword().equals("hashed")));
    }

    @Test
    void register_throwsConflict_whenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
    }
}
