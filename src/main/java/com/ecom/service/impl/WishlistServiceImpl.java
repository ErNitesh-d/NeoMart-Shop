package com.ecom.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.ecom.model.Product;
import com.ecom.model.UserDtls;
import com.ecom.model.Wishlist;
import com.ecom.repository.ProductRepository;
import com.ecom.repository.UserRepository;
import com.ecom.repository.WishlistRepository;
import com.ecom.service.WishlistService;

@Service
public class WishlistServiceImpl implements WishlistService {

	@Autowired
	private WishlistRepository wishlistRepository;

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ProductRepository productRepository;

	@Override
	public boolean isProductInWishlist(Integer userId, Integer productId) {
		return wishlistRepository.existsByUserIdAndProductId(userId, productId);
	}

	@Override
	public Wishlist saveWishlist(Integer productId, Integer userId) {
		// Fetch the user and product using the provided IDs
		UserDtls userDtls = userRepository.findById(userId).get();
		Product product = productRepository.findById(productId).get();

		// Check if the product already exists in the user's wishlist
		Wishlist wishlistStatus = wishlistRepository.findByProductIdAndUserId(productId, userId);

		// Initialize the wishlist object
		Wishlist wishlist = null;

		// If the product is not in the wishlist, add it
		if (ObjectUtils.isEmpty(wishlistStatus)) {
			wishlist = new Wishlist();
			wishlist.setProduct(product);
			wishlist.setUser(userDtls);
			wishlist.setTitle(product.getTitle()); // Assuming title is coming from product
			wishlist.setDescription(product.getDescription()); // Assuming description is coming from product
			wishlist.setCategory(product.getCategory()); // Assuming category is coming from product
			wishlist.setPrice(product.getPrice()); // Assuming price is coming from product
			wishlist.setStock(product.getStock()); // Assuming stock is coming from product
			wishlist.setImage(product.getImage()); // Assuming image is coming from product
			wishlist.setDiscount(product.getDiscount()); // Assuming discount is coming from product
			wishlist.setDiscountPrice(product.getDiscountPrice()); // Assuming discount price is coming from product
			wishlist.setIsActive(true); // You can set to true or based on your business logic
			wishlist.setAddedDate(LocalDateTime.now()); // Set the addedDate here
		} else {
			// If the product is already in the wishlist, you might not need to do anything
			wishlist = wishlistStatus;
		}

		// Save the wishlist object
		Wishlist savedWishlist = wishlistRepository.save(wishlist);

		return savedWishlist;
	}

	@Override
	public List<Wishlist> getWishlistByUser(Integer userId) {
		return wishlistRepository.findByUserId(userId);

	}

	@Override
	public Integer getCountWishlist(Integer userId) {
		Integer countByUserId = wishlistRepository.countByUserId(userId);

		return countByUserId;
	}

	@Override
	public void updateQuantity(String sy, Integer cid) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean toggleWishlist(Integer productId, Integer userId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void deleteWishlistItem(Integer userId, Integer productId) {
		// Find the wishlist item by userId and productId
		Optional<Wishlist> wishlistItem = wishlistRepository.findByUserIdAndProductId(userId, productId);

		if (wishlistItem.isPresent()) {
			wishlistRepository.delete(wishlistItem.get()); // Delete the wishlist item
		} else {
			// Handle the case where the item isn't found in the wishlist (optional)
			// You could log it or set an error message
		}
	}

}
