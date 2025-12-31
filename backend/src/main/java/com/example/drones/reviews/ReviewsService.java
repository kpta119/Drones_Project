package com.example.drones.reviews;

import com.example.drones.common.config.exceptions.UserNotFoundException;
import com.example.drones.orders.OrderStatus;
import com.example.drones.orders.OrdersEntity;
import com.example.drones.orders.OrdersRepository;
import com.example.drones.orders.exceptions.IllegalOrderStateException;
import com.example.drones.orders.exceptions.IllegalOrderStatusException;
import com.example.drones.orders.exceptions.OrderNotFoundException;
import com.example.drones.reviews.dto.ReviewRequest;
import com.example.drones.reviews.dto.ReviewResponse;
import com.example.drones.reviews.dto.UserReviewResponse;
import com.example.drones.reviews.exceptions.IllegalOrderStateToReviewException;
import com.example.drones.reviews.exceptions.IllegalTargetOfReviewException;
import com.example.drones.reviews.exceptions.ReviewAlreadyExistsException;
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

    @Transactional
    public ReviewResponse createReview(UUID orderId, UUID targetId, UUID authorId, ReviewRequest request) {
        if (authorId.equals(targetId)) {
            throw new IllegalTargetOfReviewException();
        }

        OrdersEntity order = ordersRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (order.getStatus() != OrderStatus.COMPLETED && order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new IllegalOrderStateToReviewException();
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