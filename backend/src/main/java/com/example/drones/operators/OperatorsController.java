package com.example.drones.operators;

import com.example.drones.operators.dto.CreateOperatorProfileDto;
import com.example.drones.operators.dto.CreatePortfolioDto;
import com.example.drones.operators.dto.OperatorPortfolioDto;
import com.example.drones.operators.dto.OperatorProfileDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operators")
@RequiredArgsConstructor
public class OperatorsController {

    private final OperatorsService operatorsService;

    @PostMapping("/createOperatorProfile")
    public ResponseEntity<OperatorProfileDto> createOperatorProfile(@RequestBody @Valid CreateOperatorProfileDto operatorDto) {
        OperatorProfileDto response = operatorsService.createProfile(operatorDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/editOperatorProfile")
    public ResponseEntity<OperatorProfileDto> editOperatorProfile(@RequestBody @Valid OperatorProfileDto operatorDto) {
        OperatorProfileDto response = operatorsService.editProfile(operatorDto);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

    }

    @PostMapping("/addPortfolio")
    public ResponseEntity<OperatorPortfolioDto> addPortfolio(@RequestBody @Valid CreatePortfolioDto portfolioDto) {
        OperatorPortfolioDto response = operatorsService.createPortfolio(portfolioDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
