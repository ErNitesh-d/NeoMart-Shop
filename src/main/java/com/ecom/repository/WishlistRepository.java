package com.ecom.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.model.Wishlist;

public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {

	public Wishlist findByProductIdAndUserId(Integer productId, Integer userId);

	public Integer countByUserId(Integer userId);

	public List<Wishlist> findByUserId(Integer userId);

	public Optional<Wishlist> findByUserIdAndProductId(Integer userId, Integer productId);

	boolean existsByUserIdAndProductId(Integer userId, Integer productId);

}
