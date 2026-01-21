package com.example.drones.emailService;

import com.example.drones.auth.dto.LoginRequest;
import com.example.drones.auth.dto.LoginResponse;
import com.example.drones.auth.dto.RegisterRequest;
import com.example.drones.orders.NewMatchedOrdersRepository;
import com.example.drones.orders.OrdersRepository;
import com.example.drones.orders.dto.OrderRequest;
import com.example.drones.orders.dto.OrderResponse;
import com.example.drones.services.OperatorServicesEntity;
import com.example.drones.services.OperatorServicesRepository;
import com.example.drones.services.ServicesEntity;
import com.example.drones.services.ServicesRepository;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class EmailIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("test", "test"))
            .withPerMethodLifecycle(true);

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private ServicesRepository servicesRepository;

    @Autowired
    private OperatorServicesRepository operatorServicesRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NewMatchedOrdersRepository newMatchedOrdersRepository;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RegisterRequest clientRegister;
    private LoginRequest clientLogin;
    private final String SERVICE_NAME = "Laser Scanning";

    @BeforeEach
    void setUp() {
        if (!servicesRepository.existsById(SERVICE_NAME)) {
            ServicesEntity service = new ServicesEntity();
            service.setName(SERVICE_NAME);
            servicesRepository.save(service);
        }

        clientRegister = RegisterRequest.builder()
                .displayName("testClient")
                .password("password123")
                .name("Jan")
                .surname("Testowy")
                .email("client@example.com")
                .phoneNumber("123456789")
                .build();

        clientLogin = LoginRequest.builder()
                .email("client@example.com")
                .password("password123")
                .build();
    }

    @AfterEach
    void tearDown() {
        ordersRepository.deleteAll();
        newMatchedOrdersRepository.deleteAll();
        operatorServicesRepository.deleteAll();
        userRepository.deleteAll();
        servicesRepository.deleteAll();
    }

    private String registerAndLoginClient() {
        testRestTemplate.postForEntity("/api/auth/register", clientRegister, Void.class);
        ResponseEntity<LoginResponse> response = testRestTemplate.postForEntity("/api/auth/login", clientLogin, LoginResponse.class);
        Assertions.assertNotNull(response.getBody());
        return response.getBody().token();
    }

    private HttpHeaders getHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-TOKEN", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private void createTestOperator(String username, String email, String coords, int radius, ServicesEntity service) {
        UserEntity operator = UserEntity.builder()
                .displayName(username)
                .email(email)
                .password(passwordEncoder.encode("pass"))
                .role(UserRole.OPERATOR)
                .name("Operator")
                .surname(username)
                .coordinates(coords)
                .radius(radius)
                .build();

        userRepository.save(operator);

        OperatorServicesEntity link = new OperatorServicesEntity();
        link.setOperator(operator);
        link.setServiceName(service.getName());
        operatorServicesRepository.save(link);

    }

    @Test
    void givenMatchingOperator_whenCreateOrder_thenEmailIsSentToOperator() throws MessagingException, IOException {
        // Given
        String clientToken = registerAndLoginClient();
        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();

        createTestOperator(
                "operator1",
                "operator1@test.com",
                "52.2200, 21.0100",
                20,
                service
        );

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Test Order")
                .description("Test description")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")  // W zasięgu operatora
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        // When
        HttpEntity<OrderRequest> requestEntity = new HttpEntity<>(orderRequest, getHeaders(clientToken));
        ResponseEntity<OrderResponse> response = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                requestEntity,
                OrderResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Czekaj na asynchroniczne dopasowanie operatorów i wysłanie emaila
        await().atMost(5, SECONDS).untilAsserted(() -> assertThat(greenMail.getReceivedMessages()).hasSize(1));

        MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
        assertThat(receivedMessage.getAllRecipients()[0].toString()).isEqualTo("operator1@test.com");
        assertThat(receivedMessage.getSubject()).isEqualTo("Nowe zlecenie: " + SERVICE_NAME);
        assertThat(receivedMessage.getContent().toString()).contains("Witaj Operator");
        assertThat(receivedMessage.getContent().toString()).contains(SERVICE_NAME);
    }

    @Test
    void givenMultipleMatchingOperators_whenCreateOrder_thenAllReceiveEmails() {
        // Given
        String clientToken = registerAndLoginClient();
        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();

        createTestOperator("operator1", "operator1@test.com", "52.2200, 21.0100", 20, service);
        createTestOperator("operator2", "operator2@test.com", "52.2250, 21.0150", 25, service);
        createTestOperator("operator3", "operator3@test.com", "52.2180, 21.0080", 30, service);

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Test Order")
                .description("Test description")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        // When
        HttpEntity<OrderRequest> requestEntity = new HttpEntity<>(orderRequest, getHeaders(clientToken));
        ResponseEntity<OrderResponse> response = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                requestEntity,
                OrderResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        await().atMost(5, SECONDS).untilAsserted(() -> assertThat(greenMail.getReceivedMessages()).hasSize(3));

        MimeMessage[] messages = greenMail.getReceivedMessages();
        List<String> recipients = Arrays.stream(messages)
                .map(msg -> {
                    try {
                        return msg.getAllRecipients()[0].toString();
                    } catch (MessagingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        assertThat(recipients).containsExactlyInAnyOrder(
                "operator1@test.com",
                "operator2@test.com",
                "operator3@test.com"
        );
    }

    @Test
    void givenOperatorOutOfRange_whenCreateOrder_thenNoEmailIsSent() {
        // Given
        String clientToken = registerAndLoginClient();
        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();

        // Operator daleko poza zasięgiem zamówienia
        createTestOperator(
                "operator_far",
                "operator_far@test.com",
                "50.0647, 19.9450",  // Kraków - daleko od zamówienia w Warszawie
                10,
                service
        );

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Test Order")
                .description("Test description")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")  // Warszawa
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        // When
        HttpEntity<OrderRequest> requestEntity = new HttpEntity<>(orderRequest, getHeaders(clientToken));
        ResponseEntity<OrderResponse> response = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                requestEntity,
                OrderResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Czekaj chwilę na ewentualne wysłanie emaila
        await().pollDelay(2, SECONDS).atMost(3, SECONDS).untilAsserted(() -> assertThat(greenMail.getReceivedMessages()).hasSize(0));
    }

    @Test
    void givenOperatorWithDifferentService_whenCreateOrder_thenNoEmailIsSent() {
        // Given
        String clientToken = registerAndLoginClient();

        // Dodaj inną usługę
        if (!servicesRepository.existsById("Fotografia/Wideo")) {
            ServicesEntity photoService = new ServicesEntity();
            photoService.setName("Fotografia/Wideo");
            servicesRepository.save(photoService);
        }
        ServicesEntity photoService = servicesRepository.findById("Fotografia/Wideo").orElseThrow();

        // Operator tylko dla usługi fotograficznej
        createTestOperator(
                "photo_operator",
                "photo_operator@test.com",
                "52.2200, 21.0100",
                50,
                photoService  // Inna usługa niż w zamówieniu
        );

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Laser Scan Order")
                .description("Need laser scanning")
                .service(SERVICE_NAME)  // Laser Scanning
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        // When
        HttpEntity<OrderRequest> requestEntity = new HttpEntity<>(orderRequest, getHeaders(clientToken));
        ResponseEntity<OrderResponse> response = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                requestEntity,
                OrderResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        await().pollDelay(2, SECONDS).atMost(3, SECONDS).untilAsserted(() -> assertThat(greenMail.getReceivedMessages()).hasSize(0));
    }

    @Test
    void givenMixedOperators_whenCreateOrder_thenOnlyMatchingOperatorsReceiveEmails() {
        // Given
        String clientToken = registerAndLoginClient();
        ServicesEntity laserService = servicesRepository.findById(SERVICE_NAME).orElseThrow();

        if (!servicesRepository.existsById("Fotografia/Wideo")) {
            ServicesEntity photoService = new ServicesEntity();
            photoService.setName("Fotografia/Wideo");
            servicesRepository.save(photoService);
        }
        ServicesEntity photoService = servicesRepository.findById("Fotografia/Wideo").orElseThrow();

        // Operatorzy pasujący (usługa + zasięg)
        createTestOperator("match1", "match1@test.com", "52.2200, 21.0100", 20, laserService);
        createTestOperator("match2", "match2@test.com", "52.2250, 21.0150", 25, laserService);

        // Operator z dobrą usługą, ale poza zasięgiem
        createTestOperator("far", "far@test.com", "50.0647, 19.9450", 10, laserService);

        // Operator w zasięgu, ale inna usługa
        createTestOperator("wrongService", "wrong@test.com", "52.2200, 21.0100", 50, photoService);

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Test Order")
                .description("Test description")
                .service(SERVICE_NAME)
                .coordinates("52.23, 21.01")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        // When
        HttpEntity<OrderRequest> requestEntity = new HttpEntity<>(orderRequest, getHeaders(clientToken));
        ResponseEntity<OrderResponse> response = testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                requestEntity,
                OrderResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        await().atMost(5, SECONDS).untilAsserted(() -> assertThat(greenMail.getReceivedMessages()).hasSize(2));

        MimeMessage[] messages = greenMail.getReceivedMessages();
        List<String> recipients = Arrays.stream(messages)
                .map(msg -> {
                    try {
                        return msg.getAllRecipients()[0].toString();
                    } catch (MessagingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        assertThat(recipients).containsExactlyInAnyOrder("match1@test.com", "match2@test.com");
    }

    @Test
    void givenOrder_whenEmailContainsGoogleMapsLink_thenLinkIsCorrect() throws MessagingException, IOException {
        // Given
        String clientToken = registerAndLoginClient();
        ServicesEntity service = servicesRepository.findById(SERVICE_NAME).orElseThrow();

        createTestOperator("operator1", "operator1@test.com", "52.2200, 21.0100", 20, service);

        OrderRequest orderRequest = OrderRequest.builder()
                .title("Test Order")
                .description("Test description")
                .service(SERVICE_NAME)
                .coordinates("52.2297, 21.0122")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .build();

        // When
        HttpEntity<OrderRequest> requestEntity = new HttpEntity<>(orderRequest, getHeaders(clientToken));
        testRestTemplate.exchange(
                "/api/orders/createOrder",
                HttpMethod.POST,
                requestEntity,
                OrderResponse.class
        );

        // Then
        await().atMost(5, SECONDS).untilAsserted(() -> assertThat(greenMail.getReceivedMessages()).hasSize(1));

        MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
        String emailContent = receivedMessage.getContent().toString();

        assertThat(emailContent).contains("https://www.google.com/maps/search/?api=1&query=52.2297,21.0122");
    }
}

