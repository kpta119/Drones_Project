package com.example.drones.user;

import com.example.drones.auth.exceptions.InvalidCredentialsException;
import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.user.dto.UserResponse;
import com.example.drones.user.dto.UserUpdateRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse getUserData(UUID userIdParam) {
        UUID userId = (userIdParam != null)
                ? userIdParam
                : UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        return userMapper.toResponse(userEntity);
    }

    @Transactional
    public UserResponse editUserData(UserUpdateRequest request){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(authentication.getName());

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (request.getRole() != null) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                throw new InvalidCredentialsException();
            }
            userEntity.setRole(request.getRole());
        }

        userMapper.updateEntityFromRequest(request, userEntity);
        UserEntity savedUser = userRepository.save(userEntity);

        return userMapper.toResponse(savedUser);
    }
}
