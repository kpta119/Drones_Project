package com.example.drones.operators;

import com.example.drones.operators.dto.OperatorPortfolioDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface PortfolioMapper {

    @Mapping(
            target = "photos",
            source = "photos",
            defaultExpression = "java(java.util.Collections.emptyList())"
    )
    OperatorPortfolioDto toOperatorPortfolioDto(PortfolioEntity portfolioEntity);
}
