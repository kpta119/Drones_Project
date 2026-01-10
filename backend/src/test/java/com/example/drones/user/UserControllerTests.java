package com.example.drones.user;

import com.example.drones.auth.exceptions.InvalidCredentialsException;
import com.example.drones.common.config.auth.JwtService;
import com.example.drones.user.dto.UserResponse;
import com.example.drones.user.dto.UserUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTests {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserController userController;

    private UserResponse mockUserResponse;
    private UserUpdateRequest mockUpdateRequest;
    private UUID userId;

    @BeforeEach
    public void setUp() {
        userId = UUID.randomUUID();
        mockUserResponse = UserResponse.builder()
                .username("testUser")
                .name("Jan")
                .surname("Kowalski")
                .email("jan@example.com")
                .role("CLIENT")
                .phoneNumber("123456789")
                .build();


        mockUpdateRequest = UserUpdateRequest.builder()
                .name("Janusz")
                .phoneNumber("987654321")
                .build();
    }


    @Test
    public void givenUserId_whenGetUserData_thenReturnsOkAndUser() {
        when(userService.getUserData(userId)).thenReturn(mockUserResponse);
        ResponseEntity<UserResponse> response = userController.getUserData(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockUserResponse.getUsername(), response.getBody().getUsername());
        verify(userService).getUserData(userId);
    }

    @Test
    public void givenNullUserId_whenGetUserData_thenReturnsOkAndCurrentUser() {
        UUID currentUserId = UUID.randomUUID();

        when(jwtService.extractUserId()).thenReturn(currentUserId);
        when(userService.getUserData(currentUserId)).thenReturn(mockUserResponse);

        ResponseEntity<UserResponse> response = userController.getUserData(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockUserResponse, response.getBody());
        verify(userService).getUserData(currentUserId);
    }

    @Test
    public void givenNonExistentUser_whenGetUserData_thenThrowsException() {
        when(userService.getUserData(userId)).thenThrow(new RuntimeException("User not found"));
        RuntimeException exception = assertThrows(RuntimeException.class, () -> userController.getUserData(userId));

        assertEquals("User not found", exception.getMessage());
        verify(userService).getUserData(userId);
    }


    @Test
    public void givenValidUpdateRequest_whenEditUserData_thenReturnsUpdatedUser() {
        when(userService.editUserData(mockUpdateRequest)).thenReturn(mockUserResponse);

        UserResponse response = userController.editUserData(mockUpdateRequest);

        assertNotNull(response);
        assertEquals(mockUserResponse.getName(), response.getName());
        verify(userService).editUserData(mockUpdateRequest);
    }

    @Test
    public void givenTakenEmail_whenEditUserData_thenThrowsException() {
        doThrow(new InvalidCredentialsException()).when(userService).editUserData(mockUpdateRequest);

        RuntimeException exception = assertThrows(InvalidCredentialsException.class, () -> userController.editUserData(mockUpdateRequest));

        assertEquals("The provided credentials are invalid.", exception.getMessage());
        verify(userService).editUserData(mockUpdateRequest);
    }

    @Test
    public void givenAdminUser_whenEditRole_thenReturnsUserWithNewRole() {
        UserUpdateRequest roleChangeRequest = UserUpdateRequest.builder()
                .role(UserRole.ADMIN)
                .build();

        UserResponse responseWithNewRole = UserResponse.builder()
                .username("testUser")
                .role("ADMIN")
                .build();

        when(userService.editUserData(roleChangeRequest)).thenReturn(responseWithNewRole);
        UserResponse result = userController.editUserData(roleChangeRequest);

        assertNotNull(result);
        assertEquals("ADMIN", result.getRole());
        verify(userService).editUserData(roleChangeRequest);
    }

    @Test
    public void givenClientUser_whenEditRole_thenThrowsAccessDeniedException() {
        UserUpdateRequest roleChangeRequest = UserUpdateRequest.builder()
                .role(UserRole.ADMIN)
                .build();

        when(userService.editUserData(roleChangeRequest))
                .thenThrow(new InvalidCredentialsException());

        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class, () -> userController.editUserData(roleChangeRequest));

        assertEquals("The provided credentials are invalid.", exception.getMessage());
        verify(userService).editUserData(roleChangeRequest);
    }
}