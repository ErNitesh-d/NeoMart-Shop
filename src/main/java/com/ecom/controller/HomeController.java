
package com.ecom.controller;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.CartService;
import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.Reviews;
import com.ecom.model.UserDtls;
import com.ecom.repository.ReviewsRepository;
import com.ecom.service.CategoryService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.service.WishlistService;
import com.ecom.util.CommonUtil;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

	@Autowired
	private ReviewsRepository reviewsRepository;
	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ProductService productService;

	@Autowired
	private UserService userService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private WishlistService wishlistService;

	@Autowired
	private CartService cartService;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {

		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user", userDtls);
			Integer countCart = cartService.getCountCart(userDtls.getId());
			m.addAttribute("countCart", countCart);
			Integer countWishlist = wishlistService.getCountWishlist(userDtls.getId());
			m.addAttribute("countWishlist", countWishlist);
			m.addAttribute("uid", userDtls.getId());
			m.addAttribute("user", userDtls);
		}

		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);
	}

	@GetMapping("/")
	public String index(Model m) {

		List<Category> allActiveCategory = categoryService.getAllActiveCategory().stream()
				.sorted((c1, c2) -> c2.getId().compareTo(c1.getId())).limit(10).toList();
		List<Product> allActiveProducts = productService.getAllActiveProducts("").stream()
				.sorted((p1, p2) -> p2.getId().compareTo(p1.getId())).limit(8).toList();

		m.addAttribute("category", allActiveCategory);
		m.addAttribute("products", allActiveProducts);

		return "index";
	}

	@GetMapping("/signin")
	public String login() {
		return "login";
	}

	@GetMapping("/register")
	public String register() {
		return "register";
	}

	@GetMapping("/terms&conditions")
	public String termsCon() {
		return "terms&conditions";
	}

	@GetMapping("/privacy-policy")
	public String policy() {
		return "privacy-policy";
	}

	@GetMapping("/about-neomart")
	public String about() {
		return "about-us";
	}
	/*
	 * @SuppressWarnings("deprecation")
	 * 
	 * @GetMapping("/products") public String products(Model m, @RequestParam(value
	 * = "category", defaultValue = "") String category,
	 * 
	 * @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
	 * 
	 * @RequestParam(name = "pageSize", defaultValue = "4") Integer pageSize,
	 * 
	 * @RequestParam(defaultValue = "") String ch) {
	 * 
	 * // System.out.println("CATEGORY="+category); List<Category> categories =
	 * categoryService.getAllActiveCategory(); m.addAttribute("paramValue",
	 * category); m.addAttribute("categories", categories);
	 * 
	 * Page<Product> page = null;
	 * 
	 * List<Product> products = productService.getAllActiveProducts(category);
	 * m.addAttribute("products", products);
	 * 
	 * if (StringUtils.isEmpty(ch)) { page =
	 * productService.getAllActiveProductPagination(pageNo, pageSize, category); }
	 * else { page = productService.searchActiveProductPagination(pageNo, pageSize,
	 * category, ch); }
	 * 
	 * List<Product> products = page.getContent(); m.addAttribute("products",
	 * products); m.addAttribute("productsSize", products.size());
	 * 
	 * m.addAttribute("pageNo", page.getNumber()); m.addAttribute("totalElements",
	 * page.getTotalElements()); m.addAttribute("pageSize", pageSize);
	 * m.addAttribute("totalPages", page.getTotalPages()); m.addAttribute("isFirst",
	 * page.isFirst()); m.addAttribute("isLast", page.isLast());
	 * 
	 * return "product"; }
	 */

	@GetMapping("/products")
	public String products(Model m, Principal p, @RequestParam(defaultValue = "") String category,
			@RequestParam(defaultValue = "0") Integer pageNo, @RequestParam(defaultValue = "16") Integer pageSize,
			@RequestParam(defaultValue = "") String ch) {

		// Fetch all categories to display in the UI
		List<Category> categories = categoryService.getAllActiveCategory();
		m.addAttribute("paramValue", category);
		m.addAttribute("categories", categories);

		// Fetch products based on search term or category
		Page<Product> page;
		List<Product> products;
		if (ObjectUtils.isEmpty(ch)) {
			page = productService.getAllActiveProductPagination(pageNo, pageSize, category);
		} else {
			page = productService.searchActiveProductPagination(pageNo, pageSize, category, ch);
		}
		products = page.getContent(); // Get the content of the page

		// Add product data and pagination details to the model
		m.addAttribute("products", products);
		m.addAttribute("productsSize", products.size());
		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		// Get the logged-in user if available
		UserDtls user = null;
		if (p != null) {
			user = getLoggedInUserDetails(p); // Only call this if Principal is not null
		}
		m.addAttribute("user", user);

		// If user is logged in, check wishlist status for each product
		if (user != null) {
			Map<Integer, Boolean> wishlistStatus = new HashMap<>();
			for (Product product : products) {
				boolean isInWishlist = wishlistService.isProductInWishlist(user.getId(), product.getId());
				wishlistStatus.put(product.getId(), isInWishlist);
			}
			m.addAttribute("wishlistStatus", wishlistStatus); // Pass wishlist status to model
		} else {
			m.addAttribute("wishlistStatus", new HashMap<>()); // Empty map for non-logged-in users
		}

		return "product"; // Return the view name for rendering
	}

	private UserDtls getLoggedInUserDetails(Principal p) {
		String email = p.getName();
		UserDtls userDtls = userService.getUserByEmail(email);
		return userDtls;
	}

	@GetMapping("/api/products/suggestions")
	@ResponseBody
	public List<String> getSuggestions(@RequestParam String query) {
		return productService.getSuggestions(query).stream().map(Product::getTitle).toList();
	}

	@GetMapping("/search")
	public String search(Model m, @RequestParam(defaultValue = "") String category,
			@RequestParam(defaultValue = "0") Integer pageNo, @RequestParam(defaultValue = "1000") Integer pageSize,
			@RequestParam(defaultValue = "") String ch, @RequestParam(required = false) Integer uid, // User
																										// ID
			@RequestParam(required = false) Integer pid) // Product ID
	{
		// Fetch all categories to display in the UI
		List<Category> categories = categoryService.getAllActiveCategory();
		m.addAttribute("paramValue", category);
		m.addAttribute("categories", categories);

		Page<Product> page = null;
		List<Product> products = new ArrayList<>();

		// If search term is provided, search in Elasticsearch, otherwise search in
		// MySQL
		if (ObjectUtils.isEmpty(ch)) {
			// Query MySQL for active products with pagination
			page = productService.getAllActiveProductPagination(pageNo, pageSize, category);
			products = page.getContent(); // Get the content from the page
		} else {
			// Query Elasticsearch for products based on search term (ch) and pagination
			page = productService.searchActiveProductPagination(pageNo, pageSize, category, ch);
			products = page.getContent(); // Get the content from the page
		}

		Map<Integer, Boolean> wishlistStatus = new HashMap<>();
		if (uid != null) {
			for (Product product : products) {
				wishlistStatus.put(product.getId(), wishlistService.isProductInWishlist(uid, product.getId()));
			}
		}
		m.addAttribute("wishlistStatus", wishlistStatus);

		m.addAttribute("products", products);
		m.addAttribute("productsSize", products.size());
		m.addAttribute("wishlistStatus", wishlistStatus); // Add wishlist status to model

		// Add pagination details to the model
		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "product";
	}

	@GetMapping("/product/{id}")
	public String product(@PathVariable int id, Model m, @RequestParam(defaultValue = "0") int pageNo,
			@RequestParam(defaultValue = "2") int pageSize) {

		Product productById = productService.getProductById(id);
		m.addAttribute("product", productById);

		Page<Reviews> page = reviewsRepository.findByProductId(id, PageRequest.of(pageNo, pageSize));
		List<Reviews> reviews = page.getContent();

		if (!reviews.isEmpty()) {
			List<Reviews> allReviews = reviewsRepository.findByProductId(id);
			double totalRating = 0;
			int totalReviews = allReviews.size();

			m.addAttribute("totalReviews", totalReviews);

			Map<Integer, Integer> ratingCounts = new HashMap<>();

			for (int i = 1; i <= 5; i++) {
				ratingCounts.put(i, 0); // ✅ Initialize all ratings with 0 count
			}

			for (Reviews review : allReviews) {
				ratingCounts.put(review.getRating(), ratingCounts.get(review.getRating()) + 1);
				totalRating += review.getRating();
			}

			Map<Integer, Double> ratingCountsPercentage = new HashMap<>();
			for (int i = 1; i <= 5; i++) {
				int count = ratingCounts.getOrDefault(i, 0);
				double percent = (totalReviews > 0) ? (count * 100.0 / totalReviews) : 0.0;
				ratingCountsPercentage.put(i, percent);
			}
			m.addAttribute("ratingCountsPercentage", ratingCountsPercentage);

			// System.out.println("Total Reviews: " + totalReviews);
			// System.out.println("Rating Counts: " + ratingCounts);
			// System.out.println("ratingCountsPercentage: " + ratingCountsPercentage);

			double avgRating = (totalReviews > 0) ? (totalRating / totalReviews) : 0.0;
			m.addAttribute("avgRating", avgRating);
			m.addAttribute("ratingCounts", ratingCounts); // ✅ Ensure it has all keys

			m.addAttribute("reviews", reviews);
		}

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "view_product";
	}

	@PostMapping("/saveUser")
	public String saveUser(@ModelAttribute UserDtls user, @RequestParam("img") MultipartFile file, HttpSession session)
			throws IOException {

		Boolean existsEmail = userService.existsEmail(user.getEmail());

		if (existsEmail) {
			session.setAttribute("errorMsg", "User with this email already registered");
		}

		else {
			String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
			user.setProfleImage(imageName);
			UserDtls saveUser = userService.saveUser(user);

			if (!ObjectUtils.isEmpty(saveUser)) {
				if (!file.isEmpty()) {
					File saveFile = new ClassPathResource("static/img").getFile();

					Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator
							+ file.getOriginalFilename());

					// System.out.println(path);
					Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				}
				session.setAttribute("succMsg", "Registeration Successfull ");

			} else {
				session.setAttribute("errorMsg", "Something wrong on server");
			}
		}

		return "redirect:/register";

	}

