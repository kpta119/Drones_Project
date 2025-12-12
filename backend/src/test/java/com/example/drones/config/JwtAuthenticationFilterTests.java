package com.example.drones.config;

import com.example.drones.common.config.auth.CustomUserDetailsService;
import com.example.drones.common.config.auth.JwtAuthenticationFilter;
import com.example.drones.common.config.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTests {

    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService customUserDetailsService;
    @Mock
    private HandlerExceptionResolver handlerExceptionResolver;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UUID testUserId;
    private String validToken;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        testUserId = UUID.randomUUID();
        validToken = "valid.jwt.token";

        userDetails = new User(
                testUserId.toString(),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenNoAuthHeader_whenDoFilterInternal_thenContinuesFilterChain() throws ServletException, IOException {
        when(request.getHeader("X-USER-TOKEN")).thenReturn(null);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUserId(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void givenInvalidAuthHeaderPrefix_whenDoFilterInternal_thenContinuesFilterChain() throws ServletException, IOException {
        when(request.getHeader("X-USER-TOKEN")).thenReturn("InvalidPrefix token");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUserId(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void givenValidToken_whenDoFilterInternal_thenSetsAuthentication() throws ServletException, IOException {
        when(request.getHeader("X-USER-TOKEN")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUserId(validToken)).thenReturn(testUserId);
        when(customUserDetailsService.loadUserById(testUserId)).thenReturn(userDetails);
        when(jwtService.isTokenValid(validToken)).thenReturn(true);
        when(jwtService.refreshTokenIfNeeded(validToken)).thenReturn(validToken);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(userDetails);
    }

    @Test
    void givenInvalidToken_whenDoFilterInternal_thenDoesNotSetAuthentication() throws ServletException, IOException {
        when(request.getHeader("X-USER-TOKEN")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUserId(validToken)).thenReturn(testUserId);
        when(customUserDetailsService.loadUserById(testUserId)).thenReturn(userDetails);
        when(jwtService.isTokenValid(validToken)).thenReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).refreshTokenIfNeeded(any());
    }

    @Test
    void givenTokenWithNullUserId_whenDoFilterInternal_thenDoesNotSetAuthentication() throws ServletException, IOException {
        when(request.getHeader("X-USER-TOKEN")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUserId(validToken)).thenReturn(null);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(customUserDetailsService, never()).loadUserById(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void givenAuthenticationAlreadyExists_whenDoFilterInternal_thenDoesNotOverrideAuthentication() throws ServletException, IOException {
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "existingUser", null, List.of()
                )
        );
        when(request.getHeader("X-USER-TOKEN")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUserId(validToken)).thenReturn(testUserId);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(customUserDetailsService, never()).loadUserById(any());
    }

    @Test
    void givenTokenNeedsRefresh_whenDoFilterInternal_thenSetsRefreshTokenHeader() throws ServletException, IOException {
        String newToken = "new.jwt.token";
        when(request.getHeader("X-USER-TOKEN")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUserId(validToken)).thenReturn(testUserId);
        when(customUserDetailsService.loadUserById(testUserId)).thenReturn(userDetails);
        when(jwtService.isTokenValid(validToken)).thenReturn(true);
        when(jwtService.refreshTokenIfNeeded(validToken)).thenReturn(newToken);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(response).setHeader("X-Refresh-Token", newToken);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void givenTokenDoesNotNeedRefresh_whenDoFilterInternal_thenDoesNotSetRefreshTokenHeader() throws ServletException, IOException {
        when(request.getHeader("X-USER-TOKEN")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUserId(validToken)).thenReturn(testUserId);
        when(customUserDetailsService.loadUserById(testUserId)).thenReturn(userDetails);
        when(jwtService.isTokenValid(validToken)).thenReturn(true);
        when(jwtService.refreshTokenIfNeeded(validToken)).thenReturn(validToken);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(response, never()).setHeader(eq("X-Refresh-Token"), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void givenExceptionDuringProcessing_whenDoFilterInternal_thenHandlesException() throws ServletException, IOException {
        when(request.getHeader("X-USER-TOKEN")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUserId(validToken)).thenThrow(new RuntimeException("JWT parsing error"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(handlerExceptionResolver).resolveException(
                eq(request),
                eq(response),
                eq(null),
                any(RuntimeException.class)
        );
        verify(filterChain, never()).doFilter(request, response);
    }
}
