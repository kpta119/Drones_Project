package com.example.drones.operators;

import com.example.drones.common.config.JwtService;
import com.example.drones.operators.dto.CreateOperatorProfileDto;
import com.example.drones.operators.dto.CreatePortfolioDto;
import com.example.drones.operators.dto.OperatorPortfolioDto;
import com.example.drones.operators.dto.OperatorProfileDto;
import com.example.drones.services.OperatorServicesEntity;
import com.example.drones.services.OperatorServicesRepository;
import com.example.drones.services.ServicesEntity;
import com.example.drones.services.ServicesRepository;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                .andExpect(status().isForbidden());

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
    void givenNoAuthToken_whenEditOperatorProfile_thenReturnsForbidden() throws Exception {
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
                .andExpect(status().isForbidden());
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
    void givenNoAuthToken_whenAddPortfolio_thenReturnsForbidden() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        userRepository.save(testUser);

        CreatePortfolioDto portfolioDto = CreatePortfolioDto.builder()
                .title("Aerial Photography Portfolio")
                .description("Collection of my best aerial photography work")
                .build();

        mockMvc.perform(post("/api/operators/addPortfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(portfolioDto)))
                .andExpect(status().isForbidden());

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

        OperatorPortfolioDto updateDto = OperatorPortfolioDto.builder()
                .title("Updated Portfolio Title")
                .description("Updated description with more details")
                .photos(List.of())
                .build();

        mockMvc.perform(patch("/api/operators/editPortfolio")
                        .header("X-USER-TOKEN", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.title").value("Updated Portfolio Title"))
                .andExpect(jsonPath("$.description").value("Updated description with more details"))
                .andExpect(jsonPath("$.photos").isArray());

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

        OperatorPortfolioDto updateDto = OperatorPortfolioDto.builder()
                .title("New Title Only")
                .description(null)
                .photos(List.of())
                .build();

        mockMvc.perform(patch("/api/operators/editPortfolio")
                        .header("X-USER-TOKEN", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.title").value("New Title Only"))
                .andExpect(jsonPath("$.description").value("Original description"));

        var portfolios = portfolioRepository.findAll();
        assertThat(portfolios).hasSize(1);
        assertThat(portfolios.getFirst().getTitle()).isEqualTo("New Title Only");
        assertThat(portfolios.getFirst().getDescription()).isEqualTo("Original description");
    }

    @Test
    void givenNoAuthToken_whenEditPortfolio_thenReturnsForbidden() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        userRepository.save(testUser);

        PortfolioEntity portfolio = PortfolioEntity.builder()
                .operator(testUser)
                .title("Original Portfolio Title")
                .description("Original description")
                .build();
        portfolioRepository.save(portfolio);

        OperatorPortfolioDto updateDto = OperatorPortfolioDto.builder()
                .title("Updated Title")
                .description("Updated description")
                .photos(List.of())
                .build();

        mockMvc.perform(patch("/api/operators/editPortfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());

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

        OperatorPortfolioDto updateDto = OperatorPortfolioDto.builder()
                .title("Updated Title")
                .description("Updated description")
                .photos(List.of())
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
                .andExpect(jsonPath("$.portfolio.description").value("Portfolio description"));
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
    void givenOperatorWithMultipleServices_whenGetOperatorProfile_thenReturnsAllServices() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("48.8566,2.3522");
        testUser.setRadius(75);
        testUser.setCertificates(List.of("Advanced UAV License", "Night Flight Certification", "Commercial Drone Pilot"));
        userRepository.save(testUser);

        addServiceToOperator(testUser, "Aerial Photography");
        addServiceToOperator(testUser, "Surveying");
        addServiceToOperator(testUser, "Delivery");
        addServiceToOperator(testUser, "Inspection");

        PortfolioEntity portfolio = PortfolioEntity.builder()
                .operator(testUser)
                .title("Professional Drone Services")
                .description("Comprehensive drone services portfolio")
                .build();
        portfolio = portfolioRepository.save(portfolio);
        testUser.setPortfolio(portfolio);
        userRepository.save(testUser);

        mockMvc.perform(get("/api/operators/getOperatorProfile/{userId}", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test"))
                .andExpect(jsonPath("$.surname").value("Operator"))
                .andExpect(jsonPath("$.certificates.length()").value(3))
                .andExpect(jsonPath("$.operator_services.length()").value(4))
                .andExpect(jsonPath("$.portfolio").exists())
                .andExpect(jsonPath("$.portfolio.title").value("Professional Drone Services"));
    }
}
