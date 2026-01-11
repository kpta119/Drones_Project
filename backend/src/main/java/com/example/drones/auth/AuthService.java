package com.example.drones.auth;


import com.example.drones.auth.dto.LoginRequest;
import com.example.drones.auth.dto.LoginResponse;
import com.example.drones.auth.dto.RegisterRequest;
import com.example.drones.auth.exceptions.InvalidCredentialsException;
import com.example.drones.auth.exceptions.UserAccountLockedException;
import com.example.drones.auth.exceptions.UserAlreadyExistsException;
import com.example.drones.common.config.auth.JwtService;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserMapper;
import com.example.drones.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.email();
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(email);
        }
        String hashedPassword = passwordEncoder.encode(request.password());
        UserEntity user = userMapper.toEntity(request, hashedPassword);
        userRepository.save(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

        } catch (InternalAuthenticationServiceException | BadCredentialsException e) {
            if (e.getCause() instanceof LockedException) {
                throw new UserAccountLockedException();
            }
            throw new InvalidCredentialsException();
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwtToken = jwtService.generateToken(UUID.fromString(userDetails.getUsername()));
        UserEntity user = userRepository.findById(UUID.fromString(userDetails.getUsername()))
                .orElseThrow(InvalidCredentialsException::new);
        return userMapper.toLoginResponse(user, jwtToken);

    }
}
