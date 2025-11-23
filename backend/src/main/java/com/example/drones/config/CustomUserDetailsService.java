package com.example.drones.config;

import com.example.drones.config.exceptions.UserNotFoundException;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return createUserDetails(userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email)));
    }

    public UserDetails loadUserById(UUID userId) {
        return createUserDetails(userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString())));
    }

    private UserDetails createUserDetails(UserEntity user) {
        if (user.getRole() == UserRole.BLOCKED) {
            throw new LockedException("User account is blocked");
        }

        return new User(
                user.getId().toString(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
