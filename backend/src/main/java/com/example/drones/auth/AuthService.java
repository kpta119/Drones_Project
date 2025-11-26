package com.example.drones.auth;


import com.example.drones.auth.dto.LoginRequest;
import com.example.drones.auth.dto.LoginResponse;
import com.example.drones.auth.dto.RegisterRequest;
import com.example.drones.auth.exceptions.InvalidCredentialsException;
import com.example.drones.auth.exceptions.UserAlreadyExistsException;
import com.example.drones.config.JwtService;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserMapper;
import com.example.drones.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException();

        }
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        String jwtToken = jwtService.generateToken(user.getId());
        return new LoginResponse(jwtToken);

    }
}
