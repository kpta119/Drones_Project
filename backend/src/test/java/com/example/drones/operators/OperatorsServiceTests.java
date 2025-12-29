package com.example.drones.operators;

import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.operators.dto.*;
import com.example.drones.operators.exceptions.NoSuchOperatorException;
import com.example.drones.operators.exceptions.NoSuchPortfolioException;
import com.example.drones.operators.exceptions.OperatorAlreadyExistsException;
import com.example.drones.operators.exceptions.PortfolioAlreadyExistsException;
import com.example.drones.services.OperatorServicesService;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserMapper;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    public void givenUserAlreadyOperator_whenCreateProfile_thenThrowsOperatorAlreadyExistsException() {
        UUID userId = UUID.randomUUID();
        CreateOperatorProfileDto operatorDto = CreateOperatorProfileDto.builder()
                .coordinates("45.0,90.0")
                .radius(10)
                .certificates(List.of("Cert1"))
                .services(List.of("Delivery"))
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.OPERATOR)
                .coordinates("40.0,80.0")
                .radius(5)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.createProfile(userId, operatorDto))
                .isInstanceOf(OperatorAlreadyExistsException.class)
                .hasMessage("Operator profile already exists for this user.");

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
    public void givenUserNotOperator_whenEditProfile_thenThrowsNoSuchOperatorException() {
        UUID userId = UUID.randomUUID();
        OperatorProfileDto operatorDto = OperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(100)
                .certificates(List.of("Advanced License"))
                .services(List.of("Photography"))
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.CLIENT)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.editProfile(userId, operatorDto))
                .isInstanceOf(com.example.drones.operators.exceptions.NoSuchOperatorException.class)
                .hasMessage("No operator profile found for this user.");

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
    public void givenUserNotOperator_whenCreatePortfolio_thenThrowsNoSuchOperatorException() {
        UUID userId = UUID.randomUUID();
        CreatePortfolioDto portfolioDto = CreatePortfolioDto.builder()
                .title("Aerial Photography Portfolio")
                .description("Collection of my best aerial photography work")
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.CLIENT)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.createPortfolio(userId, portfolioDto))
                .isInstanceOf(NoSuchOperatorException.class)
                .hasMessage("No operator profile found for this user.");

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
    public void givenUserNotOperator_whenEditPortfolio_thenThrowsNoSuchOperatorException() {
        UUID userId = UUID.randomUUID();
        UpdatePortfolioDto portfolioDto = UpdatePortfolioDto.builder()
                .title("Updated Title")
                .description("Updated description")
                .build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .role(UserRole.CLIENT)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.editPortfolio(userId, portfolioDto))
                .isInstanceOf(NoSuchOperatorException.class)
                .hasMessage("No operator profile found for this user.");

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
}

