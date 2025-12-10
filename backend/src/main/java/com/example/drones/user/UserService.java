package com.example.drones.user;

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

    public UserResponse getUserData(UUID userId) {
        if (userId == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserIdString = authentication.getName();
            userId = UUID.fromString(currentUserIdString);
        }
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Logged in user not found in database"));

        return userMapper.toResponse(userEntity);
    }

    @Transactional
    public UserResponse editUserData(UserUpdateRequest request){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(authentication.getName());

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("User not found"));

        userMapper.updateEntityFromRequest(request, userEntity);
        UserEntity savedUser = userRepository.save(userEntity);

        return userMapper.toResponse(savedUser);
    }
}
