package com.example.drones.auth;

import com.example.drones.auth.dto.LoginRequest;
import com.example.drones.auth.dto.RegisterRequest;
import com.example.drones.auth.exceptions.InvalidCredentialsException;
import com.example.drones.auth.exceptions.UserAlreadyExistsException;
import com.example.drones.config.JwtService;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserMapper;
import com.example.drones.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegister;
    private LoginRequest validLogin;

    @BeforeEach
    public void setUp() {
        validRegister = RegisterRequest.builder()
                .displayName("testUser")
                .password("password123")
                .name("Test")
                .surname("User")
                .email("user123@gmail.com")
                .phoneNumber("1234567890")
                .build();
        validLogin = LoginRequest.builder()
                .email("user@gmail.com")
                .password("password123")
                .build();
    }

    @Test
    public void givenValidRegisterRequest_whenRegister_thenUserIsSaved() {
        RegisterRequest registerRequest = validRegister;
        UserEntity expectedUser = UserEntity.builder()
                .id(null)
                .displayName(registerRequest.displayName())
                .name(registerRequest.name())
                .surname(registerRequest.surname())
                .email(registerRequest.email())
                .phoneNumber(registerRequest.phoneNumber())
                .password("hashedPassword")
                .build();

        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("hashedPassword");
        when(userMapper.toEntity(registerRequest, "hashedPassword")).thenReturn(expectedUser);

        authService.register(registerRequest);

        verify(userRepository).save(expectedUser);
    }

    @Test
    public void givenExistingEmail_whenRegister_thenUserAlreadyExistsExceptionIsThrown() {
        RegisterRequest registerRequest = validRegister;
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);
        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class,
                () -> authService.register(registerRequest)
        );

        assertThat(ex.getMessage()).contains(registerRequest.email());
        verify(userRepository, never()).save(any());
    }

    @Test
    public void givenValidLoginRequest_whenLogin_thenReturnsJwtToken() {
        LoginRequest loginRequest = validLogin;
        UserDetails userDetails = new User(
                "550e8400-e29b-41d4-a716-446655440000",
                "password123",
                java.util.Collections.emptyList()
        );
        Authentication authentication = mock(Authentication.class);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(any(UUID.class))).thenReturn("jwtToken123");

        var response = authService.login(loginRequest);
        assertThat(response.token()).isEqualTo("jwtToken123");
    }

    @Test
    public void givenInvalidLoginRequest_whenLogin_thenInvalidCredentialsExceptionIsThrown() {
        LoginRequest loginRequest = validLogin;
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new InternalAuthenticationServiceException("Bad credentials"));
        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertThat(ex.getMessage()).isEqualTo("The provided credentials are invalid.");
    }

}
