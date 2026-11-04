package com.foodify.foodiesapi.service;

import com.foodify.foodiesapi.entity.UserEntity;
import com.foodify.foodiesapi.io.UserRequest;
import com.foodify.foodiesapi.io.UserResponse;
import com.foodify.foodiesapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
                .name("Test User")
                .email("test@example.com")
                .password("plain-password")
                .build();
    }

    @Test
    void registerUser_ignoresClientSuppliedRole_andAssignsUser() {
        request.setRole("ADMIN");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.registerUser(request);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertEquals("USER", captor.getValue().getRole());
        assertEquals("USER", response.getRole());
    }

    @Test
    void registerUser_encodesPasswordBeforeSaving() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.registerUser(request);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertEquals("encoded", captor.getValue().getPassword());
        assertNotEquals("plain-password", captor.getValue().getPassword());
    }

    @Test
    void registerUser_duplicateEmail_throwsAndDoesNotSave() {
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(new UserEntity()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.registerUser(request));

        assertEquals("Email already registered", ex.getMessage());
        verify(userRepository, never()).save(any());
    }
}