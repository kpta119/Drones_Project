package com.example.drones.operators;

import com.example.drones.common.config.JwtService;
import com.example.drones.operators.dto.CreateOperatorProfileDto;
import com.example.drones.services.OperatorServicesRepository;
import com.example.drones.services.ServicesEntity;
import com.example.drones.services.ServicesRepository;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
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

    @AfterEach
    void tearDown() {
        operatorServicesRepository.deleteAll();
        userRepository.deleteAll();
        servicesRepository.deleteAll();
    }

    private void createService(String serviceName) {
        ServicesEntity service = new ServicesEntity();
        service.setName(serviceName);
        servicesRepository.save(service);
    }

    @Test
    void givenValidOperatorDto_whenCreateOperatorProfile_thenReturnsCreatedOperatorDto() throws Exception {
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
                .andExpect(jsonPath("$.coordinates").value("52.2297,21.0122"))
                .andExpect(jsonPath("$.radius").value(50))
                .andExpect(jsonPath("$.certificates", hasSize(2)))
                .andExpect(jsonPath("$.certificates[0]").value("UAV License"))
                .andExpect(jsonPath("$.certificates[1]").value("Commercial Pilot"))
                .andExpect(jsonPath("$.services", hasSize(2)))
                .andExpect(jsonPath("$.services[0]").value("Aerial Photography"))
                .andExpect(jsonPath("$.services[1]").value("Surveying"));

        UserEntity updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getRole()).isEqualTo(UserRole.OPERATOR);
        assertThat(updatedUser.getCoordinates()).isEqualTo("52.2297,21.0122");
        assertThat(updatedUser.getRadius()).isEqualTo(50);
        assertThat(updatedUser.getCertificates()).containsExactly("UAV License", "Commercial Pilot");

        var operatorServices = operatorServicesRepository.findAll();
        assertThat(operatorServices).hasSize(2);
        assertThat(operatorServices)
                .extracting("serviceName")
                .containsExactlyInAnyOrder("Aerial Photography", "Surveying");
    }

    @Test
    void givenValidOperatorDtoWithNoCertificates_whenCreateOperatorProfile_thenReturnsCreated() throws Exception {
        CreateOperatorProfileDto operatorDto = CreateOperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of())
                .services(List.of("Delivery"))
                .build();

        mockMvc.perform(post("/api/operators/createOperatorProfile")
                        .header("X-USER-TOKEN", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operatorDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coordinates").value("52.2297,21.0122"))
                .andExpect(jsonPath("$.radius").value(50))
                .andExpect(jsonPath("$.certificates").isEmpty())
                .andExpect(jsonPath("$.services[0]").value("Delivery"));

        UserEntity updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getRole()).isEqualTo(UserRole.OPERATOR);
        assertThat(updatedUser.getCertificates()).isEmpty();
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
    void givenServiceNotExistsInDatabase_whenCreateOperatorProfile_thenReturnsInternalServerError() throws Exception {
        CreateOperatorProfileDto operatorDto = CreateOperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of("UAV License"))
                .services(List.of("Non-Existent Service"))
                .build();

        mockMvc.perform(post("/api/operators/createOperatorProfile")
                        .header("X-USER-TOKEN", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operatorDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Internal server error")));

        UserEntity unchangedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(unchangedUser.getRole()).isEqualTo(UserRole.CLIENT);

        var operatorServices = operatorServicesRepository.findAll();
        assertThat(operatorServices).isEmpty();
    }

    @Test
    void givenUserAlreadyOperator_whenCreateOperatorProfile_thenReturnsConflict() throws Exception {
        testUser.setRole(UserRole.OPERATOR);
        testUser.setCoordinates("50.0,20.0");
        testUser.setRadius(30);
        userRepository.save(testUser);

        CreateOperatorProfileDto operatorDto = CreateOperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of("UAV License"))
                .services(List.of("Aerial Photography"))
                .build();

        mockMvc.perform(post("/api/operators/createOperatorProfile")
                        .header("X-USER-TOKEN", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operatorDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Operator profile already exists for this user."));

        UserEntity unchangedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(unchangedUser.getRole()).isEqualTo(UserRole.OPERATOR);
        assertThat(unchangedUser.getCoordinates()).isEqualTo("50.0,20.0");
        assertThat(unchangedUser.getRadius()).isEqualTo(30);
    }


}
