package com.example.drones.auth;

import com.example.drones.auth.dto.RegisterRequest;
import com.example.drones.auth.exceptions.UserAlreadyExistsException;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.email();
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(email);
        }
        String hashedPassword = passwordEncoder.encode(request.password());
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(request.email());
        userEntity.setPassword(hashedPassword);
        userEntity.setUsername(request.username());
        userEntity.setName(request.name());
        userEntity.setSurname(request.surname());
        userEntity.setPhoneNumber(request.phoneNumber());
        userRepository.save(userEntity);
    }

}
