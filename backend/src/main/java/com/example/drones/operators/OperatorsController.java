package com.example.drones.operators;

import com.example.drones.operators.dto.CreateOperatorDto;
import com.example.drones.operators.dto.EditOperatorProfileDto;
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
    public ResponseEntity<CreateOperatorDto> createOperatorProfile(@RequestBody @Valid CreateOperatorDto operatorDto) {
        CreateOperatorDto response = operatorsService.createProfile(operatorDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/editOperatorProfile")
    public ResponseEntity<EditOperatorProfileDto> editOperatorProfile(@RequestBody @Valid EditOperatorProfileDto operatorDto) {
        EditOperatorProfileDto response = operatorsService.editProfile(operatorDto);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

    }
}
