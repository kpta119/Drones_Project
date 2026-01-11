package com.example.drones.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class OAuth2AuthenticationFailureHandlerTests {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RedirectStrategy redirectStrategy;

    @InjectMocks
    private OAuth2AuthenticationFailureHandler failureHandler;

    private static final String FRONTEND_URL = "http://localhost:3000";

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(failureHandler, "frontendUrl", FRONTEND_URL);
        failureHandler.setRedirectStrategy(redirectStrategy);
    }

    @Test
    public void givenAuthenticationFailure_whenOnAuthenticationFailure_thenRedirectsToLoginWithError() throws IOException {
        AuthenticationException exception = new BadCredentialsException("Invalid credentials");

        failureHandler.onAuthenticationFailure(request, response, exception);

        String expectedUrl = FRONTEND_URL + "/login?error=oauth_failed";
        verify(redirectStrategy).sendRedirect(request, response, expectedUrl);
    }

    @Test
    public void givenDifferentException_whenOnAuthenticationFailure_thenRedirectsToLoginWithError() throws IOException {
        AuthenticationException exception = new AuthenticationException("OAuth error") {
        };

        failureHandler.onAuthenticationFailure(request, response, exception);

        String expectedUrl = FRONTEND_URL + "/login?error=oauth_failed";
        verify(redirectStrategy).sendRedirect(request, response, expectedUrl);
    }
}

