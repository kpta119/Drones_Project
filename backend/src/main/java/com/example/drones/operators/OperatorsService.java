package com.example.drones.operators;

import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.operators.dto.*;
import com.example.drones.operators.exceptions.NoSuchOperatorException;
import com.example.drones.operators.exceptions.NoSuchPortfolioException;
import com.example.drones.operators.exceptions.OperatorAlreadyExistsException;
import com.example.drones.operators.exceptions.PortfolioAlreadyExistsException;
import com.example.drones.services.OperatorServicesService;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserMapper;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OperatorsService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final OperatorServicesService operatorServicesService;
    private final UserMapper operatorMapper;
    private final PortfolioMapper portfolioMapper;

    @Transactional
    public OperatorProfileDto createProfile(UUID userId, CreateOperatorProfileDto operatorDto) {
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

    @CacheEvict(value = "operators", key = "#userId")
    @Transactional
    public OperatorProfileDto editProfile(UUID userId, OperatorProfileDto operatorDto) {
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

    @CacheEvict(value = "operators", key = "#userId")
    @Transactional
    public OperatorPortfolioDto createPortfolio(UUID userId, CreatePortfolioDto portfolioDto) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (user.getRole() != UserRole.OPERATOR) {
            throw new NoSuchOperatorException();
        }

        Optional<PortfolioEntity> oldPortfolio = portfolioRepository.findByOperatorId(user.getId());
        if (oldPortfolio.isPresent()) {
            throw new PortfolioAlreadyExistsException();
        }

        PortfolioEntity portfolio = new PortfolioEntity();
        portfolio.setOperator(user);
        portfolio.setTitle(portfolioDto.title());
        portfolio.setDescription(portfolioDto.description());
        PortfolioEntity savedPortfolio = portfolioRepository.save(portfolio);

        return portfolioMapper.toOperatorPortfolioDto(savedPortfolio);
    }

    @CacheEvict(value = "operators", key = "#userId")
    @Transactional
    public OperatorPortfolioDto editPortfolio(UUID userId, UpdatePortfolioDto portfolioDto) {
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

    @Cacheable(value = "operators", key = "#userId")
    @Transactional(readOnly = true)
    public OperatorDto getOperatorProfile(UUID userId) {
        UserEntity user = userRepository.findByIdWithPortfolio(userId)
                .orElseThrow(UserNotFoundException::new);
        if (user.getRole() != UserRole.OPERATOR) {
            throw new NoSuchOperatorException();
        }
        List<String> services = operatorServicesService.getOperatorServices(user);
        PortfolioEntity portfolio = user.getPortfolio();
        return operatorMapper.toOperatorDto(user, services, portfolioMapper.toOperatorPortfolioDto(portfolio));
    }
}
