package com.example.drones.services;

import com.example.drones.services.dto.ServiceRequest;
import com.example.drones.services.exceptions.ServiceAlreadyExistsException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServicesService {

    private final ServicesRepository servicesRepository;

    public List<String> getAllServices() {
        return servicesRepository.findAll()
                .stream()
                .map(ServicesEntity::getName)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<String> addServices(List<String> serviceNames) {
        List<ServicesEntity> newEntities = serviceNames.stream()
                .filter(name -> !servicesRepository.existsById(name))
                .map(ServicesEntity::new)
                .collect(Collectors.toList());

        servicesRepository.saveAll(newEntities);

        return serviceNames;
    }
}