package com.example.drones.services;

import com.example.drones.user.UserEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OperatorServicesService {

    private final OperatorServicesRepository operatorServicesRepository;

    @Transactional
    public List<String> addOperatorServices(UserEntity operator, List<String> services) {
        List<String> savedServices = new ArrayList<>();
        for (String service : services) {
            OperatorServicesEntity entity = new OperatorServicesEntity();
            entity.setOperator(operator);
            entity.setServiceName(service);
            OperatorServicesEntity savedEntity = operatorServicesRepository.save(entity);
            savedServices.add(savedEntity.getServiceName());
        }
        return savedServices;
    }

    @Transactional
    public List<String> editOperatorServices(UserEntity operator, List<String> services) {
        operatorServicesRepository.deleteAllByOperator(operator);
        return addOperatorServices(operator, services);
    }

    public List<String> getOperatorServices(UserEntity operator) {
        List<OperatorServicesEntity> entities = operatorServicesRepository.findAllByOperatorId(operator.getId());
        List<String> services = new ArrayList<>();
        for (OperatorServicesEntity entity : entities) {
            services.add(entity.getServiceName());
        }
        return services;
    }
}
