package com.example.drones.operators;

import com.example.drones.auth.exceptions.InvalidCredentialsException;
import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.operators.dto.*;
import com.example.drones.operators.exceptions.NoSuchOperatorException;
import com.example.drones.operators.exceptions.NoSuchPortfolioException;
import com.example.drones.operators.exceptions.OperatorAlreadyExistsException;
import com.example.drones.operators.exceptions.PortfolioAlreadyExistsException;
import com.example.drones.orders.*;
import com.example.drones.orders.exceptions.OrderNotFoundException;
import com.example.drones.services.OperatorServicesService;
import com.example.drones.services.ServicesEntity;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserMapper;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final OrdersRepository ordersRepository;
    private final NewMatchedOrdersRepository newMatchedOrdersRepository;
    private final OrdersMapper ordersMapper;

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

    public List<MatchingOperatorDto> getOperatorInfo(UUID userId, UUID orderId) {
        OrdersEntity order = ordersRepository.findByIdWithUser(orderId).orElseThrow(OrderNotFoundException::new);

        if (!order.getUser().getId().equals(userId)) {
            throw new InvalidCredentialsException();
        }

        List<UserEntity> matchedOperators = newMatchedOrdersRepository.findInterestedOperatorByOrderId(orderId);
        return matchedOperators.stream()
                .map(operatorMapper::toMatchingOperatorDto)
                .toList();
    }

    public Page<MatchedOrderDto> getMatchedOrders(UUID userId, MatchedOrdersFilters filters, Pageable pageable) {
        UserEntity operator = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (operator.getRole() != UserRole.OPERATOR) {
            throw new NoSuchOperatorException();
        }

        String location = filters.location() != null ? filters.location() : operator.getCoordinates();
        Integer radius = filters.radius() != null ? filters.radius() : operator.getRadius();

        Specification<OrdersEntity> spec = createSpecification(userId, filters, location, radius);
        Page<OrdersEntity> orders = ordersRepository.findAll(spec, pageable);
        // TODO:
        return null;
    }

    @SuppressWarnings("unchecked")
    private Specification<OrdersEntity> createSpecification(UUID userId, MatchedOrdersFilters filters,
                                                            String location, Integer radius) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<OrdersEntity, NewMatchedOrderEntity> nmo = root.join("matchedOrders", JoinType.INNER);

            Fetch<OrdersEntity, ServicesEntity> serviceFetch = root.fetch("service", JoinType.INNER);
            Join<OrdersEntity, ServicesEntity> service = (Join<OrdersEntity, ServicesEntity>) serviceFetch;

            Fetch<OrdersEntity, UserEntity> userFetch = root.fetch("user", JoinType.INNER);
            userFetch.fetch("portfolio", JoinType.LEFT);

            predicates.add(cb.equal(nmo.get("operator").get("id"), userId));

            if (filters.service() != null) {
                predicates.add(cb.equal(service.get("name"), filters.service()));
            }
            if (filters.orderStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filters.orderStatus()));
            }
            if (filters.fromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fromDate"), filters.fromDate()));
            }
            if (filters.toDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("toDate"), filters.toDate()));
            }
            if (filters.operatorStatus() != null) {
                predicates.add(cb.equal(nmo.get("operatorStatus"), filters.operatorStatus()));
            }
            if (filters.clientStatus() != null) {
                predicates.add(cb.equal(nmo.get("clientStatus"), filters.clientStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
