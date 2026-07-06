package com.foodify.foodiesapi.service;

import com.foodify.foodiesapi.entity.UserEntity;
import com.foodify.foodiesapi.enums.UserRole;
import com.foodify.foodiesapi.io.UserRequest;
import com.foodify.foodiesapi.io.UserResponse;
import com.foodify.foodiesapi.repository.UserRepository;
import com.foodify.foodiesapi.enums.UserRole;
import lombok.AllArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public UserResponse registerUser(UserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }
        UserEntity newUser = convertToEntity(request);
        newUser = userRepository.save(newUser);
        return convertToResponse(newUser);
    }

    @Override
    public String findByUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    private UserEntity convertToEntity(UserRequest request) {
    return UserEntity.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .role(request.getRole())
            .build();
}

    private UserResponse convertToResponse(UserEntity registeredUser) {
    return UserResponse.builder()
            .id(registeredUser.getId())
            .name(registeredUser.getName())
            .email(registeredUser.getEmail())
            .role(registeredUser.getRole())
            .build();
}
}