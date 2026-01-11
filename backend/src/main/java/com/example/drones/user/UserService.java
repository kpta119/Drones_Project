package com.example.drones.user;

import com.example.drones.auth.exceptions.InvalidCredentialsException;
import com.example.drones.common.config.auth.JwtService;
import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.user.dto.UserResponse;
import com.example.drones.user.dto.UserUpdateRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    @Cacheable(value = "users", key = "#userId")
    public UserResponse getUserData(UUID userId) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return userMapper.toResponse(userEntity);
    }

    @Transactional
    @CacheEvict(value = "users", key = "@jwtService.extractUserId()")
    public UserResponse editUserData(UserUpdateRequest request) {
        UUID userId = jwtService.extractUserId();

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (request.role() != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                throw new InvalidCredentialsException();
            }
            userEntity.setRole(request.role());
        }
        if (userEntity.getRole() == UserRole.INCOMPLETE) {
            userEntity.setRole(UserRole.CLIENT);
        }

        userMapper.updateEntityFromRequest(request, userEntity);
        UserEntity savedUser = userRepository.save(userEntity);

        return userMapper.toResponse(savedUser);
    }
}
