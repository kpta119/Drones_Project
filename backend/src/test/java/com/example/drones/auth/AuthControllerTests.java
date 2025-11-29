package com.example.drones.auth;

import com.example.drones.auth.dto.LoginRequest;
import com.example.drones.auth.dto.LoginResponse;
import com.example.drones.auth.dto.RegisterRequest;
import com.example.drones.config.JwtAuthenticationFilter;
import com.example.drones.config.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(SpringExtension.class)
public class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void givenValidRegisterRequest_whenRegister_thenReturnsCreated() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .displayName("testUser")
                .password("password123")
                .name("Test")
                .surname("User")
                .email("user123@gmail.com")
                .phoneNumber("1234567890")
                .build();
        doNothing().when(authService).register(request);

        ResultActions response = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        response.andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    public void givenInvalidRegisterRequest_whenRegister_thenReturnsBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .displayName(null)
                .password("pwd")
                .name("Test")
                .surname("User")
                .email("invalid-email")
                .phoneNumber("1234567890")
                .build();

        ResultActions response = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        response.andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void givenValidLoginRequest_whenLogin_thenReturnsOk() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("user@gmail.com")
                .password("password123")
                .build();

        String expectedToken = "mock";
        when(authService.login(request)).thenReturn(new LoginResponse(expectedToken));

        ResultActions response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        response.andExpect(MockMvcResultMatchers.status().isOk());
        response.andExpect(MockMvcResultMatchers.jsonPath("$.token").value(expectedToken));
    }

    @Test
    public void givenInvalidLoginRequest_whenLogin_thenReturnsBadRequest() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("invalid-email")
                .password(null)
                .build();

        ResultActions response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        response.andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

}
