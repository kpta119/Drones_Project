package com.example.drones.operators;

import com.example.drones.operators.dto.OperatorDto;
import com.example.drones.operators.dto.OperatorPortfolioDto;
import com.example.drones.operators.dto.OperatorProfileDto;
import com.example.drones.services.OperatorServicesService;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserMapper;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
public class OperatorsCacheTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private PortfolioRepository portfolioRepository;
    @MockitoBean
    private OperatorServicesService operatorServicesService;
    @MockitoBean
    private UserMapper operatorMapper;
    @MockitoBean
    private PortfolioMapper portfolioMapper;

    @Autowired
    private OperatorsService service;

    @Autowired
    private CacheManager cacheManager;

    private UUID userId;
    private UserEntity operatorUser;
    private OperatorDto operatorDto;
    private PortfolioEntity portfolio;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(cacheName ->
                Objects.requireNonNull(cacheManager.getCache(cacheName)).clear()
        );

        userId = UUID.randomUUID();

        portfolio = new PortfolioEntity();
        portfolio.setId(1);
        portfolio.setTitle("My Portfolio");
        portfolio.setDescription("Portfolio description");

        operatorUser = UserEntity.builder()
                .id(userId)
                .role(UserRole.OPERATOR)
                .displayName("testOperator")
                .name("Test")
                .surname("Operator")
                .email("operator@test.com")
                .phoneNumber("123456789")
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of("UAV License", "Commercial Pilot"))
                .portfolio(portfolio)
                .build();

        operatorDto = OperatorDto.builder()
                .name("Test")
                .surname("Operator")
                .username("testOperator")
                .email("operator@test.com")
                .phoneNumber("123456789")
                .certificates(List.of("UAV License", "Commercial Pilot"))
                .operatorServices(List.of("Aerial Photography", "Surveying"))
                .portfolio(OperatorPortfolioDto.builder()
                        .title("My Portfolio")
                        .description("Portfolio description")
                        .build())
                .build();
    }

    @Test
    public void givenOperatorProfile_whenGetOperatorProfileTwice_thenResultIsCached() {
        when(userRepository.findByIdWithPortfolio(userId)).thenReturn(Optional.of(operatorUser));
        when(operatorServicesService.getOperatorServices(operatorUser))
                .thenReturn(List.of("Aerial Photography", "Surveying"));
        when(portfolioMapper.toOperatorPortfolioDto(portfolio))
                .thenReturn(operatorDto.portfolio());
        when(operatorMapper.toOperatorDto(any(UserEntity.class), anyList(), any()))
                .thenReturn(operatorDto);

        OperatorDto result1 = service.getOperatorProfile(userId);
        OperatorDto result2 = service.getOperatorProfile(userId);

        assertThat(result1).isEqualTo(operatorDto);
        assertThat(result2).isEqualTo(operatorDto);
        verify(userRepository, times(1)).findByIdWithPortfolio(userId);
        verify(operatorServicesService, times(1)).getOperatorServices(operatorUser);
    }

    @Test
    public void givenCachedOperatorProfile_whenEditProfile_thenCacheIsEvicted() {
        when(userRepository.findByIdWithPortfolio(userId)).thenReturn(Optional.of(operatorUser));
        when(operatorServicesService.getOperatorServices(operatorUser))
                .thenReturn(List.of("Aerial Photography", "Surveying"));
        when(portfolioMapper.toOperatorPortfolioDto(portfolio))
                .thenReturn(operatorDto.portfolio());
        when(operatorMapper.toOperatorDto(any(UserEntity.class), anyList(), any()))
                .thenReturn(operatorDto);

        service.getOperatorProfile(userId);

        OperatorProfileDto editDto = OperatorProfileDto.builder()
                .coordinates("50.0,20.0")
                .radius(100)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(operatorUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(operatorUser);
        when(operatorServicesService.getOperatorServices(operatorUser))
                .thenReturn(List.of("Aerial Photography", "Surveying"));
        when(operatorMapper.toOperatorProfileDto(any(UserEntity.class), anyList()))
                .thenReturn(editDto);

        service.editProfile(userId, editDto);

        service.getOperatorProfile(userId);

        verify(userRepository, times(2)).findByIdWithPortfolio(userId);
    }

    @Test
    public void givenCachedOperatorProfile_whenEditPortfolio_thenCacheIsEvicted() {
        when(userRepository.findByIdWithPortfolio(userId)).thenReturn(Optional.of(operatorUser));
        when(operatorServicesService.getOperatorServices(operatorUser))
                .thenReturn(List.of("Aerial Photography", "Surveying"));
        when(portfolioMapper.toOperatorPortfolioDto(portfolio))
                .thenReturn(operatorDto.portfolio());
        when(operatorMapper.toOperatorDto(any(UserEntity.class), anyList(), any()))
                .thenReturn(operatorDto);

        service.getOperatorProfile(userId);

        OperatorPortfolioDto editPortfolioDto = OperatorPortfolioDto.builder()
                .title("Updated Portfolio")
                .description("Updated description")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(operatorUser));
        when(portfolioRepository.findByOperatorId(userId)).thenReturn(Optional.of(portfolio));
        when(portfolioRepository.save(any(PortfolioEntity.class))).thenReturn(portfolio);
        when(portfolioMapper.toOperatorPortfolioDto(portfolio)).thenReturn(editPortfolioDto);

        service.editPortfolio(userId, editPortfolioDto);
        service.getOperatorProfile(userId);

        verify(userRepository, times(2)).findByIdWithPortfolio(userId);
    }

    @Test
    public void givenMultipleOperators_whenGetOperatorProfile_thenEachOperatorIsCachedSeparately() {
        UUID userId2 = UUID.randomUUID();
        UserEntity operatorUser2 = UserEntity.builder()
                .id(userId2)
                .role(UserRole.OPERATOR)
                .displayName("testOperator2")
                .name("Test2")
                .surname("Operator2")
                .email("operator2@test.com")
                .phoneNumber("987654321")
                .coordinates("51.1079,17.0385")
                .radius(30)
                .certificates(List.of("Basic License"))
                .portfolio(portfolio)
                .build();

        OperatorDto operatorDto2 = OperatorDto.builder()
                .name("Test2")
                .surname("Operator2")
                .username("testOperator2")
                .email("operator2@test.com")
                .phoneNumber("987654321")
                .certificates(List.of("Basic License"))
                .operatorServices(List.of("Delivery"))
                .portfolio(operatorDto.portfolio())
                .build();

        when(userRepository.findByIdWithPortfolio(userId)).thenReturn(Optional.of(operatorUser));
        when(userRepository.findByIdWithPortfolio(userId2)).thenReturn(Optional.of(operatorUser2));
        when(operatorServicesService.getOperatorServices(operatorUser))
                .thenReturn(List.of("Aerial Photography", "Surveying"));
        when(operatorServicesService.getOperatorServices(operatorUser2))
                .thenReturn(List.of("Delivery"));
        when(portfolioMapper.toOperatorPortfolioDto(portfolio))
                .thenReturn(operatorDto.portfolio());
        when(operatorMapper.toOperatorDto(eq(operatorUser), anyList(), any()))
                .thenReturn(operatorDto);
        when(operatorMapper.toOperatorDto(eq(operatorUser2), anyList(), any()))
                .thenReturn(operatorDto2);

        OperatorDto result1 = service.getOperatorProfile(userId);
        OperatorDto result2 = service.getOperatorProfile(userId2);
        OperatorDto result3 = service.getOperatorProfile(userId);
        OperatorDto result4 = service.getOperatorProfile(userId2);

        assertThat(result1).isEqualTo(operatorDto);
        assertThat(result2).isEqualTo(operatorDto2);
        assertThat(result3).isEqualTo(operatorDto);
        assertThat(result4).isEqualTo(operatorDto2);

        verify(userRepository, times(1)).findByIdWithPortfolio(userId);
        verify(userRepository, times(1)).findByIdWithPortfolio(userId2);
    }

}

