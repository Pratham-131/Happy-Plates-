package com.foodify.foodiesapi.service;

import com.foodify.foodiesapi.entity.UserEntity;
import com.foodify.foodiesapi.io.UserRequest;
import com.foodify.foodiesapi.io.UserResponse;
import com.foodify.foodiesapi.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRequest request;

    @BeforeEach
    void setUp() {
        request = UserRequest.builder()
                .name("Pratham")
                .email("pratham@test.com")
                .password("plainPass123")
                .role("USER")
                .build();
    }

    @Test
    void registerUser_success() {
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPass");

        UserEntity savedEntity = UserEntity.builder()
                .id("user123")
                .name(request.getName())
                .email(request.getEmail())
                .password("encodedPass")
                .role(request.getRole())
                .build();

        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);

        UserResponse response = userService.registerUser(request);

        assertNotNull(response);
        assertEquals("user123", response.getId());
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getEmail(), response.getEmail());
        assertEquals(request.getRole(), response.getRole());

        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordEncoder).encode(request.getPassword());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void registerUser_duplicateEmail_throwsException() {
        UserEntity existing = UserEntity.builder()
                .id("existingId")
                .email(request.getEmail())
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.registerUser(request));

        assertEquals("Email already registered", ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void registerUser_passwordIsEncoded_notStoredPlain() {
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPass");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.registerUser(request);

        verify(userRepository).save(argThat(entity ->
                entity.getPassword().equals("encodedPass") &&
                !entity.getPassword().equals(request.getPassword())
        ));
    }

    @Test
    void findByUserId_success() {
        UserEntity user = UserEntity.builder()
                .id("user456")
                .email("pratham@test.com")
                .build();

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);

            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("pratham@test.com");
            when(userRepository.findByEmail("pratham@test.com")).thenReturn(Optional.of(user));

            String userId = userService.findByUserId();

            assertEquals("user456", userId);
        }
    }

    @Test
    void findByUserId_userNotFound_throwsException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);

            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("ghost@test.com");
            when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.findByUserId());

            assertEquals("User not found", ex.getMessage());
        }
    }
}
