package com.example.drones.operators;

import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.operators.dto.CreateOperatorProfileDto;
import com.example.drones.operators.dto.CreatePortfolioDto;
import com.example.drones.operators.dto.OperatorPortfolioDto;
import com.example.drones.operators.dto.OperatorProfileDto;
import com.example.drones.operators.exceptions.NoSuchOperatorException;
import com.example.drones.operators.exceptions.NoSuchPortfolioException;
import com.example.drones.operators.exceptions.OperatorAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OperatorsControllerTests {

    @Mock
    private OperatorsService operatorsService;

    @InjectMocks
    private OperatorsController operatorsController;

    private CreateOperatorProfileDto validCreateOperatorDto;
    private OperatorProfileDto operatorProfileDto;
    private CreatePortfolioDto validCreatePortfolioDto;
    private OperatorPortfolioDto operatorPortfolioDto;

    @BeforeEach
    public void setUp() {
        validCreateOperatorDto = CreateOperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of("UAV License"))
                .services(List.of("Aerial Photography"))
                .build();

        operatorProfileDto = OperatorProfileDto.builder()
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of("UAV License"))
                .services(List.of("Aerial Photography"))
                .build();

        validCreatePortfolioDto = CreatePortfolioDto.builder()
                .title("Aerial Photography Portfolio")
                .description("Collection of my best aerial photography work")
                .build();

        operatorPortfolioDto = OperatorPortfolioDto.builder()
                .title("Aerial Photography Portfolio")
                .description("Collection of my best aerial photography work")
                .photos(List.of())
                .build();
    }

    @Test
    public void givenValidOperatorDto_whenCreateOperatorProfile_thenReturnsCreated() {
        CreateOperatorProfileDto request = validCreateOperatorDto;
        when(operatorsService.createProfile(request)).thenReturn(operatorProfileDto);

        ResponseEntity<OperatorProfileDto> response = operatorsController.createOperatorProfile(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(this.operatorProfileDto);
        verify(operatorsService).createProfile(request);
    }

    @Test
    public void givenUserNotFound_whenCreateOperatorProfile_thenThrowsUserNotFoundException() {
        CreateOperatorProfileDto request = validCreateOperatorDto;
        when(operatorsService.createProfile(request)).thenThrow(new UserNotFoundException());

        assertThatThrownBy(() -> operatorsController.createOperatorProfile(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
        verify(operatorsService).createProfile(request);
    }

    @Test
    public void givenUserAlreadyOperator_whenCreateOperatorProfile_thenThrowsOperatorAlreadyExistsException() {
        CreateOperatorProfileDto request = validCreateOperatorDto;
        when(operatorsService.createProfile(request)).thenThrow(new OperatorAlreadyExistsException());

        assertThatThrownBy(() -> operatorsController.createOperatorProfile(request))
                .isInstanceOf(OperatorAlreadyExistsException.class)
                .hasMessage("Operator profile already exists for this user.");
        verify(operatorsService).createProfile(request);
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
        when(operatorsService.editProfile(request)).thenReturn(updatedResponse);

        ResponseEntity<OperatorProfileDto> response = operatorsController.editOperatorProfile(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo(updatedResponse);
        verify(operatorsService).editProfile(request);
    }

    @Test
    public void givenUserNotFound_whenEditOperatorProfile_thenThrowsUserNotFoundException() {
        OperatorProfileDto request = operatorProfileDto;
        when(operatorsService.editProfile(request)).thenThrow(new UserNotFoundException());

        assertThatThrownBy(() -> operatorsController.editOperatorProfile(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
        verify(operatorsService).editProfile(request);
    }

    @Test
    public void givenUserNotOperator_whenEditOperatorProfile_thenThrowsNoSuchOperatorException() {
        OperatorProfileDto request = operatorProfileDto;
        when(operatorsService.editProfile(request)).thenThrow(new NoSuchOperatorException());

        assertThatThrownBy(() -> operatorsController.editOperatorProfile(request))
                .isInstanceOf(NoSuchOperatorException.class)
                .hasMessage("No operator profile found for this user.");
        verify(operatorsService).editProfile(request);
    }

    @Test
    public void givenValidPortfolioDto_whenAddPortfolio_thenReturnsCreated() {
        CreatePortfolioDto request = validCreatePortfolioDto;
        OperatorPortfolioDto response = operatorPortfolioDto;
        when(operatorsService.createPortfolio(request)).thenReturn(response);

        ResponseEntity<OperatorPortfolioDto> result = operatorsController.addPortfolio(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(operatorsService).createPortfolio(request);
    }

    @Test
    public void givenUserNotFound_whenAddPortfolio_thenThrowsUserNotFoundException() {
        CreatePortfolioDto request = validCreatePortfolioDto;
        when(operatorsService.createPortfolio(request)).thenThrow(new UserNotFoundException());

        assertThatThrownBy(() -> operatorsController.addPortfolio(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
        verify(operatorsService).createPortfolio(request);
    }

    @Test
    public void givenUserNotOperator_whenAddPortfolio_thenThrowsNoSuchOperatorException() {
        CreatePortfolioDto request = validCreatePortfolioDto;
        when(operatorsService.createPortfolio(request)).thenThrow(new NoSuchOperatorException());

        assertThatThrownBy(() -> operatorsController.addPortfolio(request))
                .isInstanceOf(NoSuchOperatorException.class)
                .hasMessage("No operator profile found for this user.");
        verify(operatorsService).createPortfolio(request);
    }

    @Test
    public void givenValidPortfolioDto_whenEditPortfolio_thenReturnsAccepted() {
        OperatorPortfolioDto request = operatorPortfolioDto;
        OperatorPortfolioDto updatedResponse = OperatorPortfolioDto.builder()
                .title("Updated Portfolio Title")
                .description("Updated description")
                .photos(List.of())
                .build();
        when(operatorsService.editPortfolio(request)).thenReturn(updatedResponse);

        ResponseEntity<OperatorPortfolioDto> result = operatorsController.editPortfolio(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(result.getBody()).isEqualTo(updatedResponse);
        verify(operatorsService).editPortfolio(request);
    }

    @Test
    public void givenUserNotFound_whenEditPortfolio_thenThrowsUserNotFoundException() {
        OperatorPortfolioDto request = operatorPortfolioDto;
        when(operatorsService.editPortfolio(request)).thenThrow(new UserNotFoundException());

        assertThatThrownBy(() -> operatorsController.editPortfolio(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
        verify(operatorsService).editPortfolio(request);
    }

    @Test
    public void givenUserNotOperator_whenEditPortfolio_thenThrowsNoSuchOperatorException() {
        OperatorPortfolioDto request = operatorPortfolioDto;
        when(operatorsService.editPortfolio(request)).thenThrow(new NoSuchOperatorException());

        assertThatThrownBy(() -> operatorsController.editPortfolio(request))
                .isInstanceOf(NoSuchOperatorException.class)
                .hasMessage("No operator profile found for this user.");
        verify(operatorsService).editPortfolio(request);
    }

    @Test
    public void givenNoPortfolioExists_whenEditPortfolio_thenThrowsNoSuchPortfolioException() {
        OperatorPortfolioDto request = operatorPortfolioDto;
        when(operatorsService.editPortfolio(request)).thenThrow(new NoSuchPortfolioException());

        assertThatThrownBy(() -> operatorsController.editPortfolio(request))
                .isInstanceOf(NoSuchPortfolioException.class)
                .hasMessage("Operator portfolio not found");
        verify(operatorsService).editPortfolio(request);
    }
}
