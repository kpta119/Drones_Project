package com.example.drones.operators;

import com.example.drones.common.config.JwtService;
import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.operators.dto.CreateOperatorProfileDto;
import com.example.drones.operators.dto.CreatePortfolioDto;
import com.example.drones.operators.dto.OperatorPortfolioDto;
import com.example.drones.operators.dto.OperatorProfileDto;
import com.example.drones.operators.exceptions.NoSuchOperatorException;
import com.example.drones.operators.exceptions.NoSuchPortfolioException;
import com.example.drones.operators.exceptions.OperatorAlreadyExistsException;
import com.example.drones.services.OperatorServicesService;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserMapper;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OperatorsService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final JwtService jwtService;
    private final CacheManager cacheManager;
    private final OperatorServicesService operatorServicesService;
    private final UserMapper operatorMapper;
    private final PortfolioMapper portfolioMapper;

    @Transactional
    public OperatorProfileDto createProfile(CreateOperatorProfileDto operatorDto) {
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
        UserEntity savedUser = userRepository.save(user);

        List<String> savedServices = operatorServicesService.addOperatorServices(savedUser, operatorDto.services());
        return operatorMapper.toOperatorProfileDto(savedUser, savedServices);
    }

    @Transactional
    public OperatorProfileDto editProfile(OperatorProfileDto operatorDto) {
        UUID userId = jwtService.extractUserId();

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (user.getRole() != UserRole.OPERATOR) {
            throw new NoSuchOperatorException();
        }

        if (operatorDto.coordinates() != null) {
            user.setCoordinates(operatorDto.coordinates());
        }
        if (operatorDto.radius() != null) {
            user.setRadius(operatorDto.radius());
        }
        if (operatorDto.certificates() != null) {
            user.setCertificates(operatorDto.certificates());
        }
        UserEntity savedUser = userRepository.save(user);

        List<String> savedServices;
        if (operatorDto.services() != null) {
            savedServices = operatorServicesService.editOperatorServices(savedUser, operatorDto.services());
        } else {
            savedServices = operatorServicesService.getOperatorServices(savedUser);
        }
        return operatorMapper.toOperatorProfileDto(savedUser, savedServices);
    }

    @Transactional
    public OperatorPortfolioDto createPortfolio(CreatePortfolioDto portfolioDto) {
        UUID userId = jwtService.extractUserId();

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (user.getRole() != UserRole.OPERATOR) {
            throw new NoSuchOperatorException();
        }
        PortfolioEntity portfolio = new PortfolioEntity();
        portfolio.setOperator(user);
        portfolio.setTitle(portfolioDto.title());
        portfolio.setDescription(portfolioDto.description());
        PortfolioEntity savedPortfolio = portfolioRepository.save(portfolio);

        return portfolioMapper.toOperatorPortfolioDto(savedPortfolio);
    }

    @Transactional
    public OperatorPortfolioDto editPortfolio(OperatorPortfolioDto portfolioDto) {
        UUID userId = jwtService.extractUserId();

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (user.getRole() != UserRole.OPERATOR) {
            throw new NoSuchOperatorException();
        }
        PortfolioEntity portfolio = portfolioRepository.findByOperatorId(user.getId())
                .orElseThrow(NoSuchPortfolioException::new);

        if (portfolioDto.title() != null) {
            portfolio.setTitle(portfolioDto.title());
        }
        if (portfolioDto.description() != null) {
            portfolio.setDescription(portfolioDto.description());
        }
        PortfolioEntity savedPortfolio = portfolioRepository.save(portfolio);

        return portfolioMapper.toOperatorPortfolioDto(savedPortfolio);
    }
}
