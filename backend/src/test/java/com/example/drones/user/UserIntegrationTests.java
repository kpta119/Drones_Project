package com.example.drones.user;

import com.example.drones.auth.dto.LoginRequest;
import com.example.drones.auth.dto.LoginResponse;
import com.example.drones.auth.dto.RegisterRequest;
import com.example.drones.user.dto.UserResponse;
import com.example.drones.user.dto.UserUpdateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserIntegrationTests {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestRestTemplate testRestTemplate;

    private RegisterRequest user1Register;
    private LoginRequest user1Login;

    @BeforeEach
    void setUp() {
        user1Register = RegisterRequest.builder()
                .displayName("userOne")
                .password("password123")
                .name("Jan")
                .surname("Kowalski")
                .email("jan@example.com")
                .phoneNumber("123456789")
                .build();

        user1Login = LoginRequest.builder()
                .email("jan@example.com")
                .password("password123")
                .build();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private String registerAndLogin(RegisterRequest register, LoginRequest login) {
        testRestTemplate.postForEntity("/api/auth/register", register, Void.class);
        ResponseEntity<LoginResponse> response = testRestTemplate.postForEntity("/api/auth/login", login, LoginResponse.class);
        return response.getBody().token();
    }

    // Pomocnicza metoda: Tworzy nagłówki z Twoim niestandardowym X-USER-TOKEN
    private HttpHeaders getHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-TOKEN", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void givenAuthenticatedUser_whenGetUserData_thenReturnsCurrentUserProfile() {
        String token = registerAndLogin(user1Register, user1Login);

        HttpEntity<Void> requestEntity = new HttpEntity<>(getHeaders(token));

        ResponseEntity<UserResponse> response = testRestTemplate.exchange(
                "/api/user/getUserData",
                HttpMethod.GET,
                requestEntity,
                UserResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("userOne");
        assertThat(response.getBody().getEmail()).isEqualTo("jan@example.com");
    }

    @Test
    void givenAuthenticatedUser_whenGetUserDataWithId_thenReturnsSpecificUser() {
        // 1. Zarejestruj User 1 (tego będziemy pytać)
        String tokenUser1 = registerAndLogin(user1Register, user1Login);

        // 2. Zarejestruj User 2 (tego będziemy szukać w bazie)
        RegisterRequest user2Register = RegisterRequest.builder()
                .displayName("userTwo")
                .password("pass")
                .name("Adam")
                .surname("Nowak")
                .email("adam@example.com")
                .phoneNumber("987654321")
                .build();
        testRestTemplate.postForEntity("/api/auth/register", user2Register, Void.class);

        // Pobierz ID Usera 2 z bazy
        UserEntity user2InDb = userRepository.findByEmail("adam@example.com").orElseThrow();
        UUID user2Id = user2InDb.getId();

        // 3. User 1 pyta o dane Usera 2
        HttpEntity<Void> requestEntity = new HttpEntity<>(getHeaders(tokenUser1));

        ResponseEntity<UserResponse> response = testRestTemplate.exchange(
                "/api/user/getUserData?user_id=" + user2Id,
                HttpMethod.GET,
                requestEntity,
                UserResponse.class
        );

        // 4. Sprawdź czy zwrócono dane Usera 2
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUsername()).isEqualTo("userTwo");
        assertThat(response.getBody().getEmail()).isEqualTo("adam@example.com");
    }

    @Test
    void givenValidPatchRequest_whenEditUserData_thenUpdatesOnlyProvidedFields() {
        String token = registerAndLogin(user1Register, user1Login);

        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setName("Janusz");
        updateRequest.setPhoneNumber("000000000");

        HttpEntity<UserUpdateRequest> requestEntity = new HttpEntity<>(updateRequest, getHeaders(token));


        ResponseEntity<UserResponse> response = testRestTemplate.exchange(
                "/api/user/editUserData",
                HttpMethod.PATCH,
                requestEntity,
                UserResponse.class
        );


        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getName()).isEqualTo("Janusz");
        assertThat(response.getBody().getPhoneNumber()).isEqualTo("000000000");
        assertThat(response.getBody().getSurname()).isEqualTo("Kowalski");

        UserEntity savedUser = userRepository.findByEmail("jan@example.com").orElseThrow();
        assertThat(savedUser.getName()).isEqualTo("Janusz");
        assertThat(savedUser.getSurname()).isEqualTo("Kowalski");
    }

    @Test
    void givenNoToken_whenGetUserData_thenReturnsForbidden() {
        ResponseEntity<Void> response = testRestTemplate.getForEntity("/api/user/getUserData", Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    
}
