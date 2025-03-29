package com.ecom.service;

import java.util.List;

import com.ecom.model.Wishlist;

public interface WishlistService {

	public Wishlist saveWishlist(Integer productId, Integer userId);

	public List<Wishlist> getWishlistByUser(Integer userId);

	public Integer getCountWishlist(Integer userId);

	public void updateQuantity(String sy, Integer cid);

	boolean toggleWishlist(Integer productId, Integer userId);

	void deleteWishlistItem(Integer userId, Integer productId);

	boolean isProductInWishlist(Integer userId, Integer productId);

}