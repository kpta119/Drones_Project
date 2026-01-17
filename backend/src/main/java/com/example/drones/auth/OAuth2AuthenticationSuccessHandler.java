package com.example.drones.auth;

import com.example.drones.common.config.auth.JwtService;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;
    private final UserRepository userRepository;
    @Value("${app.frontend_url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2AuthorizedClient client = authorizedClientRepository.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken,
                request
        );

        // Spotbugs wants 2 ifs
        String refreshToken = null;
        if (client != null) {
            var tokenObject = client.getRefreshToken();

            if (tokenObject != null) {
                refreshToken = tokenObject.getTokenValue();
            }
        }

        String providerId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");

        Optional<UserEntity> newUser = userRepository.findByProviderUserId(providerId);
        UserEntity user;
        if (newUser.isPresent()) {
            UserEntity oldProviderUser = newUser.get();
            if (refreshToken != null) {
                oldProviderUser.setProviderRefreshToken(refreshToken);
                userRepository.save(oldProviderUser);
            }
            user = oldProviderUser;
            log.info("Existing user logged in: {}", oldProviderUser.getEmail());
        } else {
            newUser = userRepository.findByEmail(email);
            if (newUser.isPresent()) {
                UserEntity oldUser = newUser.get();
                oldUser.setProviderUserId(providerId);
                oldUser.setProviderRefreshToken(refreshToken);
                userRepository.save(oldUser);
                user = oldUser;
                log.info("User with email {} already exists", email);
            } else {
                String name = oAuth2User.getAttribute("given_name");
                String surname = oAuth2User.getAttribute("family_name");
                UserEntity newProviderUser = UserEntity.builder()
                        .email(email)
                        .providerUserId(providerId)
                        .providerRefreshToken(refreshToken)
                        .name(name)
                        .surname(surname)
                        .role(UserRole.INCOMPLETE)
                        .build();
                userRepository.save(newProviderUser);
                user = newProviderUser;
                log.info("New user created: {}", email);
            }
        }
        String token = jwtService.generateToken(user.getId());
        String role = user.getRole().name();
        String userId = user.getId().toString();
        String username = user.getDisplayName() != null ? user.getDisplayName() : "";


        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/auth/callback")
                .queryParam("token", token)
                .queryParam("role", role)
                .queryParam("username", URLEncoder.encode(username, StandardCharsets.UTF_8))
                .queryParam("userid", userId)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}