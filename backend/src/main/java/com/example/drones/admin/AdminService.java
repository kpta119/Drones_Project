package com.example.drones.admin;

import com.example.drones.admin.dto.UserDto;
import com.example.drones.admin.exceptions.NoSuchUserException;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class AdminService {
    private final AdminRepository adminRepository;
    private final AdminMapper adminMapper;

    public Page<UserDto> getUsers(String query, UserRole role, Pageable pageable) {
        return adminRepository.findAllByQueryAndRole(query, role, pageable);
    }

    public UserDto banUser(UUID userId) {
        UserEntity user = adminRepository.findById(userId)
                .orElseThrow(NoSuchUserException::new);

        user.setRole(UserRole.BLOCKED);
        adminRepository.save(user);
        return adminMapper.toUserDto(user);
    }

}
