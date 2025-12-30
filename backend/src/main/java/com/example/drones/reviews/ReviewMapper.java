package com.example.drones.reviews;

import com.example.drones.reviews.dto.ReviewResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "target.id", target = "targetId")
    ReviewResponse toResponse(ReviewEntity entity);
}