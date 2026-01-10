package com.example.drones.common;

import com.example.drones.orders.MatchedOrderStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class MatchedOrderStatusConverter implements Converter<String, MatchedOrderStatus> {

    @Override
    public MatchedOrderStatus convert(String value) {
        return MatchedOrderStatus.valueOf(value.toUpperCase());
    }
}