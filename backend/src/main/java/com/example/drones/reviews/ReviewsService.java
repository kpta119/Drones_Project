package com.example.drones.reviews;

import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.orders.*;
import com.example.drones.orders.exceptions.OrderNotFoundException;
import com.example.drones.reviews.dto.ReviewRequest;
import com.example.drones.reviews.dto.ReviewResponse;
import com.example.drones.reviews.dto.UserReviewResponse;
import com.example.drones.reviews.exceptions.IllegalOrderStateToReviewException;
import com.example.drones.reviews.exceptions.IllegalTargetOfReviewException;
import com.example.drones.reviews.exceptions.ReviewAlreadyExistsException;
import com.example.drones.reviews.exceptions.UnauthorizedReviewException;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewsService {

    private final ReviewsRepository reviewsRepository;
    private final OrdersRepository ordersRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;
    private final NewMatchedOrdersRepository newMatchedOrdersRepository;

    @Transactional
    public ReviewResponse createReview(UUID orderId, UUID targetId, UUID authorId, ReviewRequest request) {
        if (authorId.equals(targetId)) {
            throw new IllegalTargetOfReviewException();
        }

        OrdersEntity order = ordersRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalOrderStateToReviewException();
        }

        // Sprawdzenie, czy autor ma prawo wystawić opinię dla tego zlecenia
        boolean isAuthorOrderOwner = order.getUserId().equals(authorId);
        boolean isAuthorAssignedOperator = newMatchedOrdersRepository
                .findByOrderIdAndOperatorId(orderId, authorId)
                .map(match -> match.getOperatorStatus() == MatchedOrderStatus.ACCEPTED
                        && match.getClientStatus() == MatchedOrderStatus.ACCEPTED)
                .orElse(false);

        if (!isAuthorOrderOwner && !isAuthorAssignedOperator) {
            throw new UnauthorizedReviewException();
        }

        // Sprawdzenie, czy target (odbiorca opinii) jest związany ze zleceniem
        boolean isTargetOrderOwner = order.getUserId().equals(targetId);
        boolean isTargetAssignedOperator = newMatchedOrdersRepository
                .findByOrderIdAndOperatorId(orderId, targetId)
                .map(match -> match.getOperatorStatus() == MatchedOrderStatus.ACCEPTED
                        && match.getClientStatus() == MatchedOrderStatus.ACCEPTED)
                .orElse(false);

        if (!isTargetOrderOwner && !isTargetAssignedOperator) {
            throw new UnauthorizedReviewException();
        }

        if (reviewsRepository.existsByOrderIdAndAuthorIdAndTargetId(orderId, authorId, targetId)) {
            throw new ReviewAlreadyExistsException();
        }

        UserEntity author = userRepository.findById(authorId).orElseThrow(UserNotFoundException::new);
        UserEntity target = userRepository.findById(targetId).orElseThrow(UserNotFoundException::new);

        ReviewEntity review = new ReviewEntity();
        review.setOrder(order);
        review.setAuthor(author);
        review.setTarget(target);
        review.setStars(request.getStars());
        review.setBody(request.getBody());

        ReviewEntity savedReview = reviewsRepository.save(review);
        return reviewMapper.toResponse(savedReview);
    }

    public List<UserReviewResponse> getUserReviews(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }

        List<ReviewEntity> reviews = reviewsRepository.findAllByTargetId(userId);
        return reviewMapper.toUserReviewResponseList(reviews);
    }
}