//----------Forgot Password-------
	@GetMapping("/forgot-password")
	public String showForgotPassword() {
		return "/forgot_password.html";
	}

	@PostMapping("/forgot-password")
	public String processForgotPassword(@RequestParam String email, HttpSession session, HttpServletRequest request)
			throws UnsupportedEncodingException, MessagingException {
		UserDtls userByEmail = userService.getUserByEmail(email);
		if (ObjectUtils.isEmpty(userByEmail)) {
			session.setAttribute("errorMsg", "Please enter valid email id.");

		} else {
			String resetToken = UUID.randomUUID().toString();
			userService.updateUserResetToken(email, resetToken);

			// Generate URL :
			// http://localhost:8080/reset-password?token=sfgdbgfswegfbdgfewgvsrg

			String url = CommonUtil.generateUrl(request) + "/reset-password?token=" + resetToken;

			try {
				Boolean sendMail = commonUtil.sendMail(url, email);
				if (sendMail) {
					session.setAttribute("succMsg", "Please check your mail. | Password reset link sended ");
				}
			} catch (Exception e) {

				session.setAttribute("errorMsg", "Something wrong on server mail not send        " + e);
			}
		}
		return "redirect:/forgot-password";

	}

	@GetMapping("/reset-password")
	public String showResetPassword(@RequestParam String token, HttpSession session, Model m) {

		UserDtls userByToken = userService.getUserByToken(token);

		if (userByToken == null) {
			m.addAttribute("errorMsg", "Your link is invalid or expired !!");
			return "message";
		}
		m.addAttribute("token", token);
		return "reset_password";
	}

	@PostMapping("/reset-password")
	public String resetPassword(@RequestParam String token, @RequestParam String password, HttpSession session,
			Model m) {

		UserDtls userByToken = userService.getUserByToken(token);
		if (userByToken == null) {
			m.addAttribute("errorMsg", "Your link is invalid or expired !!");
			return "message";
		} else {
			userByToken.setPassword(passwordEncoder.encode(password));
			userByToken.setResetToken(null);
			userService.updateUser(userByToken);
			// session.setAttribute("succMsg", "Password change successfully");
			m.addAttribute("msg", "Password changed successfully");

			return "message";
		}

	}

	@GetMapping("/search-product")
	public String searchProduct(@RequestParam String ch, Model m, HttpSession session) {

		List<Product> searchProducts = productService.searchProduct(ch);
		m.addAttribute("products", searchProducts);

		List<Category> categories = categoryService.getAllActiveCategory();
		m.addAttribute("categories", categories);

		return "/product";
	}
}
