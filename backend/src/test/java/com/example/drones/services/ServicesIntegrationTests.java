package com.example.drones.services;

import com.example.drones.auth.dto.LoginRequest;
import com.example.drones.auth.dto.LoginResponse;
import com.example.drones.auth.dto.RegisterRequest;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
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

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ServicesIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private ServicesRepository servicesRepository;

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
        servicesRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerAndLogin(RegisterRequest register, LoginRequest login) {
        testRestTemplate.postForEntity("/api/auth/register", register, Void.class);
        ResponseEntity<LoginResponse> response = testRestTemplate.postForEntity("/api/auth/login", login, LoginResponse.class);
        return response.getBody().token();
    }

    private HttpHeaders getHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-TOKEN", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void givenAuthenticatedClient_whenGetServices_thenReturnsListFromDb() {
        servicesRepository.save(new ServicesEntity("Geodezja"));
        servicesRepository.save(new ServicesEntity("Fotografia"));

        String token = registerAndLogin(user1Register, user1Login);

        ResponseEntity<String[]> response = testRestTemplate.exchange(
                "/api/services/getServices",
                HttpMethod.GET,
                new HttpEntity<>(getHeaders(token)),
                String[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsExactlyInAnyOrder("Geodezja", "Fotografia");
    }

    @Test
    void givenClientUser_whenTryToAddServices_thenReturnsForbidden() {
        String clientToken = registerAndLogin(user1Register, user1Login);

        List<String> servicesToAdd = List.of("Hacking Service");
        HttpEntity<List<String>> request = new HttpEntity<>(servicesToAdd, getHeaders(clientToken));

        ResponseEntity<String> response = testRestTemplate.exchange(
                "/api/services",
                HttpMethod.POST,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(servicesRepository.existsById("Hacking Service")).isFalse();
    }

    @Test
    void givenUnauthenticatedUser_whenGetServices_thenReturnsForbidden() {
        ResponseEntity<Void> response = testRestTemplate.getForEntity("/api/services/getServices", Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void givenUnauthenticatedUser_whenTryToAddServices_thenReturnsForbidden() {
        List<String> servicesToAdd = List.of("Anonymous Service");
        HttpEntity<List<String>> request = new HttpEntity<>(servicesToAdd);

        ResponseEntity<String> response = testRestTemplate.exchange(
                "/api/services",
                HttpMethod.POST,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(servicesRepository.count()).isEqualTo(0);
    }

    @Test
    void givenAdminUser_whenAddListOfServices_thenServicesAreCreated() {
        testRestTemplate.postForEntity("/api/auth/register", user1Register, Void.class);

        UserEntity userEntity = userRepository.findByEmail(user1Register.email()).orElseThrow();
        userEntity.setRole(UserRole.ADMIN);
        userRepository.save(userEntity);

        ResponseEntity<LoginResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/auth/login",
                user1Login,
                LoginResponse.class
        );
        String adminToken = loginResponse.getBody().token();

        List<String> servicesToAdd = List.of("Geodezja", "Rolnictwo");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-TOKEN", "Bearer " + adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<String>> entity = new HttpEntity<>(servicesToAdd, headers);


        ResponseEntity<String[]> response = testRestTemplate.exchange(
                "/api/services",
                HttpMethod.POST,
                entity,
                String[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(response.getBody()).contains("Geodezja", "Rolnictwo");

        assertThat(servicesRepository.existsById("Geodezja")).isTrue();
        assertThat(servicesRepository.existsById("Rolnictwo")).isTrue();
    }
}
