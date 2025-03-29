package com.ecom.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecom.model.Reviews;
import com.ecom.repository.ReviewsRepository;

@Service
public class ReviewsService {
	@Autowired
	private ReviewsRepository reviewsRepository;

	public boolean deleteReviewByProductAndUser(Integer productId, Integer userId) {
		Reviews review = reviewsRepository.findByProductIdAndUserId(productId, userId);
		if (review != null) {
			reviewsRepository.delete(review);
			return true;
		}
		return false;
	}

	public Reviews findByProductIdAndUserId(Integer productId, Integer userId) {
		Reviews review = reviewsRepository.findByProductIdAndUserId(productId, userId);
		return review;
	}

	public boolean existsByProductIdAndUserId(Integer productId, Integer userId) {
		return reviewsRepository.existsByProductIdAndUserId(productId, userId);
	}

	public Reviews saveReview(Reviews review) {
		return reviewsRepository.save(review);
	}

}
