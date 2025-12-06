package com.example.drones.services;

import com.example.drones.user.UserEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OperatorServicesService {

    private final OperatorServicesRepository operatorServicesRepository;

    @Transactional
    public void addOperatorServices(UserEntity operator, List<String> services) {
        for (String service : services) {
            OperatorServicesEntity entity = new OperatorServicesEntity();
            entity.setOperator(operator);
            entity.setServiceName(service);
            operatorServicesRepository.save(entity);
        }
    }
}
