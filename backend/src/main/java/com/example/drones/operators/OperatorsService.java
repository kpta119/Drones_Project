package com.example.drones.operators;

import com.example.drones.auth.exceptions.InvalidCredentialsException;
import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.operators.dto.*;
import com.example.drones.operators.exceptions.NoSuchOperatorException;
import com.example.drones.operators.exceptions.NoSuchPortfolioException;
import com.example.drones.operators.exceptions.PortfolioAlreadyExistsException;
import com.example.drones.orders.*;
import com.example.drones.orders.exceptions.OrderNotFoundException;
import com.example.drones.services.OperatorServicesService;
import com.example.drones.services.ServicesEntity;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserMapper;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.util.Precision;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
    @CacheEvict(value = "users", key = "#userId")
    public OperatorProfileDto createProfile(UUID userId, CreateOperatorProfileDto operatorDto) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

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

    @Transactional(readOnly = true)
    public Page<MatchedOrderDto> getMatchedOrders(UUID userId, MatchedOrdersFilters filters, Pageable pageable) {
        UserEntity operator = userRepository.findByIdWithPortfolio(userId)
                .orElseThrow(UserNotFoundException::new);

        String location = filters.location() != null ? filters.location() : operator.getCoordinates();
        Integer radius = filters.radius() != null ? filters.radius() : operator.getRadius();

        Specification<OrdersEntity> spec = createSpecification(userId, filters, location, radius);
        Page<OrdersEntity> orders = ordersRepository.findAll(spec, pageable);

        List<MatchedOrderDto> dtos = orders.getContent().stream()
                .map(order -> {
                    NewMatchedOrderEntity matchedOrder = order.getMatchedOrders().getFirst();
                    Double distance = Precision.round(calculateDistance(location, order.getCoordinates()), 2);

                    return ordersMapper.toMatchedOrderDto(order, matchedOrder, distance);
                })
                .toList();

        return new PageImpl<>(dtos, pageable, orders.getTotalElements());
    }

    @SuppressWarnings("unchecked")
    private Specification<OrdersEntity> createSpecification(UUID userId, MatchedOrdersFilters filters, String location, Integer radius) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            boolean isCountQuery = Long.class == Objects.requireNonNull(query).getResultType() || long.class == query.getResultType();

            Join<OrdersEntity, ServicesEntity> service;

            if (isCountQuery) {
                service = root.join("service", JoinType.INNER);

            } else {
                service = (Join<OrdersEntity, ServicesEntity>) (Object) root.fetch("service", JoinType.INNER);

                Fetch<OrdersEntity, UserEntity> userFetch = root.fetch("user", JoinType.INNER);
                userFetch.fetch("portfolio", JoinType.LEFT);
            }

            Join<OrdersEntity, NewMatchedOrderEntity> nmo = root.join("matchedOrders", JoinType.INNER);

            predicates.add(cb.equal(nmo.get("operator").get("id"), userId));

            if (filters.service() != null) {
                predicates.add(cb.equal(service.get("name"), filters.service()));
            }
            if (filters.order_status() != null) {
                predicates.add(cb.equal(root.get("status"), filters.order_status()));
            }
            if (filters.from_date() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fromDate"), filters.from_date()));
            }
            if (filters.to_date() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("toDate"), filters.to_date()));
            }
            if (filters.client_status() != null) {
                predicates.add(cb.equal(nmo.get("clientStatus"), filters.client_status()));
            }
            if (filters.operator_status() != null) {
                predicates.add(cb.equal(nmo.get("operatorStatus"), filters.operator_status()));
            }

            predicates.add(createDistancePredicate(cb, root, location, radius));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Predicate createDistancePredicate(
            CriteriaBuilder cb,
            Root<OrdersEntity> root,
            String location,
            Integer radius
    ) {
        try {
            String[] coords = location.split(",");
            if (coords.length != 2) {
                return cb.conjunction();
            }

            double lat1 = Double.parseDouble(coords[0].trim());
            double lon1 = Double.parseDouble(coords[1].trim());

            Expression<String> coordinates = root.get("coordinates");
            Expression<String> latString = cb.function("SPLIT_PART", String.class,
                    coordinates, cb.literal(","), cb.literal(1));

            Expression<String> lonString = cb.function("SPLIT_PART", String.class,
                    coordinates, cb.literal(","), cb.literal(2));

            Expression<Double> lat2 = cb.function("float8", Double.class, latString);
            Expression<Double> lon2 = cb.function("float8", Double.class, lonString);

            // 6371 * acos(cos(radians(lat1)) * cos(radians(lat2)) * cos(radians(lon2) - radians(lon1)) + sin(radians(lat1)) * sin(radians(lat2)))
            Expression<Double> distance = cb.prod(
                    cb.literal(6371.0),
                    cb.function("ACOS", Double.class,
                            cb.sum(
                                    cb.prod(
                                            cb.prod(
                                                    cb.function("COS", Double.class, cb.function("RADIANS", Double.class, cb.literal(lat1))),
                                                    cb.function("COS", Double.class, cb.function("RADIANS", Double.class, lat2))
                                            ),
                                            cb.function("COS", Double.class,
                                                    cb.diff(
                                                            cb.function("RADIANS", Double.class, lon2),
                                                            cb.function("RADIANS", Double.class, cb.literal(lon1))
                                                    )
                                            )
                                    ),
                                    cb.prod(
                                            cb.function("SIN", Double.class, cb.function("RADIANS", Double.class, cb.literal(lat1))),
                                            cb.function("SIN", Double.class, cb.function("RADIANS", Double.class, lat2))
                                    )
                            )
                    )
            );

            return cb.lessThanOrEqualTo(distance, radius.doubleValue());

        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return cb.conjunction();
        }
    }

    private Double calculateDistance(String location1, String location2) {
        if (location1 == null || location2 == null) {
            return null;
        }

        String[] cords1 = location1.split(",");
        String[] cords2 = location2.split(",");

        if (cords1.length != 2 || cords2.length != 2) {
            return null;
        }

        double lat1 = Double.parseDouble(cords1[0].trim());
        double lon1 = Double.parseDouble(cords1[1].trim());
        double lat2 = Double.parseDouble(cords2[0].trim());
        double lon2 = Double.parseDouble(cords2[1].trim());

        double earthRadius = 6371;
        return earthRadius * Math.acos(
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.cos(Math.toRadians(lon2) - Math.toRadians(lon1)) +
                        Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
        );
    }
}
