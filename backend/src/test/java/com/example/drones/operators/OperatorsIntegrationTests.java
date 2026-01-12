package com.example.drones.operators;

import com.example.drones.common.config.auth.JwtService;
import com.example.drones.operators.dto.CreateOperatorProfileDto;
import com.example.drones.operators.dto.CreatePortfolioDto;
import com.example.drones.operators.dto.OperatorProfileDto;
import com.example.drones.operators.dto.UpdatePortfolioDto;
import com.example.drones.orders.MatchedOrderStatus;
import com.example.drones.orders.NewMatchedOrderEntity;
import com.example.drones.orders.OrderStatus;
import com.example.drones.orders.OrdersEntity;
import com.example.drones.photos.PhotoEntity;
import com.example.drones.services.OperatorServicesEntity;
import com.example.drones.services.OperatorServicesRepository;
import com.example.drones.services.ServicesEntity;
import com.example.drones.services.ServicesRepository;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class OperatorsIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OperatorServicesRepository operatorServicesRepository;
    @Autowired
    private ServicesRepository servicesRepository;
    @Autowired
    private PortfolioRepository portfolioRepository;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private com.example.drones.orders.OrdersRepository ordersRepository;
    @Autowired
    private com.example.drones.orders.NewMatchedOrdersRepository newMatchedOrdersRepository;
    @Autowired
    private EntityManager entityManager;

    private UserEntity testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        createService("Aerial Photography");
        createService("Surveying");
        createService("Delivery");
        createService("Inspection");

        testUser = UserEntity.builder()
                .displayName("testOperator")
                .name("Test")
                .surname("Operator")
                .email("operator@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("123456789")
                .role(UserRole.CLIENT)
                .build();
        testUser = userRepository.save(testUser);

        jwtToken = jwtService.generateToken(testUser.getId());
    }

    private void createService(String serviceName) {
        ServicesEntity service = new ServicesEntity();
        service.setName(serviceName);
        servicesRepository.save(service);
    }

    private void addServiceToOperator(UserEntity operator, String serviceName) {
        com.example.drones.services.OperatorServicesEntity operatorService = new OperatorServicesEntity();
        operatorService.setServiceName(serviceName);
        operatorService.setOperator(operator);
        operatorServicesRepository.save(operatorService);
    }

    @Test
    void givenValidOperatorDto_whenCreateOperatorProfile_thenReturnsCreatedAndPersistsToDatabase() throws Exception {
        CreateOperatorProfileDto operatorDto = CreateOperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of("UAV License", "Commercial Pilot"))
                .services(List.of("Aerial Photography", "Surveying"))
                .build();

        mockMvc.perform(post("/api/operators/createOperatorProfile")
                        .header("X-USER-TOKEN", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operatorDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coordinates").exists())
                .andExpect(jsonPath("$.radius").exists())
                .andExpect(jsonPath("$.certificates").exists())
                .andExpect(jsonPath("$.services").exists());

        UserEntity updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getRole()).isEqualTo(UserRole.OPERATOR);
        assertThat(updatedUser.getCoordinates()).isEqualTo("52.2297,21.0122");
        assertThat(updatedUser.getRadius()).isEqualTo(50);

        var operatorServices = operatorServicesRepository.findAll();
        assertThat(operatorServices).hasSize(2);
    }

    @Test
    void givenNoAuthToken_whenCreateOperatorProfile_thenReturnsUnauthorized() throws Exception {
        CreateOperatorProfileDto operatorDto = CreateOperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of("UAV License"))
                .services(List.of("Aerial Photography"))
                .build();

        mockMvc.perform(post("/api/operators/createOperatorProfile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operatorDto)))
                .andExpect(status().isUnauthorized());

        UserEntity unchangedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(unchangedUser.getRole()).isEqualTo(UserRole.CLIENT);
    }

    @Test
    void givenInvalidToken_whenCreateOperatorProfile_thenReturnsUnauthorized() throws Exception {
        CreateOperatorProfileDto operatorDto = CreateOperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of("UAV License"))
                .services(List.of("Aerial Photography"))
                .build();

        mockMvc.perform(post("/api/operators/createOperatorProfile")
                        .header("X-USER-TOKEN", "Bearer invalid.token.here")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operatorDto)))
                .andExpect(status().isUnauthorized());

        UserEntity unchangedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(unchangedUser.getRole()).isEqualTo(UserRole.CLIENT);
    }

    @Test
    void givenValidOperatorDto_whenEditOperatorProfile_thenReturnsAcceptedAndPersistsToDatabase() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("50.0,20.0");
        testUser.setRadius(30);
        userRepository.save(testUser);

        OperatorProfileDto operatorDto =
                OperatorProfileDto.builder()
                        .coordinates("52.2297,21.0122")
                        .radius(100)
                        .certificates(List.of("Advanced License"))
                        .services(List.of("Delivery", "Inspection"))
                        .build();

        mockMvc.perform(patch("/api/operators/editOperatorProfile")
                        .header("X-USER-TOKEN", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operatorDto)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.coordinates").exists())
                .andExpect(jsonPath("$.radius").exists())
                .andExpect(jsonPath("$.certificates").exists())
                .andExpect(jsonPath("$.services").exists());

        UserEntity updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getCoordinates()).isEqualTo("52.2297,21.0122");
        assertThat(updatedUser.getRadius()).isEqualTo(100);

        var operatorServices = operatorServicesRepository.findAllByOperatorId(updatedUser.getId());
        assertThat(operatorServices).hasSize(2);
    }

    @Test
    void givenNoAuthToken_whenEditOperatorProfile_thenReturnsUnauthorized() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        userRepository.save(testUser);

        OperatorProfileDto operatorDto =
                OperatorProfileDto.builder()
                        .coordinates("52.2297,21.0122")
                        .radius(100)
                        .certificates(List.of("Advanced License"))
                        .services(List.of("Aerial Photography"))
                        .build();

        mockMvc.perform(patch("/api/operators/editOperatorProfile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operatorDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenInvalidToken_whenEditOperatorProfile_thenReturnsUnauthorized() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        userRepository.save(testUser);

        OperatorProfileDto operatorDto =
                OperatorProfileDto.builder()
                        .coordinates("52.2297,21.0122")
                        .radius(100)
                        .certificates(List.of("Advanced License"))
                        .services(List.of("Aerial Photography"))
                        .build();

        mockMvc.perform(patch("/api/operators/editOperatorProfile")
                        .header("X-USER-TOKEN", "Bearer invalid.token.here")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operatorDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenValidPortfolioDto_whenAddPortfolio_thenReturnsCreatedAndPersistsToDatabase() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        userRepository.save(testUser);

        CreatePortfolioDto portfolioDto = CreatePortfolioDto.builder()
                .title("Aerial Photography Portfolio")
                .description("Collection of my best aerial photography work")
                .build();

        mockMvc.perform(post("/api/operators/addPortfolio")
                        .header("X-USER-TOKEN", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(portfolioDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Aerial Photography Portfolio"))
                .andExpect(jsonPath("$.description").value("Collection of my best aerial photography work"))
                .andExpect(jsonPath("$.photos").isArray());

        var portfolios = portfolioRepository.findAll();
        assertThat(portfolios).hasSize(1);
        assertThat(portfolios.getFirst().getTitle()).isEqualTo("Aerial Photography Portfolio");
        assertThat(portfolios.getFirst().getDescription()).isEqualTo("Collection of my best aerial photography work");
        assertThat(portfolios.getFirst().getOperator().getId()).isEqualTo(testUser.getId());
    }

    @Test
    void givenNoAuthToken_whenAddPortfolio_thenReturnsUnauthorized() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        userRepository.save(testUser);

        CreatePortfolioDto portfolioDto = CreatePortfolioDto.builder()
                .title("Aerial Photography Portfolio")
                .description("Collection of my best aerial photography work")
                .build();

        mockMvc.perform(post("/api/operators/addPortfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(portfolioDto)))
                .andExpect(status().isUnauthorized());

        var portfolios = portfolioRepository.findAll();
        assertThat(portfolios).isEmpty();
    }

    @Test
    void givenInvalidToken_whenAddPortfolio_thenReturnsUnauthorized() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        userRepository.save(testUser);

        CreatePortfolioDto portfolioDto = CreatePortfolioDto.builder()
                .title("Aerial Photography Portfolio")
                .description("Collection of my best aerial photography work")
                .build();

        mockMvc.perform(post("/api/operators/addPortfolio")
                        .header("X-USER-TOKEN", "Bearer invalid.token.here")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(portfolioDto)))
                .andExpect(status().isUnauthorized());

        var portfolios = portfolioRepository.findAll();
        assertThat(portfolios).isEmpty();
    }

    @Test
    void givenValidPortfolioDto_whenEditPortfolio_thenReturnsAcceptedAndPersistsToDatabase() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        userRepository.save(testUser);

        PortfolioEntity portfolio = PortfolioEntity.builder()
                .operator(testUser)
                .title("Original Portfolio Title")
                .description("Original description")
                .build();
        portfolioRepository.save(portfolio);
        PhotoEntity photo = PhotoEntity.builder()
                .name("photo name")
                .url("https://example.com/photo.jpg")
                .portfolio(portfolio)
                .build();

        portfolio.addPhoto(photo);

        UpdatePortfolioDto updateDto = UpdatePortfolioDto.builder()
                .title("Updated Portfolio Title")
                .description("Updated description with more details")
                .build();

        mockMvc.perform(patch("/api/operators/editPortfolio")
                        .header("X-USER-TOKEN", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.title").value("Updated Portfolio Title"))
                .andExpect(jsonPath("$.description").value("Updated description with more details"))
                .andExpect(jsonPath("$.photos").isArray())
                .andExpect(jsonPath("$.photos[0].name").value("photo name"))
                .andExpect(jsonPath("$.photos[0].url").value("https://example.com/photo.jpg"));

        var portfolios = portfolioRepository.findAll();
        assertThat(portfolios).hasSize(1);
        assertThat(portfolios.getFirst().getTitle()).isEqualTo("Updated Portfolio Title");
        assertThat(portfolios.getFirst().getDescription()).isEqualTo("Updated description with more details");
        assertThat(portfolios.getFirst().getOperator().getId()).isEqualTo(testUser.getId());
    }

    @Test
    void givenPartialUpdate_whenEditPortfolio_thenUpdatesOnlyProvidedFields() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        userRepository.save(testUser);

        PortfolioEntity portfolio = PortfolioEntity.builder()
                .operator(testUser)
                .title("Original Portfolio Title")
                .description("Original description")
                .build();
        portfolioRepository.save(portfolio);
        PhotoEntity photo = PhotoEntity.builder()
                .name("photo name")
                .url("https://example.com/photo.jpg")
                .portfolio(portfolio)
                .build();

        portfolio.addPhoto(photo);

        UpdatePortfolioDto updateDto = UpdatePortfolioDto.builder()
                .title("New Title Only")
                .description(null)
                .build();

        mockMvc.perform(patch("/api/operators/editPortfolio")
                        .header("X-USER-TOKEN", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.title").value("New Title Only"))
                .andExpect(jsonPath("$.description").value("Original description"))
                .andExpect(jsonPath("$.photos[0].name").value("photo name"))
                .andExpect(jsonPath("$.photos[0].url").value("https://example.com/photo.jpg"));

        var portfolios = portfolioRepository.findAll();
        assertThat(portfolios).hasSize(1);
        assertThat(portfolios.getFirst().getTitle()).isEqualTo("New Title Only");
        assertThat(portfolios.getFirst().getDescription()).isEqualTo("Original description");
    }

    @Test
    void givenNoAuthToken_whenEditPortfolio_thenReturnsUnauthorized() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        userRepository.save(testUser);

        PortfolioEntity portfolio = PortfolioEntity.builder()
                .operator(testUser)
                .title("Original Portfolio Title")
                .description("Original description")
                .build();
        portfolioRepository.save(portfolio);

        UpdatePortfolioDto updateDto = UpdatePortfolioDto.builder()
                .title("Updated Title")
                .description("Updated description")
                .build();

        mockMvc.perform(patch("/api/operators/editPortfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isUnauthorized());

        var portfolios = portfolioRepository.findAll();
        assertThat(portfolios).hasSize(1);
        assertThat(portfolios.getFirst().getTitle()).isEqualTo("Original Portfolio Title");
        assertThat(portfolios.getFirst().getDescription()).isEqualTo("Original description");
    }

    @Test
    void givenInvalidToken_whenEditPortfolio_thenReturnsUnauthorized() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        userRepository.save(testUser);

        PortfolioEntity portfolio = PortfolioEntity.builder()
                .operator(testUser)
                .title("Original Portfolio Title")
                .description("Original description")
                .build();
        portfolioRepository.save(portfolio);

        UpdatePortfolioDto updateDto = UpdatePortfolioDto.builder()
                .title("Updated Title")
                .description("Updated description")
                .build();

        mockMvc.perform(patch("/api/operators/editPortfolio")
                        .header("X-USER-TOKEN", "Bearer invalid.token.here")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isUnauthorized());

        var portfolios = portfolioRepository.findAll();
        assertThat(portfolios).hasSize(1);
        assertThat(portfolios.getFirst().getTitle()).isEqualTo("Original Portfolio Title");
        assertThat(portfolios.getFirst().getDescription()).isEqualTo("Original description");
    }

    @Test
    void givenValidOperatorWithPortfolio_whenGetOperatorProfile_thenReturnsOkWithCompleteProfile() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("52.2297,21.0122");
        testUser.setRadius(50);
        testUser.setCertificates(List.of("UAV License", "Commercial Pilot"));
        userRepository.save(testUser);

        PortfolioEntity portfolio = PortfolioEntity.builder()
                .operator(testUser)
                .title("My Portfolio")
                .description("Portfolio description")
                .build();
        portfolio = portfolioRepository.save(portfolio);
        testUser.setPortfolio(portfolio);
        userRepository.save(testUser);

        PhotoEntity photo = PhotoEntity.builder()
                .name("photo name")
                .url("https://example.com/photo.jpg")
                .portfolio(portfolio)
                .build();
        portfolio.addPhoto(photo);

        addServiceToOperator(testUser, "Aerial Photography");
        addServiceToOperator(testUser, "Surveying");

        mockMvc.perform(get("/api/operators/getOperatorProfile/{userId}", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test"))
                .andExpect(jsonPath("$.surname").value("Operator"))
                .andExpect(jsonPath("$.username").value("testOperator"))
                .andExpect(jsonPath("$.email").value("operator@test.com"))
                .andExpect(jsonPath("$.phone_number").value("123456789"))
                .andExpect(jsonPath("$.certificates").isArray())
                .andExpect(jsonPath("$.certificates[0]").value("UAV License"))
                .andExpect(jsonPath("$.certificates[1]").value("Commercial Pilot"))
                .andExpect(jsonPath("$.operator_services").isArray())
                .andExpect(jsonPath("$.operator_services.length()").value(2))
                .andExpect(jsonPath("$.portfolio").exists())
                .andExpect(jsonPath("$.portfolio.title").value("My Portfolio"))
                .andExpect(jsonPath("$.portfolio.description").value("Portfolio description"))
                .andExpect(jsonPath("$.portfolio.photos").isArray())
                .andExpect(jsonPath("$.portfolio.photos[0].name").value("photo name"))
                .andExpect(jsonPath("$.portfolio.photos[0].url").value("https://example.com/photo.jpg"));
    }

    @Test
    void givenValidOperatorWithoutPortfolio_whenGetOperatorProfile_thenReturnsOkWithNullPortfolio() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("40.7128,74.0060");
        testUser.setRadius(30);
        testUser.setCertificates(List.of("Basic UAV License"));
        userRepository.save(testUser);

        addServiceToOperator(testUser, "Delivery");

        mockMvc.perform(get("/api/operators/getOperatorProfile/{userId}", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test"))
                .andExpect(jsonPath("$.surname").value("Operator"))
                .andExpect(jsonPath("$.username").value("testOperator"))
                .andExpect(jsonPath("$.email").value("operator@test.com"))
                .andExpect(jsonPath("$.phone_number").value("123456789"))
                .andExpect(jsonPath("$.certificates").isArray())
                .andExpect(jsonPath("$.certificates[0]").value("Basic UAV License"))
                .andExpect(jsonPath("$.operator_services").isArray())
                .andExpect(jsonPath("$.operator_services.length()").value(1))
                .andExpect(jsonPath("$.operator_services[0]").value("Delivery"))
                .andExpect(jsonPath("$.portfolio").doesNotExist());
    }

    @Test
    void givenValidOperatorWithNoServices_whenGetOperatorProfile_thenReturnsOkWithEmptyServices() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("51.5074,0.1278");
        testUser.setRadius(25);
        testUser.setCertificates(List.of());
        userRepository.save(testUser);

        mockMvc.perform(get("/api/operators/getOperatorProfile/{userId}", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test"))
                .andExpect(jsonPath("$.surname").value("Operator"))
                .andExpect(jsonPath("$.username").value("testOperator"))
                .andExpect(jsonPath("$.email").value("operator@test.com"))
                .andExpect(jsonPath("$.certificates").isArray())
                .andExpect(jsonPath("$.certificates.length()").value(0))
                .andExpect(jsonPath("$.operator_services").isArray())
                .andExpect(jsonPath("$.operator_services.length()").value(0))
                .andExpect(jsonPath("$.portfolio").doesNotExist());
    }

    @Test
    void givenNonExistentUserId_whenGetOperatorProfile_thenReturnsNotFound() throws Exception {
        UUID nonExistentUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/operators/getOperatorProfile/{userId}", nonExistentUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenNonOperatorUser_whenGetOperatorProfile_thenReturnsNotFound() throws Exception {
        testUser.setRole(UserRole.CLIENT);
        userRepository.save(testUser);

        mockMvc.perform(get("/api/operators/getOperatorProfile/{userId}", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenValidOrderWithMultipleInterestedOperators_whenGetOperatorsInfo_thenReturnsOkWithOperatorsList() throws Exception {
        ServicesEntity service = servicesRepository.findById("Aerial Photography").orElseThrow();

        OrdersEntity order = OrdersEntity.builder()
                .title("Real Estate Photography")
                .description("Need aerial photos of property")
                .user(testUser)
                .service(service)
                .coordinates("52.2297,21.0122")
                .status(OrderStatus.OPEN)
                .createdAt(java.time.LocalDateTime.now())
                .fromDate(java.time.LocalDateTime.now().plusDays(1))
                .toDate(java.time.LocalDateTime.now().plusDays(2))
                .build();
        order = ordersRepository.save(order);

        UserEntity operator1 = UserEntity.builder()
                .displayName("operator1")
                .name("John")
                .surname("Smith")
                .email("john.operator@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("111222333")
                .role(UserRole.OPERATOR)
                .certificates(List.of("UAV License", "Commercial Pilot"))
                .build();
        operator1 = userRepository.save(operator1);

        UserEntity operator2 = UserEntity.builder()
                .displayName("operator2")
                .name("Jane")
                .surname("Doe")
                .email("jane.operator@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("444555666")
                .role(UserRole.OPERATOR)
                .certificates(List.of("Advanced Drone License"))
                .build();
        operator2 = userRepository.save(operator2);

        NewMatchedOrderEntity match1 = NewMatchedOrderEntity.builder()
                .order(order)
                .operator(operator1)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match1);

        NewMatchedOrderEntity match2 = NewMatchedOrderEntity.builder()
                .order(order)
                .operator(operator2)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match2);

        mockMvc.perform(get("/api/operators/getOperatorsInfo/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].user_id").exists())
                .andExpect(jsonPath("$[0].username").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].surname").exists())
                .andExpect(jsonPath("$[0].certificates").isArray())
                .andExpect(jsonPath("$[1].user_id").exists())
                .andExpect(jsonPath("$[1].username").exists())
                .andExpect(jsonPath("$[1].name").exists())
                .andExpect(jsonPath("$[1].surname").exists())
                .andExpect(jsonPath("$[1].certificates").isArray());
    }

    @Test
    void givenValidOrderWithNoInterestedOperators_whenGetOperatorsInfo_thenReturnsOkWithEmptyList() throws Exception {
        ServicesEntity service = servicesRepository.findById("Surveying").orElseThrow();

        OrdersEntity order = OrdersEntity.builder()
                .title("Land Survey")
                .description("Need topographic survey")
                .user(testUser)
                .service(service)
                .coordinates("52.2400,21.0300")
                .status(OrderStatus.OPEN)
                .createdAt(java.time.LocalDateTime.now())
                .fromDate(java.time.LocalDateTime.now().plusDays(1))
                .toDate(java.time.LocalDateTime.now().plusDays(2))
                .build();
        order = ordersRepository.save(order);

        mockMvc.perform(get("/api/operators/getOperatorsInfo/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void givenOrderWithOnlyPendingOperators_whenGetOperatorsInfo_thenReturnsOkWithEmptyList() throws Exception {
        ServicesEntity service = servicesRepository.findById("Delivery").orElseThrow();

        OrdersEntity order = OrdersEntity.builder()
                .title("Package Delivery")
                .description("Need urgent delivery")
                .user(testUser)
                .service(service)
                .coordinates("52.2500,21.0400")
                .status(OrderStatus.OPEN)
                .createdAt(java.time.LocalDateTime.now())
                .fromDate(java.time.LocalDateTime.now().plusHours(2))
                .toDate(java.time.LocalDateTime.now().plusHours(4))
                .build();
        order = ordersRepository.save(order);

        UserEntity operator = UserEntity.builder()
                .displayName("pendingOperator")
                .name("Pending")
                .surname("Operator")
                .email("pending.operator@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("777888999")
                .role(UserRole.OPERATOR)
                .certificates(List.of("Basic License"))
                .build();
        operator = userRepository.save(operator);

        NewMatchedOrderEntity match = NewMatchedOrderEntity.builder()
                .order(order)
                .operator(operator)
                .operatorStatus(MatchedOrderStatus.PENDING)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match);

        mockMvc.perform(get("/api/operators/getOperatorsInfo/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void givenNonExistentOrderId_whenGetOperatorsInfo_thenReturnsNotFound() throws Exception {
        UUID nonExistentOrderId = UUID.randomUUID();

        mockMvc.perform(get("/api/operators/getOperatorsInfo/{orderId}", nonExistentOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenOrderBelongingToAnotherUser_whenGetOperatorsInfo_thenReturnsUnauthorized() throws Exception {
        UserEntity anotherUser = UserEntity.builder()
                .displayName("anotherClient")
                .name("Another")
                .surname("Client")
                .email("another.client@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("999888777")
                .role(UserRole.CLIENT)
                .build();
        anotherUser = userRepository.save(anotherUser);

        ServicesEntity service = servicesRepository.findById("Aerial Photography").orElseThrow();
        OrdersEntity order = OrdersEntity.builder()
                .title("Private Order")
                .description("This is someone else's order")
                .user(anotherUser)
                .service(service)
                .coordinates("52.2700,21.0600")
                .status(OrderStatus.OPEN)
                .createdAt(java.time.LocalDateTime.now())
                .fromDate(java.time.LocalDateTime.now().plusDays(1))
                .toDate(java.time.LocalDateTime.now().plusDays(2))
                .build();
        order = ordersRepository.save(order);

        mockMvc.perform(get("/api/operators/getOperatorsInfo/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenNoAuthToken_whenGetOperatorsInfo_thenReturnsUnauthorized() throws Exception {
        ServicesEntity service = servicesRepository.findById("Aerial Photography").orElseThrow();

        OrdersEntity order = OrdersEntity.builder()
                .title("Test Order")
                .description("Test description")
                .user(testUser)
                .service(service)
                .coordinates("52.2800,21.0700")
                .status(OrderStatus.OPEN)
                .createdAt(java.time.LocalDateTime.now())
                .fromDate(java.time.LocalDateTime.now().plusDays(1))
                .toDate(java.time.LocalDateTime.now().plusDays(2))
                .build();
        order = ordersRepository.save(order);

        mockMvc.perform(get("/api/operators/getOperatorsInfo/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenOperatorWithMatchedOrders_whenGetMatchedOrders_thenReturnsPagedMatchedOrders() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("52.2297,21.0122");
        testUser.setRadius(50);
        userRepository.save(testUser);

        ServicesEntity service = servicesRepository.findById("Aerial Photography").orElseThrow();

        UserEntity client = UserEntity.builder()
                .displayName("client1")
                .name("Client")
                .surname("User")
                .email("client@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("111222333")
                .role(UserRole.CLIENT)
                .build();
        client = userRepository.save(client);

        OrdersEntity order1 = OrdersEntity.builder()
                .title("Real Estate Photography")
                .description("Need aerial photos of property")
                .user(client)
                .service(service)
                .coordinates("52.2350,21.0200")
                .status(OrderStatus.OPEN)
                .createdAt(java.time.LocalDateTime.now())
                .fromDate(java.time.LocalDateTime.now().plusDays(1))
                .toDate(java.time.LocalDateTime.now().plusDays(2))
                .build();
        order1 = ordersRepository.save(order1);

        OrdersEntity order2 = OrdersEntity.builder()
                .title("Wedding Photography")
                .description("Aerial shots for wedding")
                .user(client)
                .service(service)
                .coordinates("52.2400,21.0300")
                .status(OrderStatus.OPEN)
                .createdAt(java.time.LocalDateTime.now())
                .fromDate(java.time.LocalDateTime.now().plusDays(3))
                .toDate(java.time.LocalDateTime.now().plusDays(4))
                .build();
        order2 = ordersRepository.save(order2);

        NewMatchedOrderEntity match1 = NewMatchedOrderEntity.builder()
                .order(order1)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match1);

        NewMatchedOrderEntity match2 = NewMatchedOrderEntity.builder()
                .order(order2)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.PENDING)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match2);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].client_id").exists())
                .andExpect(jsonPath("$.content[0].title").exists())
                .andExpect(jsonPath("$.content[0].description").exists())
                .andExpect(jsonPath("$.content[0].service").value("Aerial Photography"))
                .andExpect(jsonPath("$.content[0].coordinates").exists())
                .andExpect(jsonPath("$.content[0].distance").exists())
                .andExpect(jsonPath("$.content[0].from_date").exists())
                .andExpect(jsonPath("$.content[0].to_date").exists())
                .andExpect(jsonPath("$.content[0].created_at").exists())
                .andExpect(jsonPath("$.content[0].order_status").exists())
                .andExpect(jsonPath("$.content[0].client_status").exists())
                .andExpect(jsonPath("$.content[0].operator_status").exists())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(1))
                .andExpect(jsonPath("$.page.size").value(20));
    }

    @Test
    void givenOperatorWithNoMatchedOrders_whenGetMatchedOrders_thenReturnsEmptyPage() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("52.2297,21.0122");
        testUser.setRadius(50);
        userRepository.save(testUser);

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$.page.totalPages").value(0));
    }

    @Test
    void givenMatchedOrdersWithServiceFilter_whenGetMatchedOrders_thenReturnsFilteredResults() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("52.2297,21.0122");
        testUser.setRadius(100);
        userRepository.save(testUser);

        ServicesEntity aerialPhotography = servicesRepository.findById("Aerial Photography").orElseThrow();
        ServicesEntity surveying = servicesRepository.findById("Surveying").orElseThrow();

        UserEntity client = UserEntity.builder()
                .displayName("client1")
                .name("Client")
                .surname("User")
                .email("client@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("111222333")
                .role(UserRole.CLIENT)
                .build();
        client = userRepository.save(client);

        OrdersEntity order1 = OrdersEntity.builder()
                .title("Photography Order")
                .description("Photo description")
                .user(client)
                .service(aerialPhotography)
                .coordinates("52.2350,21.0200")
                .status(OrderStatus.OPEN)
                .createdAt(java.time.LocalDateTime.now())
                .fromDate(java.time.LocalDateTime.now().plusDays(1))
                .toDate(java.time.LocalDateTime.now().plusDays(2))
                .build();
        order1 = ordersRepository.save(order1);

        OrdersEntity order2 = OrdersEntity.builder()
                .title("Surveying Order")
                .description("Survey description")
                .user(client)
                .service(surveying)
                .coordinates("52.2400,21.0300")
                .status(OrderStatus.OPEN)
                .createdAt(java.time.LocalDateTime.now())
                .fromDate(java.time.LocalDateTime.now().plusDays(3))
                .toDate(java.time.LocalDateTime.now().plusDays(4))
                .build();
        order2 = ordersRepository.save(order2);

        NewMatchedOrderEntity match1 = NewMatchedOrderEntity.builder()
                .order(order1)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match1);

        NewMatchedOrderEntity match2 = NewMatchedOrderEntity.builder()
                .order(order2)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match2);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .param("service", "Aerial Photography")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].service").value("Aerial Photography"))
                .andExpect(jsonPath("$.content[0].title").value("Photography Order"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void givenMatchedOrdersWithOrderStatusFilter_whenGetMatchedOrders_thenReturnsFilteredResults() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("52.2297,21.0122");
        testUser.setRadius(100);
        userRepository.save(testUser);

        ServicesEntity service = servicesRepository.findById("Aerial Photography").orElseThrow();

        UserEntity client = UserEntity.builder()
                .displayName("client1")
                .name("Client")
                .surname("User")
                .email("client@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("111222333")
                .role(UserRole.CLIENT)
                .build();
        client = userRepository.save(client);

        OrdersEntity openOrder = OrdersEntity.builder()
                .title("Open Order")
                .description("Open description")
                .user(client)
                .service(service)
                .coordinates("52.2350,21.0200")
                .status(OrderStatus.OPEN)
                .createdAt(java.time.LocalDateTime.now())
                .fromDate(java.time.LocalDateTime.now().plusDays(1))
                .toDate(java.time.LocalDateTime.now().plusDays(2))
                .build();
        openOrder = ordersRepository.save(openOrder);

        OrdersEntity closedOrder = OrdersEntity.builder()
                .title("Closed Order")
                .description("Closed description")
                .user(client)
                .service(service)
                .coordinates("52.2400,21.0300")
                .status(OrderStatus.COMPLETED)
                .createdAt(java.time.LocalDateTime.now())
                .fromDate(java.time.LocalDateTime.now().plusDays(3))
                .toDate(java.time.LocalDateTime.now().plusDays(4))
                .build();
        closedOrder = ordersRepository.save(closedOrder);

        NewMatchedOrderEntity match1 = NewMatchedOrderEntity.builder()
                .order(openOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match1);

        NewMatchedOrderEntity match2 = NewMatchedOrderEntity.builder()
                .order(closedOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.ACCEPTED)
                .build();
        newMatchedOrdersRepository.save(match2);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .param("order_status", "OPEN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Open Order"))
                .andExpect(jsonPath("$.content[0].order_status").value("OPEN"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void givenMatchedOrdersWithPagination_whenGetMatchedOrders_thenReturnsCorrectPage() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("52.2297,21.0122");
        testUser.setRadius(100);
        userRepository.save(testUser);

        ServicesEntity service = servicesRepository.findById("Aerial Photography").orElseThrow();

        UserEntity client = UserEntity.builder()
                .displayName("client1")
                .name("Client")
                .surname("User")
                .email("client@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("111222333")
                .role(UserRole.CLIENT)
                .build();
        client = userRepository.save(client);

        for (int i = 1; i <= 5; i++) {
            OrdersEntity order = OrdersEntity.builder()
                    .title("Order " + i)
                    .description("Description " + i)
                    .user(client)
                    .service(service)
                    .coordinates("52.2350,21.0200")
                    .status(OrderStatus.OPEN)
                    .createdAt(java.time.LocalDateTime.now())
                    .fromDate(java.time.LocalDateTime.now().plusDays(i))
                    .toDate(java.time.LocalDateTime.now().plusDays(i + 1))
                    .build();
            order = ordersRepository.save(order);

            NewMatchedOrderEntity match = NewMatchedOrderEntity.builder()
                    .order(order)
                    .operator(testUser)
                    .operatorStatus(MatchedOrderStatus.ACCEPTED)
                    .clientStatus(MatchedOrderStatus.PENDING)
                    .build();
            newMatchedOrdersRepository.save(match);
        }

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .param("page", "0")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.page.totalPages").value(3))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    void givenMatchedOrdersWithCustomLocationAndRadius_whenGetMatchedOrders_thenReturnsFilteredByLocation() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("52.2297,21.0122");
        testUser.setRadius(50);
        userRepository.save(testUser);

        ServicesEntity service = servicesRepository.findById("Aerial Photography").orElseThrow();

        UserEntity client = UserEntity.builder()
                .displayName("client1")
                .name("Client")
                .surname("User")
                .email("client@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("111222333")
                .role(UserRole.CLIENT)
                .build();
        client = userRepository.save(client);

        OrdersEntity nearbyOrder = OrdersEntity.builder()
                .title("Nearby Order")
                .description("Close order")
                .user(client)
                .service(service)
                .coordinates("52.2320,21.0150")
                .status(OrderStatus.OPEN)
                .createdAt(java.time.LocalDateTime.now())
                .fromDate(java.time.LocalDateTime.now().plusDays(1))
                .toDate(java.time.LocalDateTime.now().plusDays(2))
                .build();
        nearbyOrder = ordersRepository.save(nearbyOrder);

        OrdersEntity farOrder = OrdersEntity.builder()
                .title("Far Order")
                .description("Distant order")
                .user(client)
                .service(service)
                .coordinates("50.0000,20.0000")
                .status(OrderStatus.OPEN)
                .createdAt(java.time.LocalDateTime.now())
                .fromDate(java.time.LocalDateTime.now().plusDays(3))
                .toDate(java.time.LocalDateTime.now().plusDays(4))
                .build();
        farOrder = ordersRepository.save(farOrder);

        NewMatchedOrderEntity match1 = NewMatchedOrderEntity.builder()
                .order(nearbyOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match1);

        NewMatchedOrderEntity match2 = NewMatchedOrderEntity.builder()
                .order(farOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match2);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .param("location", "52.2297,21.0122")
                        .param("radius", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Nearby Order"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void givenNonOperatorUser_whenGetMatchedOrders_thenReturnsForbidden() throws Exception {
        testUser.setRole(UserRole.CLIENT);
        userRepository.save(testUser);

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenNoAuthToken_whenGetMatchedOrders_thenReturnsUnauthorized() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("52.2297,21.0122");
        testUser.setRadius(50);
        userRepository.save(testUser);

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenInvalidToken_whenGetMatchedOrders_thenReturnsUnauthorized() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("52.2297,21.0122");
        testUser.setRadius(50);
        userRepository.save(testUser);

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .header("X-USER-TOKEN", "Bearer invalid.token.here")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenMatchedOrdersFromMultipleOperators_whenGetMatchedOrders_thenReturnsOnlyOwnMatches() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("52.2297,21.0122");
        testUser.setRadius(100);
        userRepository.save(testUser);

        UserEntity anotherOperator = UserEntity.builder()
                .displayName("operator2")
                .name("Another")
                .surname("Operator")
                .email("another.operator@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("999888777")
                .role(UserRole.OPERATOR)
                .coordinates("52.2297,21.0122")
                .radius(100)
                .build();
        anotherOperator = userRepository.save(anotherOperator);

        ServicesEntity service = servicesRepository.findById("Aerial Photography").orElseThrow();

        UserEntity client = UserEntity.builder()
                .displayName("client1")
                .name("Client")
                .surname("User")
                .email("client@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("111222333")
                .role(UserRole.CLIENT)
                .build();
        client = userRepository.save(client);

        OrdersEntity order1 = OrdersEntity.builder()
                .title("Order for testUser")
                .description("Description 1")
                .user(client)
                .service(service)
                .coordinates("52.2350,21.0200")
                .status(OrderStatus.OPEN)
                .createdAt(java.time.LocalDateTime.now())
                .fromDate(java.time.LocalDateTime.now().plusDays(1))
                .toDate(java.time.LocalDateTime.now().plusDays(2))
                .build();
        order1 = ordersRepository.save(order1);

        OrdersEntity order2 = OrdersEntity.builder()
                .title("Order for anotherOperator")
                .description("Description 2")
                .user(client)
                .service(service)
                .coordinates("52.2400,21.0300")
                .status(OrderStatus.OPEN)
                .createdAt(java.time.LocalDateTime.now())
                .fromDate(java.time.LocalDateTime.now().plusDays(3))
                .toDate(java.time.LocalDateTime.now().plusDays(4))
                .build();
        order2 = ordersRepository.save(order2);

        NewMatchedOrderEntity match1 = NewMatchedOrderEntity.builder()
                .order(order1)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match1);

        NewMatchedOrderEntity match2 = NewMatchedOrderEntity.builder()
                .order(order2)
                .operator(anotherOperator)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match2);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Order for testUser"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void givenMatchedOrdersWithFromDateFilter_whenGetMatchedOrders_thenReturnsOrdersAfterDate() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("52.2297,21.0122");
        testUser.setRadius(100);
        userRepository.save(testUser);

        ServicesEntity service = servicesRepository.findById("Aerial Photography").orElseThrow();

        UserEntity client = UserEntity.builder()
                .displayName("client1")
                .name("Client")
                .surname("User")
                .email("client@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("111222333")
                .role(UserRole.CLIENT)
                .build();
        client = userRepository.save(client);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime filterDate = now.plusDays(5);

        OrdersEntity earlyOrder = OrdersEntity.builder()
                .title("Early Order")
                .description("Early description")
                .user(client)
                .service(service)
                .coordinates("52.2350,21.0200")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(2))
                .toDate(now.plusDays(4))
                .build();
        earlyOrder = ordersRepository.save(earlyOrder);

        OrdersEntity lateOrder = OrdersEntity.builder()
                .title("Late Order")
                .description("Late description")
                .user(client)
                .service(service)
                .coordinates("52.2400,21.0300")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(7))
                .toDate(now.plusDays(9))
                .build();
        lateOrder = ordersRepository.save(lateOrder);

        OrdersEntity exactOrder = OrdersEntity.builder()
                .title("Exact Order")
                .description("Exact description")
                .user(client)
                .service(service)
                .coordinates("52.2380,21.0250")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(filterDate)
                .toDate(now.plusDays(11))
                .build();
        exactOrder = ordersRepository.save(exactOrder);

        NewMatchedOrderEntity match1 = NewMatchedOrderEntity.builder()
                .order(earlyOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match1);

        NewMatchedOrderEntity match2 = NewMatchedOrderEntity.builder()
                .order(lateOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match2);

        NewMatchedOrderEntity match3 = NewMatchedOrderEntity.builder()
                .order(exactOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match3);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .param("from_date", filterDate.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void givenMatchedOrdersWithToDateFilter_whenGetMatchedOrders_thenReturnsOrdersBeforeDate() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("52.2297,21.0122");
        testUser.setRadius(100);
        userRepository.save(testUser);

        ServicesEntity service = servicesRepository.findById("Aerial Photography").orElseThrow();

        UserEntity client = UserEntity.builder()
                .displayName("client1")
                .name("Client")
                .surname("User")
                .email("client@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("111222333")
                .role(UserRole.CLIENT)
                .build();
        client = userRepository.save(client);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime filterDate = now.plusDays(6);

        OrdersEntity earlyOrder = OrdersEntity.builder()
                .title("Early Ending Order")
                .description("Early ending description")
                .user(client)
                .service(service)
                .coordinates("52.2350,21.0200")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(1))
                .toDate(now.plusDays(3))
                .build();
        earlyOrder = ordersRepository.save(earlyOrder);

        OrdersEntity lateOrder = OrdersEntity.builder()
                .title("Late Ending Order")
                .description("Late ending description")
                .user(client)
                .service(service)
                .coordinates("52.2400,21.0300")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(5))
                .toDate(now.plusDays(10))
                .build();
        lateOrder = ordersRepository.save(lateOrder);

        OrdersEntity exactOrder = OrdersEntity.builder()
                .title("Exact Ending Order")
                .description("Exact ending description")
                .user(client)
                .service(service)
                .coordinates("52.2380,21.0250")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(4))
                .toDate(filterDate)
                .build();
        exactOrder = ordersRepository.save(exactOrder);

        NewMatchedOrderEntity match1 = NewMatchedOrderEntity.builder()
                .order(earlyOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match1);

        NewMatchedOrderEntity match2 = NewMatchedOrderEntity.builder()
                .order(lateOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match2);

        NewMatchedOrderEntity match3 = NewMatchedOrderEntity.builder()
                .order(exactOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match3);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .param("to_date", filterDate.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void givenMatchedOrdersWithDateRangeFilter_whenGetMatchedOrders_thenReturnsOrdersWithinRange() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("52.2297,21.0122");
        testUser.setRadius(100);
        userRepository.save(testUser);

        ServicesEntity service = servicesRepository.findById("Aerial Photography").orElseThrow();

        UserEntity client = UserEntity.builder()
                .displayName("client1")
                .name("Client")
                .surname("User")
                .email("client@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("111222333")
                .role(UserRole.CLIENT)
                .build();
        client = userRepository.save(client);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime fromFilter = now.plusDays(5);
        java.time.LocalDateTime toFilter = now.plusDays(10);

        OrdersEntity beforeOrder = OrdersEntity.builder()
                .title("Before Range Order")
                .description("Before range description")
                .user(client)
                .service(service)
                .coordinates("52.2350,21.0200")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(1))
                .toDate(now.plusDays(3))
                .build();
        beforeOrder = ordersRepository.save(beforeOrder);

        OrdersEntity withinOrder = OrdersEntity.builder()
                .title("Within Range Order")
                .description("Within range description")
                .user(client)
                .service(service)
                .coordinates("52.2400,21.0300")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(6))
                .toDate(now.plusDays(8))
                .build();
        withinOrder = ordersRepository.save(withinOrder);

        OrdersEntity afterOrder = OrdersEntity.builder()
                .title("After Range Order")
                .description("After range description")
                .user(client)
                .service(service)
                .coordinates("52.2380,21.0250")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(12))
                .toDate(now.plusDays(15))
                .build();
        afterOrder = ordersRepository.save(afterOrder);

        NewMatchedOrderEntity match1 = NewMatchedOrderEntity.builder()
                .order(beforeOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match1);

        NewMatchedOrderEntity match2 = NewMatchedOrderEntity.builder()
                .order(withinOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match2);

        NewMatchedOrderEntity match3 = NewMatchedOrderEntity.builder()
                .order(afterOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match3);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .param("from_date", fromFilter.toString())
                        .param("to_date", toFilter.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Within Range Order"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void givenMatchedOrdersWithClientStatusFilter_whenGetMatchedOrders_thenReturnsFilteredByClientStatus() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("52.2297,21.0122");
        testUser.setRadius(100);
        userRepository.save(testUser);

        ServicesEntity service = servicesRepository.findById("Aerial Photography").orElseThrow();

        UserEntity client = UserEntity.builder()
                .displayName("client1")
                .name("Client")
                .surname("User")
                .email("client@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("111222333")
                .role(UserRole.CLIENT)
                .build();
        client = userRepository.save(client);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        OrdersEntity pendingOrder = OrdersEntity.builder()
                .title("Client Pending Order")
                .description("Client pending description")
                .user(client)
                .service(service)
                .coordinates("52.2350,21.0200")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(1))
                .toDate(now.plusDays(3))
                .build();
        pendingOrder = ordersRepository.save(pendingOrder);

        OrdersEntity acceptedOrder = OrdersEntity.builder()
                .title("Client Accepted Order")
                .description("Client accepted description")
                .user(client)
                .service(service)
                .coordinates("52.2400,21.0300")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(4))
                .toDate(now.plusDays(6))
                .build();
        acceptedOrder = ordersRepository.save(acceptedOrder);

        OrdersEntity rejectedOrder = OrdersEntity.builder()
                .title("Client Rejected Order")
                .description("Client rejected description")
                .user(client)
                .service(service)
                .coordinates("52.2380,21.0250")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(7))
                .toDate(now.plusDays(9))
                .build();
        rejectedOrder = ordersRepository.save(rejectedOrder);

        NewMatchedOrderEntity match1 = NewMatchedOrderEntity.builder()
                .order(pendingOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match1);

        NewMatchedOrderEntity match2 = NewMatchedOrderEntity.builder()
                .order(acceptedOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.ACCEPTED)
                .build();
        newMatchedOrdersRepository.save(match2);

        NewMatchedOrderEntity match3 = NewMatchedOrderEntity.builder()
                .order(rejectedOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.REJECTED)
                .build();
        newMatchedOrdersRepository.save(match3);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .param("client_status", "PENDING")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Client Pending Order"))
                .andExpect(jsonPath("$.content[0].client_status").value("PENDING"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .param("client_status", "ACCEPTED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Client Accepted Order"))
                .andExpect(jsonPath("$.content[0].client_status").value("ACCEPTED"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .param("client_status", "REJECTED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Client Rejected Order"))
                .andExpect(jsonPath("$.content[0].client_status").value("REJECTED"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void givenMatchedOrdersWithOperatorStatusFilter_whenGetMatchedOrders_thenReturnsFilteredByOperatorStatus() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("52.2297,21.0122");
        testUser.setRadius(100);
        userRepository.save(testUser);

        ServicesEntity service = servicesRepository.findById("Aerial Photography").orElseThrow();

        UserEntity client = UserEntity.builder()
                .displayName("client1")
                .name("Client")
                .surname("User")
                .email("client@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("111222333")
                .role(UserRole.CLIENT)
                .build();
        client = userRepository.save(client);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        OrdersEntity pendingOrder = OrdersEntity.builder()
                .title("Operator Pending Order")
                .description("Operator pending description")
                .user(client)
                .service(service)
                .coordinates("52.2350,21.0200")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(1))
                .toDate(now.plusDays(3))
                .build();
        pendingOrder = ordersRepository.save(pendingOrder);

        OrdersEntity acceptedOrder = OrdersEntity.builder()
                .title("Operator Accepted Order")
                .description("Operator accepted description")
                .user(client)
                .service(service)
                .coordinates("52.2400,21.0300")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(4))
                .toDate(now.plusDays(6))
                .build();
        acceptedOrder = ordersRepository.save(acceptedOrder);

        OrdersEntity rejectedOrder = OrdersEntity.builder()
                .title("Operator Rejected Order")
                .description("Operator rejected description")
                .user(client)
                .service(service)
                .coordinates("52.2380,21.0250")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(7))
                .toDate(now.plusDays(9))
                .build();
        rejectedOrder = ordersRepository.save(rejectedOrder);

        NewMatchedOrderEntity match1 = NewMatchedOrderEntity.builder()
                .order(pendingOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.PENDING)
                .clientStatus(MatchedOrderStatus.ACCEPTED)
                .build();
        newMatchedOrdersRepository.save(match1);

        NewMatchedOrderEntity match2 = NewMatchedOrderEntity.builder()
                .order(acceptedOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.ACCEPTED)
                .build();
        newMatchedOrdersRepository.save(match2);

        NewMatchedOrderEntity match3 = NewMatchedOrderEntity.builder()
                .order(rejectedOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.REJECTED)
                .clientStatus(MatchedOrderStatus.ACCEPTED)
                .build();
        newMatchedOrdersRepository.save(match3);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .param("operator_status", "PENDING")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Operator Pending Order"))
                .andExpect(jsonPath("$.content[0].operator_status").value("PENDING"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .param("operator_status", "ACCEPTED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Operator Accepted Order"))
                .andExpect(jsonPath("$.content[0].operator_status").value("ACCEPTED"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .param("operator_status", "REJECTED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Operator Rejected Order"))
                .andExpect(jsonPath("$.content[0].operator_status").value("REJECTED"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void givenMatchedOrdersWithMultipleFilters_whenGetMatchedOrders_thenReturnsFilteredByCombinedCriteria() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("52.2297,21.0122");
        testUser.setRadius(100);
        userRepository.save(testUser);

        ServicesEntity aerialPhotography = servicesRepository.findById("Aerial Photography").orElseThrow();
        ServicesEntity surveying = servicesRepository.findById("Surveying").orElseThrow();

        UserEntity client = UserEntity.builder()
                .displayName("client1")
                .name("Client")
                .surname("User")
                .email("client@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("111222333")
                .role(UserRole.CLIENT)
                .build();
        client = userRepository.save(client);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime fromFilter = now.plusDays(5);
        java.time.LocalDateTime toFilter = now.plusDays(10);

        OrdersEntity matchingOrder = OrdersEntity.builder()
                .title("Matching Order")
                .description("Matches all filters")
                .user(client)
                .service(aerialPhotography)
                .coordinates("52.2350,21.0200")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(6))
                .toDate(now.plusDays(8))
                .build();
        matchingOrder = ordersRepository.save(matchingOrder);

        OrdersEntity wrongServiceOrder = OrdersEntity.builder()
                .title("Wrong Service Order")
                .description("Wrong service")
                .user(client)
                .service(surveying)
                .coordinates("52.2400,21.0300")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(6))
                .toDate(now.plusDays(8))
                .build();
        wrongServiceOrder = ordersRepository.save(wrongServiceOrder);

        OrdersEntity wrongDateOrder = OrdersEntity.builder()
                .title("Wrong Date Order")
                .description("Wrong date range")
                .user(client)
                .service(aerialPhotography)
                .coordinates("52.2380,21.0250")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(1))
                .toDate(now.plusDays(3))
                .build();
        wrongDateOrder = ordersRepository.save(wrongDateOrder);

        OrdersEntity wrongClientStatusOrder = OrdersEntity.builder()
                .title("Wrong Client Status Order")
                .description("Wrong client status")
                .user(client)
                .service(aerialPhotography)
                .coordinates("52.2360,21.0220")
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .fromDate(now.plusDays(6))
                .toDate(now.plusDays(8))
                .build();
        wrongClientStatusOrder = ordersRepository.save(wrongClientStatusOrder);

        NewMatchedOrderEntity match1 = NewMatchedOrderEntity.builder()
                .order(matchingOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match1);

        NewMatchedOrderEntity match2 = NewMatchedOrderEntity.builder()
                .order(wrongServiceOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match2);

        NewMatchedOrderEntity match3 = NewMatchedOrderEntity.builder()
                .order(wrongDateOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();
        newMatchedOrdersRepository.save(match3);

        NewMatchedOrderEntity match4 = NewMatchedOrderEntity.builder()
                .order(wrongClientStatusOrder)
                .operator(testUser)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.REJECTED)
                .build();
        newMatchedOrdersRepository.save(match4);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/operators/getMatchedOrders")
                        .param("service", "Aerial Photography")
                        .param("from_date", fromFilter.toString())
                        .param("to_date", toFilter.toString())
                        .param("client_status", "PENDING")
                        .param("operator_status", "ACCEPTED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Matching Order"))
                .andExpect(jsonPath("$.content[0].service").value("Aerial Photography"))
                .andExpect(jsonPath("$.content[0].client_status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].operator_status").value("ACCEPTED"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

}
