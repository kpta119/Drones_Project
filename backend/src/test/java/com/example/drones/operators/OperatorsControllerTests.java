package com.example.drones.operators;

import com.example.drones.common.config.JwtService;
import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.operators.dto.*;
import com.example.drones.operators.exceptions.NoSuchOperatorException;
import com.example.drones.operators.exceptions.NoSuchPortfolioException;
import com.example.drones.operators.exceptions.OperatorAlreadyExistsException;
import com.example.drones.operators.exceptions.PortfolioAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OperatorsControllerTests {

    @Mock
    private OperatorsService operatorsService;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private OperatorsController operatorsController;

    private CreateOperatorProfileDto validCreateOperatorDto;
    private OperatorProfileDto operatorProfileDto;
    private CreatePortfolioDto validCreatePortfolioDto;
    private OperatorPortfolioDto operatorPortfolioDto;
    private UpdatePortfolioDto updatePortfolioDto;

    @BeforeEach
    public void setUp() {
        validCreateOperatorDto = buildValidCreateOperatorDto();
        operatorProfileDto = buildOperatorProfileDto();
        validCreatePortfolioDto = buildValidCreatePortfolioDto();
        operatorPortfolioDto = buildOperatorPortfolioDto();
        updatePortfolioDto = buildValidUpdatePortfolioDto();
    }

    private CreateOperatorProfileDto buildValidCreateOperatorDto() {
        return CreateOperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of("UAV License"))
                .services(List.of("Aerial Photography"))
                .build();
    }

    private OperatorProfileDto buildOperatorProfileDto() {
        return OperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of("UAV License"))
                .services(List.of("Aerial Photography"))
                .build();
    }

    private CreatePortfolioDto buildValidCreatePortfolioDto() {
        return CreatePortfolioDto.builder()
                .title("Aerial Photography Portfolio")
                .description("Collection of my best aerial photography work")
                .build();
    }

    private OperatorPortfolioDto buildOperatorPortfolioDto() {
        return OperatorPortfolioDto.builder()
                .title("Aerial Photography Portfolio")
                .description("Collection of my best aerial photography work")
                .photos(List.of())
                .build();
    }

    private UpdatePortfolioDto buildValidUpdatePortfolioDto() {
        return UpdatePortfolioDto.builder()
                .title("Updated Portfolio Title")
                .description("Updated description")
                .build();
    }

    @Test
    public void givenValidOperatorDto_whenCreateOperatorProfile_thenReturnsCreated() {
        CreateOperatorProfileDto request = validCreateOperatorDto;
        when(jwtService.extractUserId()).thenReturn(UUID.randomUUID());
        when(operatorsService.createProfile(any(UUID.class), eq(request))).thenReturn(operatorProfileDto);

        ResponseEntity<OperatorProfileDto> response = operatorsController.createOperatorProfile(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(this.operatorProfileDto);
        verify(operatorsService).createProfile(any(UUID.class), eq(request));
    }

    @Test
    public void givenUserNotFound_whenCreateOperatorProfile_thenThrowsUserNotFoundException() {
        CreateOperatorProfileDto request = validCreateOperatorDto;
        when(jwtService.extractUserId()).thenReturn(UUID.randomUUID());
        when(operatorsService.createProfile(any(UUID.class), eq(request))).thenThrow(new UserNotFoundException());

        assertThatThrownBy(() -> operatorsController.createOperatorProfile(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
        verify(operatorsService).createProfile(any(UUID.class), eq(request));
    }

    @Test
    public void givenUserAlreadyOperator_whenCreateOperatorProfile_thenThrowsOperatorAlreadyExistsException() {
        CreateOperatorProfileDto request = validCreateOperatorDto;
        when(jwtService.extractUserId()).thenReturn(UUID.randomUUID());
        when(operatorsService.createProfile(any(UUID.class), eq(request))).thenThrow(new OperatorAlreadyExistsException());

        assertThatThrownBy(() -> operatorsController.createOperatorProfile(request))
                .isInstanceOf(OperatorAlreadyExistsException.class)
                .hasMessage("Operator profile already exists for this user.");
        verify(operatorsService).createProfile(any(UUID.class), eq(request));
    }

    @Test
    public void givenValidOperatorDto_whenEditOperatorProfile_thenReturnsAccepted() {
        OperatorProfileDto request = operatorProfileDto;
        OperatorProfileDto updatedResponse = OperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(100)
                .certificates(List.of("UAV License", "Advanced License"))
                .services(List.of("Aerial Photography", "Surveillance"))
                .build();
        when(jwtService.extractUserId()).thenReturn(UUID.randomUUID());
        when(operatorsService.editProfile(any(UUID.class), eq(request))).thenReturn(updatedResponse);

        ResponseEntity<OperatorProfileDto> response = operatorsController.editOperatorProfile(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo(updatedResponse);
        verify(operatorsService).editProfile(any(UUID.class), eq(request));
    }

    @Test
    public void givenUserNotFound_whenEditOperatorProfile_thenThrowsUserNotFoundException() {
        OperatorProfileDto request = operatorProfileDto;
        when(jwtService.extractUserId()).thenReturn(UUID.randomUUID());
        when(operatorsService.editProfile(any(UUID.class), eq(request))).thenThrow(new UserNotFoundException());

        assertThatThrownBy(() -> operatorsController.editOperatorProfile(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
        verify(operatorsService).editProfile(any(UUID.class), eq(request));
    }

    @Test
    public void givenUserNotOperator_whenEditOperatorProfile_thenThrowsNoSuchOperatorException() {
        OperatorProfileDto request = operatorProfileDto;
        when(jwtService.extractUserId()).thenReturn(UUID.randomUUID());
        when(operatorsService.editProfile(any(UUID.class), eq(request))).thenThrow(new NoSuchOperatorException());

        assertThatThrownBy(() -> operatorsController.editOperatorProfile(request))
                .isInstanceOf(NoSuchOperatorException.class)
                .hasMessage("No operator profile found for this user.");
        verify(operatorsService).editProfile(any(UUID.class), eq(request));
    }

    @Test
    public void givenValidPortfolioDto_whenAddPortfolio_thenReturnsCreated() {
        CreatePortfolioDto request = validCreatePortfolioDto;
        OperatorPortfolioDto response = operatorPortfolioDto;
        when(jwtService.extractUserId()).thenReturn(UUID.randomUUID());
        when(operatorsService.createPortfolio(any(UUID.class), eq(request))).thenReturn(response);

        ResponseEntity<OperatorPortfolioDto> result = operatorsController.addPortfolio(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(operatorsService).createPortfolio(any(UUID.class), eq(request));
    }

    @Test
    public void givenUserNotFound_whenAddPortfolio_thenThrowsUserNotFoundException() {
        CreatePortfolioDto request = validCreatePortfolioDto;
        when(jwtService.extractUserId()).thenReturn(UUID.randomUUID());
        when(operatorsService.createPortfolio(any(UUID.class), eq(request))).thenThrow(new UserNotFoundException());

        assertThatThrownBy(() -> operatorsController.addPortfolio(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
        verify(operatorsService).createPortfolio(any(UUID.class), eq(request));
    }

    @Test
    public void givenUserNotOperator_whenAddPortfolio_thenThrowsNoSuchOperatorException() {
        CreatePortfolioDto request = validCreatePortfolioDto;
        when(jwtService.extractUserId()).thenReturn(UUID.randomUUID());
        when(operatorsService.createPortfolio(any(UUID.class), eq(request))).thenThrow(new NoSuchOperatorException());

        assertThatThrownBy(() -> operatorsController.addPortfolio(request))
                .isInstanceOf(NoSuchOperatorException.class)
                .hasMessage("No operator profile found for this user.");
        verify(operatorsService).createPortfolio(any(UUID.class), eq(request));
    }

    @Test
    public void givenOperatorWithPortfolio_whenAddPortfolio_ThenThrowsPortfolioAlreadyExistsException() {
        CreatePortfolioDto request = validCreatePortfolioDto;
        when(jwtService.extractUserId()).thenReturn(UUID.randomUUID());
        when(operatorsService.createPortfolio(any(UUID.class), eq(request))).thenThrow(new PortfolioAlreadyExistsException());

        assertThatThrownBy(() -> operatorsController.addPortfolio(request))
                .isInstanceOf(PortfolioAlreadyExistsException.class)
                .hasMessage("Portfolio already exists for this operator.");
        verify(operatorsService).createPortfolio(any(UUID.class), eq(request));
    }

    @Test
    public void givenValidPortfolioDto_whenEditPortfolio_thenReturnsAccepted() {
        UpdatePortfolioDto request = updatePortfolioDto;
        OperatorPortfolioDto expectedDto = OperatorPortfolioDto.builder()
                .title("Updated Portfolio Title")
                .description("Updated description")
                .photos(List.of())
                .build();

        when(jwtService.extractUserId()).thenReturn(UUID.randomUUID());
        when(operatorsService.editPortfolio(any(UUID.class), eq(request))).thenReturn(expectedDto);

        ResponseEntity<OperatorPortfolioDto> result = operatorsController.editPortfolio(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(result.getBody()).isEqualTo(expectedDto);
        verify(operatorsService).editPortfolio(any(UUID.class), eq(request));
    }

    @Test
    public void givenUserNotFound_whenEditPortfolio_thenThrowsUserNotFoundException() {
        UpdatePortfolioDto request = updatePortfolioDto;
        when(jwtService.extractUserId()).thenReturn(UUID.randomUUID());
        when(operatorsService.editPortfolio(any(UUID.class), eq(request))).thenThrow(new UserNotFoundException());

        assertThatThrownBy(() -> operatorsController.editPortfolio(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
        verify(operatorsService).editPortfolio(any(UUID.class), eq(request));
    }

    @Test
    public void givenUserNotOperator_whenEditPortfolio_thenThrowsNoSuchOperatorException() {
        UpdatePortfolioDto request = updatePortfolioDto;
        when(jwtService.extractUserId()).thenReturn(UUID.randomUUID());
        when(operatorsService.editPortfolio(any(UUID.class), eq(request))).thenThrow(new NoSuchOperatorException());

        assertThatThrownBy(() -> operatorsController.editPortfolio(request))
                .isInstanceOf(NoSuchOperatorException.class)
                .hasMessage("No operator profile found for this user.");
        verify(operatorsService).editPortfolio(any(UUID.class), eq(request));
    }

    @Test
    public void givenNoPortfolioExists_whenEditPortfolio_thenThrowsNoSuchPortfolioException() {
        UpdatePortfolioDto request = updatePortfolioDto;
        when(jwtService.extractUserId()).thenReturn(UUID.randomUUID());
        when(operatorsService.editPortfolio(any(UUID.class), eq(request))).thenThrow(new NoSuchPortfolioException());

        assertThatThrownBy(() -> operatorsController.editPortfolio(request))
                .isInstanceOf(NoSuchPortfolioException.class)
                .hasMessage("Operator portfolio not found");
        verify(operatorsService).editPortfolio(any(UUID.class), eq(request));
    }

    @Test
    public void givenValidUserId_whenGetOperatorProfile_thenReturnsOk() {
        UUID userId = UUID.randomUUID();
        OperatorDto operatorDto = OperatorDto.builder()
                .name("John")
                .surname("Doe")
                .username("john_doe")
                .certificates(List.of("UAV License"))
                .operatorServices(List.of("Aerial Photography"))
                .email("john@example.com")
                .phoneNumber("+1234567890")
                .portfolio(operatorPortfolioDto)
                .build();
        when(operatorsService.getOperatorProfile(userId)).thenReturn(operatorDto);

        ResponseEntity<OperatorDto> response = operatorsController.getOperatorProfile(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(operatorDto);
        verify(operatorsService).getOperatorProfile(userId);
    }

    @Test
    public void givenUserNotFound_whenGetOperatorProfile_thenThrowsUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(operatorsService.getOperatorProfile(userId)).thenThrow(new UserNotFoundException());

        assertThatThrownBy(() -> operatorsController.getOperatorProfile(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
        verify(operatorsService).getOperatorProfile(userId);
    }

    @Test
    public void givenUserNotOperator_whenGetOperatorProfile_thenThrowsNoSuchOperatorException() {
        UUID userId = UUID.randomUUID();
        when(operatorsService.getOperatorProfile(userId)).thenThrow(new NoSuchOperatorException());

        assertThatThrownBy(() -> operatorsController.getOperatorProfile(userId))
                .isInstanceOf(NoSuchOperatorException.class)
                .hasMessage("No operator profile found for this user.");
        verify(operatorsService).getOperatorProfile(userId);
    }
}
