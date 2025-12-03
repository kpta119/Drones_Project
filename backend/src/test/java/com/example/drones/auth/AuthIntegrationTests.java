package com.example.drones.auth;

import com.example.drones.auth.dto.LoginRequest;
import com.example.drones.auth.dto.LoginResponse;
import com.example.drones.auth.dto.RegisterRequest;
import com.example.drones.config.exceptions.ErrorResponse;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private UserRepository userRepository;
    @Autowired
    TestRestTemplate testRestTemplate;

    private RegisterRequest validRegister;
    private LoginRequest validLogin;

    @BeforeEach
    void setUp() {
        validRegister = RegisterRequest.builder()
                .displayName("testUser")
                .password("password123")
                .name("Test")
                .surname("User")
                .email("user123@gmail.com")
                .phoneNumber("1234567890")
                .build();
        validLogin = LoginRequest.builder()
                .email("user123@gmail.com")
                .password("password123")
                .build();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void givenValidRegisterRequest_whenRegister_thenReturnsCreated() {
        RegisterRequest request = validRegister;

        ResponseEntity<Void> response = testRestTemplate.postForEntity("/api/auth/register", request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Optional<UserEntity> savedUser = userRepository.findByEmail("user123@gmail.com");
        assertThat(savedUser).isPresent();

        var user = savedUser.orElseThrow();
        assertThat(user.getDisplayName()).isEqualTo("testUser");
        assertThat(user.getId()).isNotNull();
        assertThat(user.getPassword()).isNotEqualTo("password123");

    }

    @Test
    void givenInvalidRegisterRequest_whenRegister_thenReturnsBadRequest() {
        RegisterRequest request = RegisterRequest.builder()
                .displayName("tu")
                .password("pwd")
                .name("")
                .surname("User")
                .email("invalid-email")
                .phoneNumber("123")
                .build();

        ResponseEntity<Void> response = testRestTemplate.postForEntity("/api/auth/register", request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Optional<UserEntity> savedUser = userRepository.findByEmail("invalid-email");
        assertThat(savedUser).isNotPresent();
    }

    @Test
    void givenDuplicateEmailRegisterRequest_whenRegister_thenReturnsBadRequest() {
        RegisterRequest request1 = validRegister;
        RegisterRequest request2 = RegisterRequest.builder()
                .displayName("testUser2")
                .password("password456")
                .name("Test2")
                .surname("User2")
                .email("user123@gmail.com")
                .phoneNumber("0987654321")
                .build();

        ResponseEntity<Void> response1 = testRestTemplate.postForEntity("/api/auth/register", request1, Void.class);
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ErrorResponse> response2 = testRestTemplate.postForEntity("/api/auth/register", request2, ErrorResponse.class);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Assertions.assertNotNull(response2.getBody());
        assertThat(response2.getBody().message()).isEqualTo("User with email user123@gmail.com already exists.");

        Optional<UserEntity> savedUsers = userRepository.findByEmail("user123@gmail.com");
        assertThat(savedUsers).isPresent();
    }

    @Test
    void givenValidLoginRequest_whenLogin_thenReturnsOk() {
        RegisterRequest registerRequest = validRegister;
        LoginRequest loginRequest = validLogin;

        testRestTemplate.postForEntity("/api/auth/register", registerRequest, Void.class);
        ResponseEntity<LoginResponse> response = testRestTemplate.postForEntity("/api/auth/login", loginRequest, LoginResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void givenInvalidEmail_whenLogin_thenReturnsUnauthorized() {
        RegisterRequest registerRequest = validRegister;
        LoginRequest loginRequest = LoginRequest.builder()
                .email("user@gmailllll.com")
                .password("password123")
                .build();

        testRestTemplate.postForEntity("/api/auth/register", registerRequest, Void.class);
        ResponseEntity<ErrorResponse> response = testRestTemplate.postForEntity("/api/auth/login", loginRequest, ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().message()).isEqualTo("The provided credentials are invalid.");

    }

    @Test
    void givenInvalidPassword_whenLogin_thenReturnsUnauthorized() {
        RegisterRequest registerRequest = validRegister;
        LoginRequest loginRequest = LoginRequest.builder()
                .email("user123@gmail.com")
                .password("password1233333333")
                .build();

        testRestTemplate.postForEntity("/api/auth/register", registerRequest, Void.class);
        ResponseEntity<ErrorResponse> response = testRestTemplate.postForEntity("/api/auth/login", loginRequest, ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().message()).isEqualTo("The provided credentials are invalid.");

    }
}
