package com.example.drones.operators;

import com.example.drones.common.config.JwtService;
import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.operators.exceptions.OperatorAlreadyExistsException;
import com.example.drones.operators.dto.CreateOperatorDto;
import com.example.drones.operators.dto.EditOperatorProfileDto;
import com.example.drones.services.OperatorServicesService;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OperatorsService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CacheManager cacheManager;
    private final OperatorServicesService operatorServicesService;

    @Transactional
    public CreateOperatorDto createProfile(CreateOperatorDto operatorDto) {
        UUID userId = jwtService.extractUserId();

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (user.getRole() == UserRole.OPERATOR) {
            throw new OperatorAlreadyExistsException();
        }
        user.setCoordinates(operatorDto.coordinates());
        user.setRadius(operatorDto.radius());
        user.setCertificates(operatorDto.certificates());
        user.setRole(UserRole.OPERATOR);
        userRepository.save(user);

        operatorServicesService.addOperatorServices(user, operatorDto.services());
        return operatorDto;
    }

    @Transactional
    public EditOperatorProfileDto editProfile(EditOperatorProfileDto operatorDto) {
        return operatorDto; // TODO: Placeholder for future implementation
    }
}
