package com.example.drones.auth;

import com.example.drones.auth.dto.LoginRequest;
import com.example.drones.auth.dto.LoginResponse;
import com.example.drones.auth.dto.RegisterRequest;
import com.example.drones.auth.exceptions.InvalidCredentialsException;
import com.example.drones.auth.exceptions.UserAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTests {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

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
    public void givenValidRegisterRequest_whenRegister_thenReturnsCreated() {
        RegisterRequest request = validRegister;
        doNothing().when(authService).register(request);

        ResponseEntity<Void> response = authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(authService).register(request);
    }

    @Test
    public void givenValidLoginRequest_whenLogin_thenReturnsOk() {
        LoginRequest request = validLogin;

        String expectedToken = "mock";
        when(authService.login(request)).thenReturn(new LoginResponse(expectedToken,
                null,
                null,
                null,
                null));

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedToken, response.getBody().token());
        verify(authService).login(request);
    }

    @Test
    public void givenExistingUser_whenRegister_thenThrowsUserAlreadyExistsException() {
        RegisterRequest request = validRegister;
        doThrow(new UserAlreadyExistsException(request.email()))
                .when(authService).register(request);

        assertThrows(UserAlreadyExistsException.class, () -> authController.register(request));
        verify(authService).register(request);
    }

    @Test
    public void givenInvalidCredentials_whenLogin_thenThrowsInvalidCredentialsException() {
        LoginRequest request = validLogin;
        when(authService.login(request)).thenThrow(new InvalidCredentialsException());

        assertThrows(InvalidCredentialsException.class, () -> authController.login(request));
        verify(authService).login(request);
    }

}
