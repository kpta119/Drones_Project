package com.example.drones.user;

import com.example.drones.auth.dto.LoginRequest;
import com.example.drones.auth.dto.LoginResponse;
import com.example.drones.auth.dto.RegisterRequest;
import com.example.drones.user.dto.UserResponse;
import com.example.drones.user.dto.UserUpdateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
        Assertions.assertNotNull(response.getBody());
        return response.getBody().token();
    }

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
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().getUsername()).isEqualTo("userOne");
        assertThat(response.getBody().getEmail()).isEqualTo("jan@example.com");
    }

    @Test
    void givenAuthenticatedUser_whenGetUserDataWithId_thenReturnsSpecificUser() {
        String tokenUser1 = registerAndLogin(user1Register, user1Login);

        RegisterRequest user2Register = RegisterRequest.builder()
                .displayName("userTwo")
                .password("pass")
                .name("Adam")
                .surname("Nowak")
                .email("adam@example.com")
                .phoneNumber("987654321")
                .build();
        testRestTemplate.postForEntity("/api/auth/register", user2Register, Void.class);

        UserEntity user2InDb = userRepository.findByEmail("adam@example.com").orElseThrow();
        UUID user2Id = user2InDb.getId();

        HttpEntity<Void> requestEntity = new HttpEntity<>(getHeaders(tokenUser1));

        ResponseEntity<UserResponse> response = testRestTemplate.exchange(
                "/api/user/getUserData?user_id=" + user2Id,
                HttpMethod.GET,
                requestEntity,
                UserResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().getUsername()).isEqualTo("userTwo");
        assertThat(response.getBody().getEmail()).isEqualTo("adam@example.com");
    }

    @Test
    void givenValidPatchRequest_whenEditUserData_thenUpdatesOnlyProvidedFields() {
        String token = registerAndLogin(user1Register, user1Login);

        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .name("Janusz")
                .phoneNumber("000000000")
                .build();

        HttpEntity<UserUpdateRequest> requestEntity = new HttpEntity<>(updateRequest, getHeaders(token));


        ResponseEntity<UserResponse> response = testRestTemplate.exchange(
                "/api/user/editUserData",
                HttpMethod.PATCH,
                requestEntity,
                UserResponse.class
        );


        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().getName()).isEqualTo("Janusz");
        assertThat(response.getBody().getPhoneNumber()).isEqualTo("000000000");
        assertThat(response.getBody().getSurname()).isEqualTo("Kowalski");

        UserEntity savedUser = userRepository.findByEmail("jan@example.com").orElseThrow();
        assertThat(savedUser.getName()).isEqualTo("Janusz");
        assertThat(savedUser.getSurname()).isEqualTo("Kowalski");
    }

    @Test
    void givenNoToken_whenGetUserData_thenReturnsUnauthorized() {
        ResponseEntity<Void> response = testRestTemplate.getForEntity("/api/user/getUserData", Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void givenClientUser_whenTryToChangeRole_thenReturnsForbidden() {
        String token = registerAndLogin(user1Register, user1Login);

        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .role(UserRole.ADMIN)
                .build();

        HttpEntity<UserUpdateRequest> requestEntity = new HttpEntity<>(updateRequest, getHeaders(token));
        ResponseEntity<String> response = testRestTemplate.exchange(
                "/api/user/editUserData",
                HttpMethod.PATCH,
                requestEntity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        UserEntity userInDb = userRepository.findByEmail("jan@example.com").orElseThrow();
        assertThat(userInDb.getRole()).isEqualTo(UserRole.CLIENT);
    }

    @Test
    void givenAdminUser_whenChangeRole_thenRoleIsUpdated() {
        testRestTemplate.postForEntity("/api/auth/register", user1Register, Void.class);


        UserEntity userEntity = userRepository.findByEmail(user1Register.email()).orElseThrow();
        userEntity.setRole(UserRole.ADMIN);
        userRepository.save(userEntity);

        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login",
                user1Login,
                LoginResponse.class
        );
        Assertions.assertNotNull(loginResponse.getBody());
        String adminToken = loginResponse.getBody().token();

        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .role(UserRole.OPERATOR)
                .build();

        HttpEntity<UserUpdateRequest> requestEntity = new HttpEntity<>(updateRequest, getHeaders(adminToken));

        ResponseEntity<UserResponse> response = testRestTemplate.exchange(
                "/api/user/editUserData",
                HttpMethod.PATCH,
                requestEntity,
                UserResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().getRole()).isEqualTo("OPERATOR");

        UserEntity updatedUserInDb = userRepository.findByEmail(user1Register.email()).orElseThrow();
        assertThat(updatedUserInDb.getRole()).isEqualTo(UserRole.OPERATOR);
    }

}
