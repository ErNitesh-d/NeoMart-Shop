package com.ecom.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecom.model.Reviews;

@Repository
public interface ReviewsRepository extends JpaRepository<Reviews, Integer> {
	Page<Reviews> findByProductId(int productId, Pageable pageable);

	List<Reviews> findByProductId(int productId);

	boolean existsByProductIdAndUserId(Integer productId, Integer userId);

	Reviews findByProductIdAndUserId(Integer productId, Integer userId);
}
