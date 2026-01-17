package com.example.drones.operators;

import com.example.drones.common.config.auth.JwtService;
import com.example.drones.operators.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/operators")
@RequiredArgsConstructor
public class OperatorsController {

    private final OperatorsService operatorsService;
    private final JwtService jwtService;

    @PostMapping("/createOperatorProfile")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<OperatorProfileDto> createOperatorProfile(@RequestBody @Valid CreateOperatorProfileDto operatorDto) {
        UUID userId = jwtService.extractUserId();
        OperatorProfileDto response = operatorsService.createProfile(userId, operatorDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/editOperatorProfile")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<OperatorProfileDto> editOperatorProfile(@RequestBody @Valid OperatorProfileDto operatorDto) {
        UUID userId = jwtService.extractUserId();
        OperatorProfileDto response = operatorsService.editProfile(userId, operatorDto);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

    }

    @PostMapping("/addPortfolio")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<OperatorPortfolioDto> addPortfolio(@RequestBody @Valid CreatePortfolioDto portfolioDto) {
        UUID userId = jwtService.extractUserId();
        OperatorPortfolioDto response = operatorsService.createPortfolio(userId, portfolioDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/editPortfolio")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<OperatorPortfolioDto> editPortfolio(@RequestBody @Valid UpdatePortfolioDto portfolioDto) {
        UUID userId = jwtService.extractUserId();
        OperatorPortfolioDto response = operatorsService.editPortfolio(userId, portfolioDto);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/getOperatorProfile/{userId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'CLIENT', 'ADMIN')")
    public ResponseEntity<OperatorDto> getOperatorProfile(@PathVariable UUID userId) {
        OperatorDto response = operatorsService.getOperatorProfile(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getOperatorsInfo/{orderId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'CLIENT', 'ADMIN')")
    public ResponseEntity<List<MatchingOperatorDto>> getOperatorsInfo(@PathVariable UUID orderId) {
        UUID userId = jwtService.extractUserId();
        List<MatchingOperatorDto> response = operatorsService.getOperatorInfo(userId, orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getMatchedOrders")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<Page<MatchedOrderDto>> getMatchedOrders(
            @PageableDefault(size = 20) Pageable pageable,
            @ModelAttribute MatchedOrdersFilters filters
    ) {
        UUID userId = jwtService.extractUserId();
        Page<MatchedOrderDto> response = operatorsService.getMatchedOrders(userId, filters, pageable);
        return ResponseEntity.ok(response);
    }
}
