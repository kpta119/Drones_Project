package com.example.drones.auth;

import com.example.drones.common.config.auth.JwtService;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OAuth2AuthenticationSuccessHandlerTests {
    @Mock
    private JwtService jwtService;
    @Mock
    private OAuth2AuthorizedClientRepository authorizedClientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private OAuth2User oAuth2User;
    @Mock
    private OAuth2AuthenticationToken oAuth2AuthenticationToken;
    @Mock
    private OAuth2AuthorizedClient oAuth2AuthorizedClient;
    @Mock
    private RedirectStrategy redirectStrategy;
    @Captor
    private ArgumentCaptor<Cookie> cookieCaptor;
    @InjectMocks
    private OAuth2AuthenticationSuccessHandler successHandler;
    private static final String FRONTEND_URL = "http://localhost:3000";
    private static final String PROVIDER_ID = "google-123456";
    private static final String EMAIL = "user@example.com";
    private static final String GIVEN_NAME = "John";
    private static final String FAMILY_NAME = "Doe";
    private static final String JWT_TOKEN = "jwt-token-123";
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(successHandler, "frontendUrl", FRONTEND_URL);
        successHandler.setRedirectStrategy(redirectStrategy);
        when(oAuth2AuthenticationToken.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("sub")).thenReturn(PROVIDER_ID);
        when(oAuth2User.getAttribute("email")).thenReturn(EMAIL);
        lenient().when(oAuth2User.getAttribute("given_name")).thenReturn(GIVEN_NAME);
        lenient().when(oAuth2User.getAttribute("family_name")).thenReturn(FAMILY_NAME);
        when(oAuth2AuthenticationToken.getAuthorizedClientRegistrationId()).thenReturn("google");
    }

    @Test
    public void givenExistingUserWithProviderId_whenOnAuthenticationSuccess_thenUserIsLoggedIn() throws IOException {
        UserEntity existingUser = UserEntity.builder()
                .id(USER_ID)
                .email(EMAIL)
                .providerUserId(PROVIDER_ID)
                .role(UserRole.CLIENT)
                .displayName("JohnDoe")
                .build();
        when(userRepository.findByProviderUserId(PROVIDER_ID)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(USER_ID)).thenReturn(JWT_TOKEN);
        when(authorizedClientRepository.loadAuthorizedClient(anyString(), any(), any())).thenReturn(null);
        successHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);
        verify(userRepository, never()).save(any());
        verify(jwtService).generateToken(USER_ID);
        verify(response, atLeast(5)).addCookie(any(Cookie.class));
        verify(redirectStrategy).sendRedirect(request, response, FRONTEND_URL + "/auth/callback");
    }

    @Test
    public void givenExistingUserWithEmail_whenOnAuthenticationSuccess_thenProviderIdIsLinked() throws IOException {
        UserEntity existingUser = UserEntity.builder()
                .id(USER_ID)
                .email(EMAIL)
                .role(UserRole.CLIENT)
                .displayName("JohnDoe")
                .build();
        when(userRepository.findByProviderUserId(PROVIDER_ID)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(USER_ID)).thenReturn(JWT_TOKEN);
        when(authorizedClientRepository.loadAuthorizedClient(anyString(), any(), any())).thenReturn(null);
        successHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();
        assertThat(savedUser.getProviderUserId()).isEqualTo(PROVIDER_ID);
        assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
        verify(redirectStrategy).sendRedirect(request, response, FRONTEND_URL + "/auth/callback");
    }

    @Test
    public void givenNewUser_whenOnAuthenticationSuccess_thenNewUserIsCreated() throws IOException {
        when(userRepository.findByProviderUserId(PROVIDER_ID)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Stub save to set ID using reflection (simulating JPA behavior)
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            Field idField = UserEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, USER_ID);
            return user;
        });

        lenient().when(jwtService.generateToken(any(UUID.class))).thenReturn(JWT_TOKEN);
        when(authorizedClientRepository.loadAuthorizedClient(anyString(), any(), any())).thenReturn(null);
        successHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
        assertThat(savedUser.getProviderUserId()).isEqualTo(PROVIDER_ID);
        assertThat(savedUser.getName()).isEqualTo(GIVEN_NAME);
        assertThat(savedUser.getSurname()).isEqualTo(FAMILY_NAME);
        assertThat(savedUser.getRole()).isEqualTo(UserRole.INCOMPLETE);
        verify(redirectStrategy).sendRedirect(request, response, FRONTEND_URL + "/auth/callback");
    }

    @Test
    public void givenRefreshToken_whenOnAuthenticationSuccess_thenRefreshTokenIsSaved() throws IOException {
        UserEntity existingUser = UserEntity.builder()
                .id(USER_ID)
                .email(EMAIL)
                .providerUserId(PROVIDER_ID)
                .role(UserRole.CLIENT)
                .build();
        OAuth2RefreshToken refreshToken = mock(OAuth2RefreshToken.class);
        when(refreshToken.getTokenValue()).thenReturn("refresh-token-123");
        when(oAuth2AuthorizedClient.getRefreshToken()).thenReturn(refreshToken);
        when(userRepository.findByProviderUserId(PROVIDER_ID)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(USER_ID)).thenReturn(JWT_TOKEN);
        when(authorizedClientRepository.loadAuthorizedClient(anyString(), any(), any()))
                .thenReturn(oAuth2AuthorizedClient);
        successHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();
        assertThat(savedUser.getProviderRefreshToken()).isEqualTo("refresh-token-123");
    }

    @Test
    public void givenNoRefreshToken_whenOnAuthenticationSuccess_thenUserIsSavedWithoutRefreshToken() throws IOException {
        UserEntity existingUser = UserEntity.builder()
                .id(USER_ID)
                .email(EMAIL)
                .providerUserId(PROVIDER_ID)
                .role(UserRole.CLIENT)
                .build();
        when(oAuth2AuthorizedClient.getRefreshToken()).thenReturn(null);
        when(userRepository.findByProviderUserId(PROVIDER_ID)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(USER_ID)).thenReturn(JWT_TOKEN);
        when(authorizedClientRepository.loadAuthorizedClient(anyString(), any(), any()))
                .thenReturn(oAuth2AuthorizedClient);
        successHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);
        verify(userRepository, never()).save(any());
        verify(redirectStrategy).sendRedirect(request, response, FRONTEND_URL + "/auth/callback");
    }

    @Test
    public void givenNullAuthorizedClient_whenOnAuthenticationSuccess_thenNoRefreshTokenIsProcessed() throws IOException {
        UserEntity existingUser = UserEntity.builder()
                .id(USER_ID)
                .email(EMAIL)
                .providerUserId(PROVIDER_ID)
                .role(UserRole.CLIENT)
                .build();
        when(userRepository.findByProviderUserId(PROVIDER_ID)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(USER_ID)).thenReturn(JWT_TOKEN);
        when(authorizedClientRepository.loadAuthorizedClient(anyString(), any(), any())).thenReturn(null);
        successHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);
        verify(userRepository, never()).save(any());
        verify(redirectStrategy).sendRedirect(request, response, FRONTEND_URL + "/auth/callback");
    }

    @Test
    public void givenUserWithDisplayName_whenOnAuthenticationSuccess_thenCookiesContainDisplayName() throws IOException {
        UserEntity existingUser = UserEntity.builder()
                .id(USER_ID)
                .email(EMAIL)
                .providerUserId(PROVIDER_ID)
                .role(UserRole.CLIENT)
                .displayName("CustomName")
                .build();
        when(userRepository.findByProviderUserId(PROVIDER_ID)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(USER_ID)).thenReturn(JWT_TOKEN);
        when(authorizedClientRepository.loadAuthorizedClient(anyString(), any(), any())).thenReturn(null);
        successHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);
        verify(response, atLeast(5)).addCookie(cookieCaptor.capture());
        Cookie usernameCookie = cookieCaptor.getAllValues().stream()
                .filter(c -> c.getName().equals("auth_username"))
                .findFirst()
                .orElseThrow();
        String expectedEncodedUsername = URLEncoder.encode("CustomName", StandardCharsets.UTF_8);
        assertThat(usernameCookie.getValue()).isEqualTo(expectedEncodedUsername);
        assertThat(usernameCookie.getPath()).isEqualTo("/");
        assertThat(usernameCookie.getMaxAge()).isEqualTo(30);
        assertThat(usernameCookie.getSecure()).isTrue();
        assertThat(usernameCookie.isHttpOnly()).isFalse();
    }

    @Test
    public void givenUserWithoutDisplayName_whenOnAuthenticationSuccess_thenCookiesContainEmail() throws IOException {
        UserEntity existingUser = UserEntity.builder()
                .id(USER_ID)
                .email(EMAIL)
                .providerUserId(PROVIDER_ID)
                .role(UserRole.CLIENT)
                .build();
        when(userRepository.findByProviderUserId(PROVIDER_ID)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(USER_ID)).thenReturn(JWT_TOKEN);
        when(authorizedClientRepository.loadAuthorizedClient(anyString(), any(), any())).thenReturn(null);
        successHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);
        verify(response, atLeast(5)).addCookie(cookieCaptor.capture());
        Cookie usernameCookie = cookieCaptor.getAllValues().stream()
                .filter(c -> c.getName().equals("auth_username"))
                .findFirst()
                .orElseThrow();
        String expectedEncodedEmail = URLEncoder.encode(EMAIL, StandardCharsets.UTF_8);
        assertThat(usernameCookie.getValue()).isEqualTo(expectedEncodedEmail);
    }

    @Test
    public void givenAuthenticationSuccess_whenOnAuthenticationSuccess_thenAllCookiesAreSet() throws IOException {
        UserEntity existingUser = UserEntity.builder()
                .id(USER_ID)
                .email(EMAIL)
                .providerUserId(PROVIDER_ID)
                .role(UserRole.CLIENT)
                .displayName("JohnDoe")
                .build();
        when(userRepository.findByProviderUserId(PROVIDER_ID)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(USER_ID)).thenReturn(JWT_TOKEN);
        when(authorizedClientRepository.loadAuthorizedClient(anyString(), any(), any())).thenReturn(null);
        successHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);
        verify(response, times(5)).addCookie(cookieCaptor.capture());
        Map<String, Cookie> cookieMap = cookieCaptor.getAllValues().stream()
                .collect(java.util.stream.Collectors.toMap(Cookie::getName, c -> c));
        assertThat(cookieMap).containsKeys("auth_token", "auth_role", "auth_userid", "auth_email", "auth_username");
        assertThat(cookieMap.get("auth_token").getValue()).isEqualTo(URLEncoder.encode(JWT_TOKEN, StandardCharsets.UTF_8));
        assertThat(cookieMap.get("auth_role").getValue()).isEqualTo("CLIENT");
        assertThat(cookieMap.get("auth_userid").getValue()).isEqualTo(URLEncoder.encode(USER_ID.toString(), StandardCharsets.UTF_8));
        assertThat(cookieMap.get("auth_email").getValue()).isEqualTo(URLEncoder.encode(EMAIL, StandardCharsets.UTF_8));
        for (Cookie cookie : cookieMap.values()) {
            assertThat(cookie.getPath()).isEqualTo("/");
            assertThat(cookie.getMaxAge()).isEqualTo(30);
            assertThat(cookie.getSecure()).isTrue();
            assertThat(cookie.isHttpOnly()).isFalse();
        }
    }

    @Test
    public void givenOperatorUser_whenOnAuthenticationSuccess_thenRoleCookieIsOperator() throws IOException {
        UserEntity existingUser = UserEntity.builder()
                .id(USER_ID)
                .email(EMAIL)
                .providerUserId(PROVIDER_ID)
                .role(UserRole.OPERATOR)
                .displayName("JohnDoe")
                .build();
        when(userRepository.findByProviderUserId(PROVIDER_ID)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(USER_ID)).thenReturn(JWT_TOKEN);
        when(authorizedClientRepository.loadAuthorizedClient(anyString(), any(), any())).thenReturn(null);
        successHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);
        verify(response, atLeast(5)).addCookie(cookieCaptor.capture());
        Cookie roleCookie = cookieCaptor.getAllValues().stream()
                .filter(c -> c.getName().equals("auth_role"))
                .findFirst()
                .orElseThrow();
        assertThat(roleCookie.getValue()).isEqualTo("OPERATOR");
    }
}
