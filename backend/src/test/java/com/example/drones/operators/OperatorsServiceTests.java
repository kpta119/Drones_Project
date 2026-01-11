package com.example.drones.operators;

import com.example.drones.auth.exceptions.InvalidCredentialsException;
import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.operators.dto.*;
import com.example.drones.operators.exceptions.NoSuchOperatorException;
import com.example.drones.operators.exceptions.NoSuchPortfolioException;
import com.example.drones.operators.exceptions.PortfolioAlreadyExistsException;
import com.example.drones.orders.*;
import com.example.drones.orders.exceptions.OrderNotFoundException;
import com.example.drones.services.OperatorServicesService;
import com.example.drones.services.ServicesEntity;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserMapper;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OperatorsServiceTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private OperatorServicesService operatorServicesService;
    @Mock
    private UserMapper operatorMapper;
    @Mock
    private PortfolioMapper portfolioMapper;
    @Mock
    private OrdersRepository ordersRepository;
    @Mock
    private NewMatchedOrdersRepository newMatchedOrdersRepository;
    @Mock
    private OrdersMapper ordersMapper;

    @InjectMocks
    private OperatorsService service;

    @Test
    public void givenValidOperatorDto_whenCreateProfile_thenProfileCreatedAndReturnsDto() {
        UUID userId = UUID.randomUUID();
        CreateOperatorProfileDto operatorDto = CreateOperatorProfileDto.builder()
                .coordinates("45.0,90.0")
                .radius(10)
                .certificates(List.of("Cert1", "Cert2"))
                .services(List.of("Delivery", "Surveillance"))
                .build();
        OperatorProfileDto expectedDto = OperatorProfileDto.builder()
                .coordinates("45.0,90.0")
                .radius(10)
                .certificates(List.of("Cert1", "Cert2"))
                .services(List.of("Delivery", "Surveillance"))
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.CLIENT)
                .build();


        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);
        when(operatorServicesService.addOperatorServices(any(UserEntity.class), anyList()))
                .thenReturn(operatorDto.services());
        when(operatorMapper.toOperatorProfileDto(any(UserEntity.class), anyList()))
                .thenReturn(expectedDto);

        OperatorProfileDto result = service.createProfile(userId, operatorDto);

        verify(userRepository).findById(userId);
        verify(userRepository).save(any(UserEntity.class));
        verify(operatorServicesService).addOperatorServices(any(UserEntity.class), eq(operatorDto.services()));
        verify(operatorMapper).toOperatorProfileDto(any(UserEntity.class), eq(operatorDto.services()));

        assertThat(user.getRole()).isEqualTo(UserRole.OPERATOR);
        assertThat(user.getCoordinates()).isEqualTo(operatorDto.coordinates());
        assertThat(user.getRadius()).isEqualTo(operatorDto.radius());
        assertThat(user.getCertificates()).isEqualTo(operatorDto.certificates());
        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    public void givenOperatorDtoWithNoCertificates_whenCreateProfile_thenProfileCreatedWithNullCertificates() {
        UUID userId = UUID.randomUUID();
        CreateOperatorProfileDto operatorDto = CreateOperatorProfileDto.builder()
                .coordinates("45.0,90.0")
                .radius(10)
                .certificates(List.of())
                .services(List.of("Delivery"))
                .build();
        OperatorProfileDto expectedDto = OperatorProfileDto.builder()
                .coordinates("45.0,90.0")
                .radius(10)
                .certificates(List.of())
                .services(List.of("Delivery"))
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.CLIENT)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);
        when(operatorServicesService.addOperatorServices(any(UserEntity.class), anyList()))
                .thenReturn(operatorDto.services());
        when(operatorMapper.toOperatorProfileDto(any(UserEntity.class), anyList()))
                .thenReturn(expectedDto);

        OperatorProfileDto result = service.createProfile(userId, operatorDto);

        assertThat(user.getCertificates()).isEmpty();
        assertThat(result).isEqualTo(expectedDto);
        verify(userRepository).save(any(UserEntity.class));
        verify(operatorServicesService).addOperatorServices(any(UserEntity.class), eq(operatorDto.services()));
        verify(operatorMapper).toOperatorProfileDto(any(UserEntity.class), eq(operatorDto.services()));
    }

    @Test
    public void givenUserNotFound_whenCreateProfile_thenThrowsUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        CreateOperatorProfileDto operatorDto = CreateOperatorProfileDto.builder()
                .coordinates("45.0,90.0")
                .radius(10)
                .certificates(List.of("Cert1"))
                .services(List.of("Delivery"))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createProfile(userId, operatorDto))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
        verify(operatorServicesService, never()).addOperatorServices(any(), any());
    }

    @Test
    public void givenValidOperatorDto_whenEditProfile_thenProfileUpdatedAndReturnsDto() {
        UUID userId = UUID.randomUUID();
        OperatorProfileDto operatorDto = OperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(100)
                .certificates(List.of("Advanced License", "Night Flight"))
                .services(List.of("Photography", "Mapping"))
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.OPERATOR)
                .coordinates("45.0,90.0")
                .radius(10)
                .certificates(List.of("Basic License"))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);
        when(operatorServicesService.editOperatorServices(any(UserEntity.class), anyList()))
                .thenReturn(operatorDto.services());
        when(operatorMapper.toOperatorProfileDto(any(UserEntity.class), anyList()))
                .thenReturn(operatorDto);

        OperatorProfileDto result = service.editProfile(userId, operatorDto);

        assertThat(user.getCoordinates()).isEqualTo("52.2297,21.0122");
        assertThat(user.getRadius()).isEqualTo(100);
        assertThat(user.getCertificates()).isEqualTo(List.of("Advanced License", "Night Flight"));
        assertThat(result).isEqualTo(operatorDto);
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(UserEntity.class));
        verify(operatorServicesService).editOperatorServices(any(UserEntity.class), eq(operatorDto.services()));
        verify(operatorMapper).toOperatorProfileDto(any(UserEntity.class), eq(operatorDto.services()));
    }

    @Test
    public void givenPartialOperatorDto_whenEditProfile_thenOnlyProvidedFieldsUpdated() {
        UUID userId = UUID.randomUUID();
        OperatorProfileDto operatorDto = OperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(null)
                .certificates(null)
                .services(List.of("Photography"))
                .build();
        OperatorProfileDto expectedDto = OperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(10)
                .certificates(List.of("Basic License"))
                .services(List.of("Photography"))
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.OPERATOR)
                .coordinates("45.0,90.0")
                .radius(10)
                .certificates(List.of("Basic License"))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);
        when(operatorServicesService.editOperatorServices(any(UserEntity.class), anyList()))
                .thenReturn(operatorDto.services());
        when(operatorMapper.toOperatorProfileDto(any(UserEntity.class), anyList()))
                .thenReturn(expectedDto);

        OperatorProfileDto result = service.editProfile(userId, operatorDto);

        assertThat(user.getCoordinates()).isEqualTo("52.2297,21.0122");
        assertThat(user.getRadius()).isEqualTo(10);
        assertThat(user.getCertificates()).isEqualTo(List.of("Basic License")); // unchanged
        assertThat(result).isEqualTo(expectedDto);
        verify(userRepository).save(any(UserEntity.class));
        verify(operatorServicesService).editOperatorServices(any(UserEntity.class), eq(operatorDto.services()));
    }

    @Test
    public void givenOperatorDtoWithNoServices_whenEditProfile_thenExistingServicesRetrieved() {
        UUID userId = UUID.randomUUID();
        OperatorProfileDto operatorDto = OperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(100)
                .certificates(List.of("Advanced License"))
                .services(null)
                .build();
        List<String> existingServices = List.of("Delivery", "Surveillance");
        OperatorProfileDto expectedDto = OperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(100)
                .certificates(List.of("Advanced License"))
                .services(existingServices)
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.OPERATOR)
                .coordinates("45.0,90.0")
                .radius(10)
                .certificates(List.of("Basic License"))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);
        when(operatorServicesService.getOperatorServices(any(UserEntity.class)))
                .thenReturn(existingServices);
        when(operatorMapper.toOperatorProfileDto(any(UserEntity.class), anyList()))
                .thenReturn(expectedDto);

        OperatorProfileDto result = service.editProfile(userId, operatorDto);

        assertThat(result).isEqualTo(expectedDto);
        verify(userRepository).save(any(UserEntity.class));
        verify(operatorServicesService).getOperatorServices(any(UserEntity.class));
        verify(operatorServicesService, never()).editOperatorServices(any(), any());
        verify(operatorMapper).toOperatorProfileDto(any(UserEntity.class), eq(existingServices));
    }

    @Test
    public void givenOperatorDtoWithEmptyServices_whenEditProfile_thenServicesCleared() {
        UUID userId = UUID.randomUUID();
        OperatorProfileDto operatorDto = OperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(100)
                .certificates(List.of("Advanced License"))
                .services(List.of())
                .build();
        OperatorProfileDto expectedDto = OperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(100)
                .certificates(List.of("Advanced License"))
                .services(List.of())
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.OPERATOR)
                .coordinates("45.0,90.0")
                .radius(10)
                .certificates(List.of("Basic License"))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);
        when(operatorServicesService.editOperatorServices(any(UserEntity.class), anyList()))
                .thenReturn(List.of());
        when(operatorMapper.toOperatorProfileDto(any(UserEntity.class), anyList()))
                .thenReturn(expectedDto);

        OperatorProfileDto result = service.editProfile(userId, operatorDto);

        assertThat(result).isEqualTo(expectedDto);
        verify(userRepository).save(any(UserEntity.class));
        verify(operatorServicesService).editOperatorServices(any(UserEntity.class), eq(List.of()));
        verify(operatorMapper).toOperatorProfileDto(any(UserEntity.class), eq(List.of()));
    }

    @Test
    public void givenUserNotFound_whenEditProfile_thenThrowsUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        OperatorProfileDto operatorDto = OperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(100)
                .certificates(List.of("Advanced License"))
                .services(List.of("Photography"))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editProfile(userId, operatorDto))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
        verify(operatorServicesService, never()).editOperatorServices(any(), any());
        verify(operatorServicesService, never()).getOperatorServices(any());
    }

    @Test
    public void givenValidPortfolioDto_whenCreatePortfolio_thenPortfolioCreatedAndReturnsDto() {
        UUID userId = UUID.randomUUID();
        CreatePortfolioDto portfolioDto = CreatePortfolioDto.builder()
                .title("Aerial Photography Portfolio")
                .description("Collection of my best aerial photography work")
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.OPERATOR)
                .build();
        PortfolioEntity savedPortfolio = PortfolioEntity.builder()
                .id(1)
                .operator(user)
                .title(portfolioDto.title())
                .description(portfolioDto.description())
                .build();
        OperatorPortfolioDto expectedDto = OperatorPortfolioDto.builder()
                .title(portfolioDto.title())
                .description(portfolioDto.description())
                .photos(List.of())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(portfolioRepository.save(any(PortfolioEntity.class))).thenReturn(savedPortfolio);
        when(portfolioMapper.toOperatorPortfolioDto(any(PortfolioEntity.class))).thenReturn(expectedDto);

        OperatorPortfolioDto result = service.createPortfolio(userId, portfolioDto);

        verify(userRepository).findById(userId);
        verify(portfolioRepository).save(any(PortfolioEntity.class));
        verify(portfolioMapper).toOperatorPortfolioDto(savedPortfolio);

        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    public void givenUserNotFound_whenCreatePortfolio_thenThrowsUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        CreatePortfolioDto portfolioDto = CreatePortfolioDto.builder()
                .title("Aerial Photography Portfolio")
                .description("Collection of my best aerial photography work")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPortfolio(userId, portfolioDto))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findById(userId);
        verify(portfolioRepository, never()).save(any());
        verify(portfolioMapper, never()).toOperatorPortfolioDto(any());
    }

    @Test
    public void givenOperatorWithPortfolio_whenCreatePortfolio_thenThrowsPortfolioAlreadyExistsException() {
        UUID userId = UUID.randomUUID();
        CreatePortfolioDto portfolioDto = CreatePortfolioDto.builder()
                .title("Aerial Photography Portfolio")
                .description("Collection of my best aerial photography work")
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.OPERATOR)
                .build();
        PortfolioEntity existingPortfolio = PortfolioEntity.builder()
                .id(1)
                .operator(user)
                .title("Existing Portfolio")
                .description("Existing description")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(portfolioRepository.findByOperatorId(userId)).thenReturn(Optional.of(existingPortfolio));

        assertThatThrownBy(() -> service.createPortfolio(userId, portfolioDto))
                .isInstanceOf(PortfolioAlreadyExistsException.class)
                .hasMessage("Portfolio already exists for this operator.");

        verify(userRepository).findById(userId);
        verify(portfolioRepository).findByOperatorId(userId);
        verify(portfolioRepository, never()).save(any());
        verify(portfolioMapper, never()).toOperatorPortfolioDto(any());
    }

    @Test
    public void givenValidPortfolioDto_whenEditPortfolio_thenPortfolioUpdatedAndReturnsDto() {
        UUID userId = UUID.randomUUID();
        UpdatePortfolioDto portfolioDto = UpdatePortfolioDto.builder()
                .title("Updated Portfolio Title")
                .description("Updated description")
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.OPERATOR)
                .build();
        PortfolioEntity existingPortfolio = PortfolioEntity.builder()
                .id(1)
                .operator(user)
                .title("Old Title")
                .description("Old description")
                .build();
        PortfolioEntity savedPortfolio = PortfolioEntity.builder()
                .id(1)
                .operator(user)
                .title(portfolioDto.title())
                .description(portfolioDto.description())
                .build();
        OperatorPortfolioDto expectedDto = OperatorPortfolioDto.builder()
                .title(portfolioDto.title())
                .description(portfolioDto.description())
                .photos(List.of())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(portfolioRepository.findByOperatorId(userId)).thenReturn(Optional.of(existingPortfolio));
        when(portfolioRepository.save(any(PortfolioEntity.class))).thenReturn(savedPortfolio);
        when(portfolioMapper.toOperatorPortfolioDto(any(PortfolioEntity.class))).thenReturn(expectedDto);

        OperatorPortfolioDto result = service.editPortfolio(userId, portfolioDto);

        assertThat(existingPortfolio.getTitle()).isEqualTo("Updated Portfolio Title");
        assertThat(existingPortfolio.getDescription()).isEqualTo("Updated description");
        assertThat(result).isEqualTo(expectedDto);
        verify(userRepository).findById(userId);
        verify(portfolioRepository).findByOperatorId(userId);
        verify(portfolioRepository).save(existingPortfolio);
        verify(portfolioMapper).toOperatorPortfolioDto(savedPortfolio);
    }

    @Test
    public void givenPartialPortfolioDto_whenEditPortfolio_thenOnlyProvidedFieldsUpdated() {
        UUID userId = UUID.randomUUID();
        UpdatePortfolioDto portfolioDto = UpdatePortfolioDto.builder()
                .title("Updated Title Only")
                .description(null)
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.OPERATOR)
                .build();
        PortfolioEntity existingPortfolio = PortfolioEntity.builder()
                .id(1)
                .operator(user)
                .title("Old Title")
                .description("Old description")
                .build();
        OperatorPortfolioDto expectedDto = OperatorPortfolioDto.builder()
                .title("Updated Title Only")
                .description("Old description")
                .photos(List.of())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(portfolioRepository.findByOperatorId(userId)).thenReturn(Optional.of(existingPortfolio));
        when(portfolioRepository.save(any(PortfolioEntity.class))).thenReturn(existingPortfolio);
        when(portfolioMapper.toOperatorPortfolioDto(any(PortfolioEntity.class))).thenReturn(expectedDto);

        OperatorPortfolioDto result = service.editPortfolio(userId, portfolioDto);

        assertThat(existingPortfolio.getTitle()).isEqualTo("Updated Title Only");
        assertThat(existingPortfolio.getDescription()).isEqualTo("Old description");
        assertThat(result).isEqualTo(expectedDto);
        verify(portfolioRepository).save(existingPortfolio);
        verify(portfolioMapper).toOperatorPortfolioDto(existingPortfolio);
    }

    @Test
    public void givenPortfolioDtoWithOnlyDescription_whenEditPortfolio_thenOnlyDescriptionUpdated() {
        UUID userId = UUID.randomUUID();
        UpdatePortfolioDto portfolioDto = UpdatePortfolioDto.builder()
                .title(null)
                .description("Updated description only")
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.OPERATOR)
                .build();
        PortfolioEntity existingPortfolio = PortfolioEntity.builder()
                .id(1)
                .operator(user)
                .title("Old Title")
                .description("Old description")
                .build();
        OperatorPortfolioDto expectedDto = OperatorPortfolioDto.builder()
                .title("Old Title")
                .description("Updated description only")
                .photos(List.of())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(portfolioRepository.findByOperatorId(userId)).thenReturn(Optional.of(existingPortfolio));
        when(portfolioRepository.save(any(PortfolioEntity.class))).thenReturn(existingPortfolio);
        when(portfolioMapper.toOperatorPortfolioDto(any(PortfolioEntity.class))).thenReturn(expectedDto);

        OperatorPortfolioDto result = service.editPortfolio(userId, portfolioDto);

        assertThat(existingPortfolio.getTitle()).isEqualTo("Old Title");
        assertThat(existingPortfolio.getDescription()).isEqualTo("Updated description only");
        assertThat(result).isEqualTo(expectedDto);
        verify(portfolioRepository).save(existingPortfolio);
        verify(portfolioMapper).toOperatorPortfolioDto(existingPortfolio);
    }

    @Test
    public void givenUserNotFound_whenEditPortfolio_thenThrowsUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        UpdatePortfolioDto portfolioDto = UpdatePortfolioDto.builder()
                .title("Updated Title")
                .description("Updated description")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editPortfolio(userId, portfolioDto))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findById(userId);
        verify(portfolioRepository, never()).findByOperatorId(any());
        verify(portfolioRepository, never()).save(any());
        verify(portfolioMapper, never()).toOperatorPortfolioDto(any());
    }

    @Test
    public void givenNoPortfolioExists_whenEditPortfolio_thenThrowsNoSuchPortfolioException() {
        UUID userId = UUID.randomUUID();
        UpdatePortfolioDto portfolioDto = UpdatePortfolioDto.builder()
                .title("Updated Title")
                .description("Updated description")
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.OPERATOR)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(portfolioRepository.findByOperatorId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editPortfolio(userId, portfolioDto))
                .isInstanceOf(NoSuchPortfolioException.class)
                .hasMessage("Operator portfolio not found");

        verify(userRepository).findById(userId);
        verify(portfolioRepository).findByOperatorId(userId);
        verify(portfolioRepository, never()).save(any());
        verify(portfolioMapper, never()).toOperatorPortfolioDto(any());
    }

    @Test
    public void givenValidOperatorWithPortfolio_whenGetOperatorProfile_thenReturnsCompleteOperatorDto() {
        UUID userId = UUID.randomUUID();
        List<String> services = List.of("Aerial Photography", "Surveying");
        PortfolioEntity portfolio = PortfolioEntity.builder()
                .id(1)
                .title("My Portfolio")
                .description("Portfolio description")
                .build();
        UserEntity operator = UserEntity.builder()
                .id(userId)
                .name("John")
                .surname("Doe")
                .displayName("john_doe")
                .email("john@example.com")
                .phoneNumber("+1234567890")
                .role(UserRole.OPERATOR)
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of("UAV License", "Commercial Pilot"))
                .portfolio(portfolio)
                .build();
        OperatorPortfolioDto portfolioDto = OperatorPortfolioDto.builder()
                .title("My Portfolio")
                .description("Portfolio description")
                .photos(List.of())
                .build();
        OperatorDto expectedDto = OperatorDto.builder()
                .name("John")
                .surname("Doe")
                .username("john_doe")
                .email("john@example.com")
                .phoneNumber("+1234567890")
                .certificates(List.of("UAV License", "Commercial Pilot"))
                .operatorServices(services)
                .portfolio(portfolioDto)
                .build();

        when(userRepository.findByIdWithPortfolio(userId)).thenReturn(Optional.of(operator));
        when(operatorServicesService.getOperatorServices(operator)).thenReturn(services);
        when(portfolioMapper.toOperatorPortfolioDto(portfolio)).thenReturn(portfolioDto);
        when(operatorMapper.toOperatorDto(operator, services, portfolioDto)).thenReturn(expectedDto);

        OperatorDto result = service.getOperatorProfile(userId);

        assertThat(result).isEqualTo(expectedDto);
        verify(userRepository).findByIdWithPortfolio(userId);
        verify(operatorServicesService).getOperatorServices(operator);
        verify(portfolioMapper).toOperatorPortfolioDto(portfolio);
        verify(operatorMapper).toOperatorDto(operator, services, portfolioDto);
    }

    @Test
    public void givenValidOperatorWithoutPortfolio_whenGetOperatorProfile_thenReturnsOperatorDtoWithNullPortfolio() {
        UUID userId = UUID.randomUUID();
        List<String> services = List.of("Aerial Photography");
        UserEntity operator = UserEntity.builder()
                .id(userId)
                .name("Jane")
                .surname("Smith")
                .displayName("Naismith")
                .email("jane@example.com")
                .phoneNumber("+9876543210")
                .role(UserRole.OPERATOR)
                .coordinates("40.7128,74.0060")
                .radius(30)
                .certificates(List.of("Basic UAV License"))
                .portfolio(null)
                .build();
        OperatorDto expectedDto = OperatorDto.builder()
                .name("Jane")
                .surname("Smith")
                .username("Naismith")
                .email("jane@example.com")
                .phoneNumber("+9876543210")
                .certificates(List.of("Basic UAV License"))
                .operatorServices(services)
                .portfolio(null)
                .build();

        when(userRepository.findByIdWithPortfolio(userId)).thenReturn(Optional.of(operator));
        when(operatorServicesService.getOperatorServices(operator)).thenReturn(services);
        when(portfolioMapper.toOperatorPortfolioDto(null)).thenReturn(null);
        when(operatorMapper.toOperatorDto(operator, services, null)).thenReturn(expectedDto);

        OperatorDto result = service.getOperatorProfile(userId);

        assertThat(result).isEqualTo(expectedDto);
        assertThat(result.portfolio()).isNull();
        verify(userRepository).findByIdWithPortfolio(userId);
        verify(operatorServicesService).getOperatorServices(operator);
        verify(portfolioMapper).toOperatorPortfolioDto(null);
        verify(operatorMapper).toOperatorDto(operator, services, null);
    }

    @Test
    public void givenUserNotFound_whenGetOperatorProfile_thenThrowsUserNotFoundException() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findByIdWithPortfolio(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOperatorProfile(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findByIdWithPortfolio(userId);
        verify(operatorServicesService, never()).getOperatorServices(any());
        verify(portfolioMapper, never()).toOperatorPortfolioDto(any());
        verify(operatorMapper, never()).toOperatorDto(any(), any(), any());
    }

    @Test
    public void givenUserNotOperator_whenGetOperatorProfile_thenThrowsNoSuchOperatorException() {
        UUID userId = UUID.randomUUID();
        UserEntity clientUser = UserEntity.builder()
                .id(userId)
                .name("Bob")
                .surname("Client")
                .displayName("bob client")
                .email("bob@example.com")
                .role(UserRole.CLIENT)
                .build();

        when(userRepository.findByIdWithPortfolio(userId)).thenReturn(Optional.of(clientUser));

        assertThatThrownBy(() -> service.getOperatorProfile(userId))
                .isInstanceOf(NoSuchOperatorException.class)
                .hasMessage("No operator profile found for this user.");

        verify(userRepository).findByIdWithPortfolio(userId);
        verify(operatorServicesService, never()).getOperatorServices(any());
        verify(portfolioMapper, never()).toOperatorPortfolioDto(any());
        verify(operatorMapper, never()).toOperatorDto(any(), any(), any());
    }

    @Test
    public void givenOperatorWithNoServices_whenGetOperatorProfile_thenReturnsOperatorDtoWithEmptyServices() {
        UUID userId = UUID.randomUUID();
        List<String> emptyServices = List.of();
        UserEntity operator = UserEntity.builder()
                .id(userId)
                .name("Alice")
                .surname("Operator")
                .displayName("Alice")
                .email("alice@example.com")
                .phoneNumber("+1112223333")
                .role(UserRole.OPERATOR)
                .coordinates("51.5074,0.1278")
                .radius(25)
                .certificates(List.of())
                .portfolio(null)
                .build();
        OperatorDto expectedDto = OperatorDto.builder()
                .name("Alice")
                .surname("Operator")
                .username("Alice")
                .email("alice@example.com")
                .phoneNumber("+1112223333")
                .certificates(List.of())
                .operatorServices(emptyServices)
                .portfolio(null)
                .build();

        when(userRepository.findByIdWithPortfolio(userId)).thenReturn(Optional.of(operator));
        when(operatorServicesService.getOperatorServices(operator)).thenReturn(emptyServices);
        when(portfolioMapper.toOperatorPortfolioDto(null)).thenReturn(null);
        when(operatorMapper.toOperatorDto(operator, emptyServices, null)).thenReturn(expectedDto);

        OperatorDto result = service.getOperatorProfile(userId);

        assertThat(result).isEqualTo(expectedDto);
        assertThat(result.operatorServices()).isEmpty();
        verify(userRepository).findByIdWithPortfolio(userId);
        verify(operatorServicesService).getOperatorServices(operator);
        verify(portfolioMapper).toOperatorPortfolioDto(null);
        verify(operatorMapper).toOperatorDto(operator, emptyServices, null);
    }

    @Test
    public void givenValidOrderAndUser_whenGetOperatorInfo_thenReturnsListOfMatchingOperators() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID operator1Id = UUID.randomUUID();
        UUID operator2Id = UUID.randomUUID();

        UserEntity client = UserEntity.builder()
                .id(userId)
                .role(UserRole.CLIENT)
                .build();

        OrdersEntity order = OrdersEntity.builder()
                .id(orderId)
                .user(client)
                .build();

        UserEntity operator1 = UserEntity.builder()
                .id(operator1Id)
                .name("John")
                .surname("Smith")
                .displayName("operator1")
                .role(UserRole.OPERATOR)
                .certificates(List.of("UAV License", "Commercial Pilot"))
                .build();

        UserEntity operator2 = UserEntity.builder()
                .id(operator2Id)
                .name("Jane")
                .surname("Doe")
                .displayName("operator2")
                .role(UserRole.OPERATOR)
                .certificates(List.of("Basic UAV License"))
                .build();

        List<UserEntity> matchedOperators = List.of(operator1, operator2);

        MatchingOperatorDto matchingDto1 = new MatchingOperatorDto(
                operator1Id,
                "operator1",
                "John",
                "Smith",
                List.of("UAV License", "Commercial Pilot")
        );

        MatchingOperatorDto matchingDto2 = new MatchingOperatorDto(
                operator2Id,
                "operator2",
                "Jane",
                "Doe",
                List.of("Basic UAV License")
        );

        when(ordersRepository.findByIdWithUser(orderId)).thenReturn(Optional.of(order));
        when(newMatchedOrdersRepository.findInterestedOperatorByOrderId(orderId)).thenReturn(matchedOperators);
        when(operatorMapper.toMatchingOperatorDto(operator1)).thenReturn(matchingDto1);
        when(operatorMapper.toMatchingOperatorDto(operator2)).thenReturn(matchingDto2);

        List<MatchingOperatorDto> result = service.getOperatorInfo(userId, orderId);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(matchingDto1, matchingDto2);
        verify(ordersRepository).findByIdWithUser(orderId);
        verify(newMatchedOrdersRepository).findInterestedOperatorByOrderId(orderId);
        verify(operatorMapper).toMatchingOperatorDto(operator1);
        verify(operatorMapper).toMatchingOperatorDto(operator2);
    }

    @Test
    public void givenValidOrderWithNoMatchedOperators_whenGetOperatorInfo_thenReturnsEmptyList() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        UserEntity client = UserEntity.builder()
                .id(userId)
                .role(UserRole.CLIENT)
                .build();

        OrdersEntity order = OrdersEntity.builder()
                .id(orderId)
                .user(client)
                .build();

        List<UserEntity> emptyMatchedOperators = List.of();

        when(ordersRepository.findByIdWithUser(orderId)).thenReturn(Optional.of(order));
        when(newMatchedOrdersRepository.findInterestedOperatorByOrderId(orderId)).thenReturn(emptyMatchedOperators);

        List<MatchingOperatorDto> result = service.getOperatorInfo(userId, orderId);

        assertThat(result).isEmpty();
        verify(ordersRepository).findByIdWithUser(orderId);
        verify(newMatchedOrdersRepository).findInterestedOperatorByOrderId(orderId);
        verify(operatorMapper, never()).toMatchingOperatorDto(any());
    }

    @Test
    public void givenOrderNotFound_whenGetOperatorInfo_thenThrowsOrderNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(ordersRepository.findByIdWithUser(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOperatorInfo(userId, orderId))
                .isInstanceOf(OrderNotFoundException.class);

        verify(ordersRepository).findByIdWithUser(orderId);
        verify(newMatchedOrdersRepository, never()).findInterestedOperatorByOrderId(any());
        verify(operatorMapper, never()).toMatchingOperatorDto(any());
    }

    @Test
    public void givenOrderBelongsToAnotherUser_whenGetOperatorInfo_thenThrowsInvalidCredentialsException() {
        UUID userId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        UserEntity anotherClient = UserEntity.builder()
                .id(anotherUserId)
                .role(UserRole.CLIENT)
                .build();

        OrdersEntity order = OrdersEntity.builder()
                .id(orderId)
                .user(anotherClient)
                .build();

        when(ordersRepository.findByIdWithUser(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.getOperatorInfo(userId, orderId))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(ordersRepository).findByIdWithUser(orderId);
        verify(newMatchedOrdersRepository, never()).findInterestedOperatorByOrderId(any());
        verify(operatorMapper, never()).toMatchingOperatorDto(any());
    }

    @Test
    public void givenValidOrderWithSingleOperator_whenGetOperatorInfo_thenReturnsSingleOperator() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();

        UserEntity client = UserEntity.builder()
                .id(userId)
                .role(UserRole.CLIENT)
                .build();

        OrdersEntity order = OrdersEntity.builder()
                .id(orderId)
                .user(client)
                .build();

        UserEntity operator = UserEntity.builder()
                .id(operatorId)
                .name("Alice")
                .surname("Operator")
                .displayName("alice_operator")
                .role(UserRole.OPERATOR)
                .certificates(List.of("Advanced Drone License", "Night Flight Certification"))
                .build();

        List<UserEntity> matchedOperators = List.of(operator);

        MatchingOperatorDto matchingDto = new MatchingOperatorDto(
                operatorId,
                "alice_operator",
                "Alice",
                "Operator",
                List.of("Advanced Drone License", "Night Flight Certification")
        );

        when(ordersRepository.findByIdWithUser(orderId)).thenReturn(Optional.of(order));
        when(newMatchedOrdersRepository.findInterestedOperatorByOrderId(orderId)).thenReturn(matchedOperators);
        when(operatorMapper.toMatchingOperatorDto(operator)).thenReturn(matchingDto);

        List<MatchingOperatorDto> result = service.getOperatorInfo(userId, orderId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(matchingDto);
        assertThat(result.getFirst().userId()).isEqualTo(operatorId);
        assertThat(result.getFirst().displayName()).isEqualTo("alice_operator");
        assertThat(result.getFirst().name()).isEqualTo("Alice");
        assertThat(result.getFirst().surname()).isEqualTo("Operator");
        assertThat(result.getFirst().certificates()).hasSize(2);
        verify(ordersRepository).findByIdWithUser(orderId);
        verify(newMatchedOrdersRepository).findInterestedOperatorByOrderId(orderId);
        verify(operatorMapper).toMatchingOperatorDto(operator);
    }

    @Test
    public void givenValidOperatorWithFilters_whenGetMatchedOrders_thenReturnsFilteredOrders() {
        UUID operatorId = UUID.randomUUID();
        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        UserEntity operator = UserEntity.builder()
                .id(operatorId)
                .role(UserRole.OPERATOR)
                .coordinates("52.2297,21.0122")
                .radius(50)
                .build();

        ServicesEntity serviceEntity = new ServicesEntity("Aerial Photography");

        UserEntity client = UserEntity.builder()
                .id(clientId)
                .name("Client")
                .surname("User")
                .build();

        NewMatchedOrderEntity matchedOrder1 = NewMatchedOrderEntity.builder()
                .id(1)
                .operator(operator)
                .operatorStatus(MatchedOrderStatus.PENDING)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();

        NewMatchedOrderEntity matchedOrder2 = NewMatchedOrderEntity.builder()
                .id(2)
                .operator(operator)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .clientStatus(MatchedOrderStatus.PENDING)
                .build();

        OrdersEntity order1 = OrdersEntity.builder()
                .id(orderId1)
                .title("Order 1")
                .description("Description 1")
                .service(serviceEntity)
                .coordinates("52.2300,21.0130")
                .status(OrderStatus.OPEN)
                .user(client)
                .matchedOrders(List.of(matchedOrder1))
                .build();

        OrdersEntity order2 = OrdersEntity.builder()
                .id(orderId2)
                .title("Order 2")
                .description("Description 2")
                .service(serviceEntity)
                .coordinates("52.2400,21.0200")
                .status(OrderStatus.OPEN)
                .user(client)
                .matchedOrders(List.of(matchedOrder2))
                .build();

        MatchedOrdersFilters filters = new MatchedOrdersFilters(
                null, null, "Aerial Photography", null, null, OrderStatus.OPEN, null, null
        );

        Pageable pageable = PageRequest.of(0, 20);
        Page<OrdersEntity> ordersPage =
                new PageImpl<>(List.of(order1, order2), pageable, 2);

        MatchedOrderDto dto1 = MatchedOrderDto.builder()
                .id(orderId1)
                .clientId(clientId)
                .title("Order 1")
                .service("Aerial Photography")
                .coordinates("52.2300,21.0130")
                .distance(0.08)
                .orderStatus(OrderStatus.OPEN)
                .clientStatus(MatchedOrderStatus.PENDING)
                .operatorStatus(MatchedOrderStatus.PENDING)
                .build();

        MatchedOrderDto dto2 = MatchedOrderDto.builder()
                .id(orderId2)
                .clientId(clientId)
                .title("Order 2")
                .service("Aerial Photography")
                .coordinates("52.2400,21.0200")
                .distance(1.38)
                .orderStatus(OrderStatus.OPEN)
                .clientStatus(MatchedOrderStatus.PENDING)
                .operatorStatus(MatchedOrderStatus.ACCEPTED)
                .build();

        when(userRepository.findByIdWithPortfolio(operatorId)).thenReturn(Optional.of(operator));
        when(ordersRepository.findAll(ArgumentMatchers.<Specification<OrdersEntity>>any(), eq(pageable)))
                .thenReturn(ordersPage);
        when(ordersMapper.toMatchedOrderDto(eq(order1), eq(matchedOrder1), any(Double.class))).thenReturn(dto1);
        when(ordersMapper.toMatchedOrderDto(eq(order2), eq(matchedOrder2), any(Double.class))).thenReturn(dto2);

        org.springframework.data.domain.Page<MatchedOrderDto> result = this.service.getMatchedOrders(operatorId, filters, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).containsExactly(dto1, dto2);
        verify(userRepository).findByIdWithPortfolio(operatorId);
        verify(ordersRepository).findAll(ArgumentMatchers.<Specification<OrdersEntity>>any(), eq(pageable));
    }

    @Test
    public void givenOperatorWithCustomLocationAndRadius_whenGetMatchedOrders_thenUsesProvidedValues() {
        UUID operatorId = UUID.randomUUID();

        UserEntity operator = UserEntity.builder()
                .id(operatorId)
                .role(UserRole.OPERATOR)
                .coordinates("52.2297,21.0122")
                .radius(50)
                .build();

        MatchedOrdersFilters filters = new MatchedOrdersFilters(
                "50.0614,19.9383", 100, null, null, null, null, null, null
        );

        Pageable pageable = PageRequest.of(0, 20);
        Page<OrdersEntity> emptyPage =
                new PageImpl<>(List.of(), pageable, 0);

        when(userRepository.findByIdWithPortfolio(operatorId)).thenReturn(Optional.of(operator));
        when(ordersRepository.findAll(ArgumentMatchers.<Specification<OrdersEntity>>any(), eq(pageable)))
                .thenReturn(emptyPage);

        Page<MatchedOrderDto> result = service.getMatchedOrders(operatorId, filters, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(userRepository).findByIdWithPortfolio(operatorId);
        verify(ordersRepository).findAll(ArgumentMatchers.<Specification<OrdersEntity>>any(), eq(pageable));
    }

    @Test
    public void givenOperatorWithNullLocationInFilters_whenGetMatchedOrders_thenUsesOperatorLocation() {
        UUID operatorId = UUID.randomUUID();

        UserEntity operator = UserEntity.builder()
                .id(operatorId)
                .role(UserRole.OPERATOR)
                .coordinates("52.2297,21.0122")
                .radius(50)
                .build();

        MatchedOrdersFilters filters = new MatchedOrdersFilters(
                null, null, null, null, null, null, null, null
        );

        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        Page<OrdersEntity> emptyPage =
                new PageImpl<>(List.of(), pageable, 0);

        when(userRepository.findByIdWithPortfolio(operatorId)).thenReturn(Optional.of(operator));
        when(ordersRepository.findAll(ArgumentMatchers.<Specification<OrdersEntity>>any(), eq(pageable)))
                .thenReturn(emptyPage);

        Page<MatchedOrderDto> result = service.getMatchedOrders(operatorId, filters, pageable);

        assertThat(result).isNotNull();
        verify(userRepository).findByIdWithPortfolio(operatorId);
        verify(ordersRepository).findAll(ArgumentMatchers.<Specification<OrdersEntity>>any(), eq(pageable));
    }

    @Test
    public void givenUserNotFound_whenGetMatchedOrders_thenThrowsUserNotFoundException() {
        UUID operatorId = UUID.randomUUID();
        MatchedOrdersFilters filters = new MatchedOrdersFilters(
                null, null, null, null, null, null, null, null
        );
        Pageable pageable = PageRequest.of(0, 20);

        when(userRepository.findByIdWithPortfolio(operatorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMatchedOrders(operatorId, filters, pageable))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findByIdWithPortfolio(operatorId);
        verify(ordersRepository, never()).findAll(ArgumentMatchers.<Specification<OrdersEntity>>any(), any(Pageable.class));
    }

    @Test
    public void givenEmptyOrdersList_whenGetMatchedOrders_thenReturnsEmptyPage() {
        UUID operatorId = UUID.randomUUID();

        UserEntity operator = UserEntity.builder()
                .id(operatorId)
                .role(UserRole.OPERATOR)
                .coordinates("52.2297,21.0122")
                .radius(50)
                .build();

        MatchedOrdersFilters filters = new MatchedOrdersFilters(
                null, null, null, null, null, null, null, null
        );

        Pageable pageable = PageRequest.of(0, 20);
        Page<OrdersEntity> emptyPage =
                new PageImpl<>(List.of(), pageable, 0);

        when(userRepository.findByIdWithPortfolio(operatorId)).thenReturn(Optional.of(operator));
        when(ordersRepository.findAll(ArgumentMatchers.<Specification<OrdersEntity>>any(), eq(pageable)))
                .thenReturn(emptyPage);

        Page<MatchedOrderDto> result = service.getMatchedOrders(operatorId, filters, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(userRepository).findByIdWithPortfolio(operatorId);
        verify(ordersRepository).findAll(ArgumentMatchers.<Specification<OrdersEntity>>any(), eq(pageable));
    }

    @Test
    public void givenValidCoordinates_whenCalculateDistance_thenReturnsCorrectDistance() throws Exception {
        String location1 = "52.2297,21.0122";
        String location2 = "50.0614,19.9383";

        Method method = OperatorsService.class.getDeclaredMethod(
                "calculateDistance", String.class, String.class);
        method.setAccessible(true);

        Double distance = (Double) method.invoke(service, location1, location2);

        assertThat(distance).isNotNull();
        assertThat(distance).isGreaterThan(250.0);
        assertThat(distance).isLessThan(260.0);
    }

    @Test
    public void givenSameCoordinates_whenCalculateDistance_thenReturnsZero() throws Exception {
        String location1 = "52.2297,21.0122";
        String location2 = "52.2297,21.0122";

        Method method = OperatorsService.class.getDeclaredMethod(
                "calculateDistance", String.class, String.class);
        method.setAccessible(true);

        Double distance = (Double) method.invoke(service, location1, location2);

        assertThat(distance).isNotNull();
        assertThat(distance.isNaN() || distance < 0.1).isTrue();
    }

    @Test
    public void givenNullLocation1_whenCalculateDistance_thenReturnsNull() throws Exception {
        String location2 = "52.2297,21.0122";

        Method method = OperatorsService.class.getDeclaredMethod(
                "calculateDistance", String.class, String.class);
        method.setAccessible(true);

        Double distance = (Double) method.invoke(service, null, location2);

        assertThat(distance).isNull();
    }

    @Test
    public void givenNullLocation2_whenCalculateDistance_thenReturnsNull() throws Exception {
        String location1 = "52.2297,21.0122";

        Method method = OperatorsService.class.getDeclaredMethod(
                "calculateDistance", String.class, String.class);
        method.setAccessible(true);

        Double distance = (Double) method.invoke(service, location1, null);

        assertThat(distance).isNull();
    }

    @Test
    public void givenInvalidCoordinatesFormat_whenCalculateDistance_thenReturnsNull() throws Exception {
        String location1 = "52.2297";
        String location2 = "52.2297,21.0122";

        Method method = OperatorsService.class.getDeclaredMethod(
                "calculateDistance", String.class, String.class);
        method.setAccessible(true);

        Double distance = (Double) method.invoke(service, location1, location2);

        assertThat(distance).isNull();
    }

    @Test
    public void givenEmptyCoordinates_whenCalculateDistance_thenReturnsNull() throws Exception {
        String location1 = "";
        String location2 = "52.2297,21.0122";

        Method method = OperatorsService.class.getDeclaredMethod(
                "calculateDistance", String.class, String.class);
        method.setAccessible(true);

        Double distance = (Double) method.invoke(service, location1, location2);

        assertThat(distance).isNull();
    }

    @Test
    public void givenCoordinatesWithSpaces_whenCalculateDistance_thenCalculatesCorrectly() throws Exception {
        String location1 = " 52.2297 , 21.0122 ";
        String location2 = " 50.0614 , 19.9383 ";

        Method method = OperatorsService.class.getDeclaredMethod(
                "calculateDistance", String.class, String.class);
        method.setAccessible(true);

        Double distance = (Double) method.invoke(service, location1, location2);

        assertThat(distance).isNotNull();
        assertThat(distance).isGreaterThan(250.0);
        assertThat(distance).isLessThan(260.0);
    }

    @Test
    public void givenCloseCoordinates_whenCalculateDistance_thenReturnsSmallDistance() throws Exception {
        String location1 = "52.2297,21.0122";
        String location2 = "52.2300,21.0130";

        Method method = OperatorsService.class.getDeclaredMethod(
                "calculateDistance", String.class, String.class);
        method.setAccessible(true);

        Double distance = (Double) method.invoke(service, location1, location2);

        assertThat(distance).isNotNull();
        assertThat(distance).isLessThan(1.0);
    }
}

