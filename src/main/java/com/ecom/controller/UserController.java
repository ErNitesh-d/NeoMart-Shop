package com.ecom.controller;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Cart;
import com.ecom.model.CartService;
import com.ecom.model.Category;
import com.ecom.model.Coupons;
import com.ecom.model.OrderAudit;
import com.ecom.model.OrderRequest;
import com.ecom.model.Product;
import com.ecom.model.ProductOrder;
import com.ecom.model.Reviews;
import com.ecom.model.UserDtls;
import com.ecom.model.Wishlist;
import com.ecom.repository.ProductOrderRepository;
import com.ecom.repository.ReviewsRepository;
import com.ecom.service.CategoryService;
import com.ecom.service.CouponsService;
import com.ecom.service.OrderService;
import com.ecom.service.ProductService;
import com.ecom.service.ReviewsService;
import com.ecom.service.UserService;
import com.ecom.service.WishlistService;
import com.ecom.service.impl.OrderAuditService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;
import com.google.api.client.util.Value;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private CouponsService couponsService;
	@Autowired
	private Storage storage;

	@Value("${drapp-9da04.appspot.com}") // ✅ Inject the property value

	private String bucketName;

	@Autowired
	private ReviewsRepository reviewsRepository;
	@Autowired
	private ReviewsService reviewsService;
	@Autowired
	private WishlistService wishlistService;
	@Autowired
	private ProductOrderRepository orderRepository;
	@Autowired
	private CategoryService categoryService;
	@Autowired
	private UserService userService;
	@Autowired
	private CartService cartService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private CommonUtil commonUtils;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ProductService productService;

	@Autowired
	private OrderAuditService orderAuditService;

	@GetMapping("/")
	public String home() {
		return "user/home";
	}
	/*--------------------------------COUPONS--------------------------------        */

	@GetMapping("/orders/coupon/{code}")
	@ResponseBody
	public ResponseEntity<?> getCouponByCode(@PathVariable String code) {
		Optional<Coupons> coupon = couponsService.getCouponsByCode(code);

		if (coupon.isPresent()) {
			Coupons validCoupon = coupon.get();

			if (Boolean.FALSE.equals(validCoupon.getStatus())) {
				return ResponseEntity.ok(Map.of("valid", false, "message", "❌ Enter valid Coupon"));
			}

			if (validCoupon.getExpiryDate() != null && validCoupon.getExpiryDate().isBefore(LocalDate.now())) {
				return ResponseEntity.ok(Map.of("valid", false, "message", "⚠️ This coupon has expired!"));
			}

			// Default values for null data
			String name = validCoupon.getName() != null ? validCoupon.getName() : "Unknown Coupon";
			String applyOn = validCoupon.getCategory() != null ? validCoupon.getCategory() : "All Products"; // Rename
																												// Category
																												// to
			Integer id = validCoupon.getId();
			/// System.out.println(id);
			// ApplyOn
			int discount = validCoupon.getDiscount() != null ? validCoupon.getDiscount() : 0;
			Double minAmount = validCoupon.getMinAmount() != null ? validCoupon.getMinAmount() : 0;
			String expiryDate = validCoupon.getExpiryDate() != null ? validCoupon.getExpiryDate().toString()
					: "No Expiry Date";

			// System.out.println(applyOn);
			// Custom messages based on applyOn field
			String customMessage = switch (applyOn.toUpperCase()) {
			case "ELECTRONICS", "FURNITURE", "CLOTHING", "GROCCERY", "BEAUTYCARE", "SPORTSFITNESS", "ACCESSORIES",
					"TOYSGAMES", "BOOKSSTATIONERY", "APPLIANCES" ->
				"🛒 Great! You got " + discount + "% OFF on " + applyOn + "!";
			case "MIN_AMOUNT" -> "💰 Discount of " + discount + "% applies if order is above ₹" + minAmount;
			case "ALL" -> "🎉 Special Offer! Get " + discount + "% OFF on All Products!";
			default -> "✅ Coupon Applied! You got " + discount + "% discount!";
			};

			return ResponseEntity.ok(Map.of("valid", true, "code", validCoupon.getCode(), "discount", discount,
					"expiryDate", expiryDate, "name", name, "applyOn", applyOn, "id", id, // Changed from category to
																							// applyOn
					"minAmount", minAmount, "customMessage", customMessage // Add message here
			));
		}

		return ResponseEntity.ok(Map.of("valid", false, "message", "❌ Invalid Coupon Code!"));
	}

	/*--------------------------------COUPONS--------------------------------        */

	@PostMapping("/save-order")
	public String saveOrder(@ModelAttribute OrderRequest request, Principal p) throws Exception {

		UserDtls user = getLoggedInUserDetails(p);
		orderService.saveOrder(user.getId(), request);

		return "redirect:/user/success";
	}

	@GetMapping("/orders")
	public String orderPage(Principal p, Model m) {

		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		m.addAttribute("carts", carts);
		if (carts.size() > 0) {
			Double orderPrice = carts.get(carts.size() - 1).getTotalOrderPrice();

			Double tex = 00.0;// Example for 7% tax
			m.addAttribute("tex", tex);
			Double totalOrderPrice = carts.get(carts.size() - 1).getTotalOrderPrice() + tex + 50;
			m.addAttribute("orderPrice", orderPrice);
			m.addAttribute("totalOrderPrice", totalOrderPrice);
		}
		return "/user/order";
	}

	@GetMapping("/reviews/{pid}/{uid}/{oid}")
	public String getReviews(@PathVariable Integer pid, @PathVariable Integer uid, @PathVariable Integer oid,
			@RequestParam(defaultValue = "0") int pageNo, @RequestParam(defaultValue = "3") int pageSize, Model model) {

		ProductOrder productOrder = orderRepository.findByUserIdAndProductIdAndStatus(uid, pid,
				OrderStatus.DELIVERED.getName());
		if (productOrder == null) {

			return "redirect:/error";
		}

		// ✅ Fetch product details
		Product product = productService.getProductById(pid);
		model.addAttribute("product", product);

		// ✅ Fetch paginated reviews
		Page<Reviews> page = reviewsRepository.findByProductId(pid, PageRequest.of(pageNo, pageSize));
		List<Reviews> reviews = page.getContent();

		// ✅ Fetch all reviews (without pagination) to calculate global average rating
		List<Reviews> allReviews = reviewsRepository.findByProductId(pid);
		double totalRating = 0;
		int totalReviews = allReviews.size();

		model.addAttribute("totalReviews", totalReviews);
		// System.out.println(totalReviews);
		if (totalReviews > 0) {
			for (Reviews review : allReviews) {
				totalRating += review.getRating();
			}
		}

		double avgRating = (totalReviews > 0) ? (totalRating / totalReviews) : 0.0;
		model.addAttribute("avgRating", avgRating);

		// ✅ Add attributes to the model
		model.addAttribute("reviews", reviews);

		model.addAttribute("pageNo", page.getNumber());
		model.addAttribute("totalPages", page.getTotalPages());
		model.addAttribute("isFirst", page.isFirst());
		model.addAttribute("isLast", page.isLast());
		model.addAttribute("oid", oid);
		// ✅ Pass average rating to frontend

		return "user/reviews"; // ✅ Ensure this template exists!
	}

	@PostMapping("/saveReviews/{pid}/{oid}")
	public String saveReview(@PathVariable Integer pid, @PathVariable Integer oid, @RequestParam Integer rating,
			@RequestParam String title, @RequestParam String reviewText, HttpSession session, Principal p, Model m,
			@RequestParam("img") MultipartFile file) throws IOException {

		UserDtls user = getLoggedInUserDetails(p);
		ProductOrder productOrder = orderRepository.findByUserIdAndProductIdAndStatus(user.getId(), pid,
				OrderStatus.DELIVERED.getName());
		if (productOrder == null) {

			return "redirect:/error";
		}
		Product product = productService.getProductById(pid);

		if (user == null || product == null) {
			session.setAttribute("errorMsg", "Invalid user or product.");
			return "redirect:/user/reviews";
		}

		// Integer uid = user.getId();

		// ✅ Ensure storage is initialized
		if (file != null && !file.isEmpty()) {
			String bucketName = "drapp-9da04.appspot.com";
			String fileName = file.getOriginalFilename();

			// ✅ Upload file to Firebase Storage
			BlobId blobId = BlobId.of(bucketName, fileName);
			BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(file.getContentType()).build();
			storage.create(blobInfo, file.getBytes());

			// ✅ Generate URL
			String imageUrl = "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media".formatted(bucketName,
					fileName);

			// ✅ Save the review with image URL
			Reviews review = new Reviews(product, user, rating, title, reviewText, imageUrl);
			reviewsService.saveReview(review);
		} else {
			// ✅ Save review without image
			Reviews review = new Reviews(product, user, rating, title, reviewText, null);
			reviewsService.saveReview(review);
		}

		session.setAttribute("succMsg", "Review submitted successfully.");
		return "redirect:/user/audit-report/" + oid;
	}

	@GetMapping("/wishlistFromCart/{pid}/{uid}")
	public String wishlistFromCart(@PathVariable Integer pid, @PathVariable Integer uid, HttpSession session, Model m,
			Principal p) {

		cartService.removeFromCart(pid, uid);

		Wishlist saveWishlist = wishlistService.saveWishlist(pid, uid);
		if (ObjectUtils.isEmpty(saveWishlist)) {
			session.setAttribute("errorMsg", "Failed to add product to wishlist");
		} else {
			session.setAttribute("succMsg", "Product added to wishlist");
		}

		return "redirect:/user/cart";
	}

	@GetMapping("/addToCartFromWish")
	public String addToCartFromWish(@RequestParam Integer pid, @RequestParam Integer uid, HttpSession session) {
		Cart saveCart = cartService.saveCart(pid, uid);

		if (ObjectUtils.isEmpty(saveCart)) {
			session.setAttribute("errorMsg", "Product add to cart failed from WISHlist ");
		} else {
			session.setAttribute("succMsg", "Product added to cart from WISHlist");
		}
		// Remove the item from the wishlist
		wishlistService.deleteWishlistItem(uid, pid);

		return "redirect:/user/wishlist/" + uid;
	}

	@GetMapping("/wishlist/{uid}")
	public String viewWishlist(@PathVariable Integer uid, Model model, Principal principal) {
		// Ensure the user is logged in
		UserDtls user = getLoggedInUserDetails(principal);
		if (user == null || !user.getId().equals(uid)) {
			return "redirect:/signin"; // Redirect to login if the user is not authenticated
		}

		// Fetch the wishlist items for the user
		List<Wishlist> wishlistItems = wishlistService.getWishlistByUser(uid);

		model.addAttribute("wishlistItems", wishlistItems);
		model.addAttribute("user", user);

		return "user/wishlist"; // Ensure a Thymeleaf template named "wishlist.html" exists
	}

	@GetMapping("/wishlist/{pid}/{uid}")
	public String wishlist(@PathVariable Integer pid, @PathVariable Integer uid, HttpSession session,
			HttpServletRequest request) {

		boolean exists = wishlistService.isProductInWishlist(uid, pid);
		if (exists) {
			wishlistService.deleteWishlistItem(uid, pid);
			session.setAttribute("succMsg", "Product removed from wishlist");
		} else {
			Wishlist saveWishlist = wishlistService.saveWishlist(pid, uid);
			if (ObjectUtils.isEmpty(saveWishlist)) {
				session.setAttribute("errorMsg", "Failed to add product to wishlist");
			} else {
				session.setAttribute("succMsg", "Product added to wishlist");
			}
		}

		// Get referer URL and redirect back
		String referer = request.getHeader("Referer");
		return "redirect:" + (referer != null ? referer : "/products");
	}

	@GetMapping("/wishlist/remove/{pid}/{uid}")
	public String removeFromWishlist(@PathVariable Integer pid, @PathVariable Integer uid, HttpSession session,
			Model m) {
		// Remove the item from the wishlist
		wishlistService.deleteWishlistItem(uid, pid);

		// Optionally, you can add a success message or perform any other action
		session.setAttribute("succMsg", "Product removed from wishlist");

		// Redirect to the wishlist page or another page
		return "redirect:/user/wishlist/" + uid;
	}

	@GetMapping("/audit-report/{id}")
	public String auditReport(@PathVariable Integer id, Principal p, Model m, HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		if (user != null) {
			// Fetch the product order by ID (assuming you have a service method for that)
			ProductOrder order = orderService.getProductOrderById(id);
			// Check if a review exists for each product

			if (order != null) {
				m.addAttribute("order", order);
				List<OrderAudit> orderAudits = orderAuditService.getOrderAuditsByProductOrderId(id);
				m.addAttribute("orderAudits", orderAudits);

				// Calculate expected delivery date (7 days after current date)
				LocalDate expectedDeliveryDate = LocalDate.now().plusDays(7);
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				String formattedExpectedDeliveryDate = expectedDeliveryDate.format(formatter);
				m.addAttribute("formattedExpectedDeliveryDate", formattedExpectedDeliveryDate);

				boolean showDeliveryDateButton = orderAudits.stream().anyMatch(
						audit -> List.of("IN_PROGRESS", "PRODUCT_PACKED", "ORDER_RECEIVED", "OUT_FOR_DELIVERY")
								.contains(audit.getStatus()))
						&& orderAudits.stream().noneMatch(audit -> List
								.of("DELIVERED", "CANCELLED", "RETURNED", "CANCELED").contains(audit.getStatus()));

				m.addAttribute("showDeliveryDateButton", showDeliveryDateButton);

				// Check if review exists
				boolean reviewExists = reviewsService.existsByProductIdAndUserId(order.getProduct().getId(),
						user.getId());
				// System.out.println(reviewExists);

				m.addAttribute("reviewExists", reviewExists);

				Reviews review = reviewsService.findByProductIdAndUserId(order.getProduct().getId(), user.getId());
				if (review != null) {
					m.addAttribute("review", review);
				}

				boolean showReturnDoneButton = orderAudits.stream()
						.anyMatch(audit -> "RETURN_PROCCESING".equals(audit.getStatus())
								&& !orderAudits.stream().anyMatch(a -> List.of("RETURNED").contains(a.getStatus())));

				m.addAttribute("showReturnDoneButton", showReturnDoneButton);

			}
		}
		return "user/audit_report";
	}

	@PostMapping("/delete-review/{productId}/{userId}")
	public String deleteReview(@PathVariable Integer productId, HttpServletRequest request,
			@PathVariable Integer userId, Model model) {
		boolean isDeleted = reviewsService.deleteReviewByProductAndUser(productId, userId);

		if (isDeleted) {
			model.addAttribute("succMsg", "Review deleted successfully.");
		} else {
			model.addAttribute("errorMsg", "Review not found or already deleted.");
		}

		// Redirect back to the previous page using referer
		String referer = request.getHeader("Referer");
		return "redirect:" + (referer != null ? referer : "/user-orders");
	}

	@GetMapping("/save-return-order/{orderId}")
	public String saveReturnOrder(@PathVariable String orderId, @ModelAttribute OrderRequest request, Principal p)
			throws Exception {

		UserDtls user = getLoggedInUserDetails(p);
		orderService.saveReturnOrder(orderId, user.getId(), request);
		return "user/return_success"; // Redirect to return success page
	}

	@GetMapping("/return-order/{orderId}")
	public String returnOrder(@PathVariable String orderId, Principal p, Model m, HttpSession session) {
		// Fetch order details by ID
		UserDtls user = getLoggedInUserDetails(p);
		if (user != null) {
			ProductOrder productOrder = orderService.getOrderId(orderId); // Correct usage of
			// orderId
			// ProductOrder productOrder = new ProductOrder();

			// productOrder.setPrice(22.22);
			m.addAttribute("order", productOrder); // Adding the order to the model

			productOrder.getOrderId();
			productOrder.getPrice();
			productOrder.getOrderAddress().getAddress();
			productOrder.getOrderAddress().getFirstName();
			productOrder.getOrderAddress().getLastName();
			productOrder.getOrderAddress().getCity();
			productOrder.getOrderAddress().getPincode();
			productOrder.getOrderAddress().getState();
			productOrder.getStatus();
			productOrder.getQuantity();
			/*
			 * productOrder.getProduct().getImage(); productOrder.getProduct().getTitle();
			 * productOrder.getProduct().getDescription();
			 */
			productOrder.getStatus();

		}
		return "user/return_order"; // Make sure the path corresponds to the actual location of the .html file
	}

	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {

		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user", userDtls);
			Integer countCart = cartService.getCountCart(userDtls.getId());
			Integer countWishlist = wishlistService.getCountWishlist(userDtls.getId());
			m.addAttribute("countWishlist", countWishlist);
			m.addAttribute("countCart", countCart);
			m.addAttribute("uid", userDtls.getId());
			m.addAttribute("user", userDtls);
		}
		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);

	}

	@GetMapping("/addCart")
	public Object addToCart(@RequestParam Integer pid, @RequestParam Integer uid, HttpSession session,
			HttpServletRequest request) {
		Cart saveCart = cartService.saveCart(pid, uid);

		if (ObjectUtils.isEmpty(saveCart)) {
			session.setAttribute("errorMsg", "Product add to cart failed");
		} else {
			session.setAttribute("succMsg", "Product added to cart");
		}
		return "redirect:/product/" + pid;
	}

	@GetMapping("/addCartFromBuy")
	public String addToCartFromBuy(@RequestParam Integer pid, @RequestParam Integer uid, HttpSession session) {
		Product p = productService.getProductById(pid);
		Integer stock = p.getStock();

		// Check if stock is available
		if (stock > 0) {
			// Save the cart and assign the result to a variable
			Cart savedCart = cartService.saveCart(pid, uid);

			// Check if saving the cart was successful
			if (savedCart == null) {
				session.setAttribute("errorMsg", "Product add to cart failed");
			} else {
				session.setAttribute("succMsg", "Product added to cart");
			}
		} else {
			session.setAttribute("errorMsg", "Product is out of stock");
		}

		// Redirect back to the product page
		return "redirect:/user/purchasing/" + pid + '/' + uid;
	}

	@GetMapping("/cart")
	public String loadCartPage(Principal p, Model m) {

		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		m.addAttribute("carts", carts);
		if (carts.size() > 0) {
			Double totalOrderPrice = carts.get(carts.size() - 1).getTotalOrderPrice();
			m.addAttribute("totalOrderPrice", totalOrderPrice);
		}
		return "/user/cart";
	}

	@GetMapping("/cartQuantityUpdate")
	public String updateCartQuantity(@RequestParam String sy, @RequestParam Integer cid) {
		cartService.updateQuantity(sy, cid);
		return "redirect:/user/cart";
	}

	private UserDtls getLoggedInUserDetails(Principal p) {
		String email = p.getName();
		UserDtls userDtls = userService.getUserByEmail(email);
		return userDtls;
	}

	@GetMapping("/purchasing/{id}/{uid}")
	public String purchasingNow(@PathVariable Integer id, Principal p, Model m) {
		UserDtls user = getLoggedInUserDetails(p);
		// ✅ Fetch product from database
		Product product = productService.getProductById(id);

		if (product == null) {
			return "redirect:/error"; // Handle case if product is not found
		}
		// ✅ Get the price correctly
		Double discountPrice = product.getDiscountPrice();

		Double totalOrderPrice = discountPrice;
		// ✅ Add attributes to the model
		m.addAttribute("product", product);
		m.addAttribute("orderRequest", new OrderRequest());
		m.addAttribute("discountPrice", discountPrice);
		m.addAttribute("totalOrderPrice", totalOrderPrice);
		Double tax = 0.0; // Example: You can calculate based on your tax logic
		m.addAttribute("tax", tax);

		// System.out.println(user);
		m.addAttribute("user", user);
		// System.out.println("Product: " + product);
		return "/user/buynow";
	}

	@PostMapping("/purchase")
	public String purchase(@RequestParam Integer pid, @RequestParam UserDtls uid, @ModelAttribute OrderRequest request,
			Principal p) throws Exception {

		Product product = productService.getProductById(pid);

		orderService.purchaseOrder(product, uid, request);
		return "redirect:/user/success";
	}

	@GetMapping("/success")
	public String loadSuccuss() {
		return "/user/success";
	}

	@GetMapping("/user-orders")
	public String myOrders(Model m, Principal p) {
		UserDtls loginUser = getLoggedInUserDetails(p);

		List<ProductOrder> orders = orderService.getOrdersByUser(loginUser.getId());

		m.addAttribute("orders", orders);

		return "/user/my_orders";
	}

	@GetMapping("/update-status")
	public String updateOrderStatus(@RequestParam Integer id, @RequestParam Integer st, HttpSession session) {

		OrderStatus[] values = OrderStatus.values();
		String status = null;

		for (OrderStatus orderSt : values) {
			if (orderSt.getId().equals(st)) {
				status = orderSt.getName();
			}
		}

		ProductOrder updateOrder = orderService.updateOrderStatus(id, status);

		try {
			commonUtils.sendMailForProductOrder(updateOrder, status);
		} catch (Exception e) {
			e.printStackTrace();
		}

		OrderAudit orderAudit = new OrderAudit();
		orderAudit.setProductOrder(updateOrder); // Use the updated order
		orderAudit.setStatus(updateOrder.getStatus()); // The new status
		orderAudit.setStatusDate(LocalDateTime.now()); // The date and time when the status was updated

		// Save the OrderAudit entry
		orderRepository.save(orderAudit);

		if (!ObjectUtils.isEmpty(updateOrder)) {
			session.setAttribute("succMsg", "Status Updated");
		} else {
			session.setAttribute("errorMsg", "status not updated");
		}
		return "redirect:/user/user-orders";
	}

	@GetMapping("/profile")
	public String profile() {
		return "/user/profile";
	}

	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img, HttpSession session)
			throws IOException {
		UserDtls updateUserProfile = userService.updateUserProfile(user, img);
		if (ObjectUtils.isEmpty(updateUserProfile)) {
			session.setAttribute("errorMsg", "Profile not updated");

		} else {
			session.setAttribute("succMsg", "Profile Updated");
		}
		return "redirect:/user/profile";
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam String newPassword, @RequestParam String currentPassword, Principal p,
			HttpSession session) {
		UserDtls loggedInUserDetails = getLoggedInUserDetails(p);
		Boolean matches = passwordEncoder.matches(currentPassword, loggedInUserDetails.getPassword());

		if (matches) {
			String encodePassword = passwordEncoder.encode(newPassword);
			loggedInUserDetails.setPassword(encodePassword);
			UserDtls updateUser = userService.updateUser(loggedInUserDetails);
			if (ObjectUtils.isEmpty(updateUser)) {
				session.setAttribute("errorMsg", "Something want wrong");
			} else {
				session.setAttribute("succMsg", "Password Updated Succesfully");
			}
		} else {
			session.setAttribute("errorMsg", "Invalid Current Password");
		}
		return "redirect:/user/profile";
	}

}