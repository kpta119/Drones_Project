package com.example.drones.reviews;

import com.example.drones.reviews.dto.ReviewResponse;
import com.example.drones.reviews.dto.UserReviewResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "target.id", target = "targetId")
    ReviewResponse toResponse(ReviewEntity entity);

    @Mapping(source = "author.name", target = "name")
    @Mapping(source = "author.surname", target = "surname")
    @Mapping(source = "author.displayName", target = "username")
    UserReviewResponse toUserReviewResponse(ReviewEntity entity);

    List<UserReviewResponse> toUserReviewResponseList(List<ReviewEntity> entities);
}