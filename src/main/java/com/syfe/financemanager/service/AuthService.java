package com.syfe.financemanager.service;

import com.syfe.financemanager.dto.request.RegisterRequest;
import com.syfe.financemanager.dto.response.RegisterResponse;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.exception.ConflictException;
import com.syfe.financemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles user registration. Login/logout are session concerns owned by the controller. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("A user with this username already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .build();

        User saved = userRepository.save(user);
        return new RegisterResponse("User registered successfully", saved.getId());
    }
}
