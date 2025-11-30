package com.example.drones.config;

import com.example.drones.config.exceptions.UserNotFoundException;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private UserEntity testUser;
    private UUID testUserId;
    private String testEmail;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";

        testUser = UserEntity.builder()
                .id(testUserId)
                .email(testEmail)
                .password("hashedPassword123")
                .displayName("Test User")
                .name("Test")
                .surname("User")
                .phoneNumber("+48123456789")
                .role(UserRole.CLIENT)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void givenValidEmail_whenLoadUserByUsername_thenReturnUserDetails() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(testEmail);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(testUserId.toString());
        assertThat(userDetails.getPassword()).isEqualTo("hashedPassword123");
        verify(userRepository).findByEmail(testEmail);
    }

    @Test
    void givenInvalidEmail_whenLoadUserByUsername_thenThrowUserNotFoundException() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(testEmail))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository).findByEmail(testEmail);
    }

    @Test
    void givenValidId_whenLoadUserById_thenReturnUserDetails() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = customUserDetailsService.loadUserById(testUserId);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(testUserId.toString());
        verify(userRepository).findById(testUserId);
    }

    @Test
    void givenInvalidId_whenLoadUserById_thenThrowUserNotFoundException() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserById(nonExistentId))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository).findById(nonExistentId);
    }

    @Test
    void givenBlockedUser_whenLoadUserByUsername_thenThrowLockedException() {
        testUser.setRole(UserRole.BLOCKED);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(testEmail))
                .isInstanceOf(LockedException.class)
                .hasMessageContaining("User account is blocked");
        verify(userRepository).findByEmail(testEmail);
    }

    @Test
    void givenAdminUser_whenLoadUserByUsername_thenReturnUserDetailsWithAdminRole() {
        testUser.setRole(UserRole.ADMIN);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(testEmail);

        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_ADMIN");
    }
}
