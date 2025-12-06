package com.example.drones.operators;

import com.example.drones.operators.dto.OperatorDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operators")
@RequiredArgsConstructor
public class OperatorsController {

    private final OperatorsService operatorsService;

    @PostMapping("/createOperatorProfile")
    public ResponseEntity<OperatorDto> createOperatorProfile(@RequestBody @Valid OperatorDto operatorDto) {
        OperatorDto response = operatorsService.createProfile(operatorDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
