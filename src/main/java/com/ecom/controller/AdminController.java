package com.ecom.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecom.model.CartService;
import com.ecom.model.Category;
import com.ecom.model.Coupons;
import com.ecom.model.OrderAudit;
import com.ecom.model.Product;
import com.ecom.model.ProductElastic;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.repository.ElasticSearchQuery;
import com.ecom.repository.ProductOrderRepository;
import com.ecom.service.CategoryService;
import com.ecom.service.CouponsService;
import com.ecom.service.OrderService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private CouponsService couponsService;

	@Autowired
	private ElasticsearchClient elasticsearchClient;
	@Autowired
	private ElasticSearchQuery elasticSearchQuery;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ProductOrderRepository orderRepository;
	@Autowired
	private ProductService productService;

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

	@GetMapping("/")
	public String index(Model m) {

		List<Category> allActiveCategory = categoryService.getAllActiveCategory().stream()
				.sorted((c1, c2) -> c2.getId().compareTo(c1.getId())).limit(6).toList();

		List<Product> allActiveProducts = productService.getAllActiveProducts("").stream()
				.sorted((p1, p2) -> p2.getId().compareTo(p1.getId())).toList(); // Removed .limit(8) for better testing

		// Fetch users with ROLE_USER
		List<UserDtls> allUsers = userService.getAllUsers(); // Fetch all users
		long userCount = allUsers.stream().filter(user -> "ROLE_USER".equals(user.getRole())) // Filter ROLE_USER
				.count();

		// Fetch all product orders
		List<ProductOrder> allOrders = orderService.getAllOrders();

		// Calculate Total Revenue from Delivered Orders (Handle Null Values)
		double totalRevenue = allOrders.stream().filter(order -> "DELIVERED".equals(order.getStatus()))
				.mapToDouble(order -> order.getTotalPrice() != null ? order.getTotalPrice() : 0.0) // Handle null
				.sum();

		totalRevenue = Double.parseDouble(String.format("%.2f", totalRevenue));
		// Get the current and previous month
		LocalDate currentDate = LocalDate.now();

		YearMonth currentMonth = YearMonth.from(currentDate);
		YearMonth previousMonth = currentMonth.minusMonths(1);

		// Get the first and last day of the current & previous months
		LocalDate firstDayOfCurrentMonth = currentMonth.atDay(1);
		LocalDate lastDayOfCurrentMonth = currentMonth.atEndOfMonth();
		LocalDate firstDayOfPreviousMonth = previousMonth.atDay(1);
		LocalDate lastDayOfPreviousMonth = previousMonth.atEndOfMonth();
		// Get Current & Previous Year

		// Separate lists for previous and current month orders
		List<ProductOrder> previousMonthOrders = new ArrayList<>();
		List<ProductOrder> currentMonthOrders = new ArrayList<>();

		// Filter orders separately for the current & previous month
		for (ProductOrder order : allOrders) {
			if (order.getOrderDate() != null) {
				LocalDate orderDate = order.getOrderDate();

				if (!orderDate.isBefore(firstDayOfPreviousMonth) && !orderDate.isAfter(lastDayOfPreviousMonth)) {
					previousMonthOrders.add(order);
				} else if (!orderDate.isBefore(firstDayOfCurrentMonth) && !orderDate.isAfter(lastDayOfCurrentMonth)) {
					currentMonthOrders.add(order);
				}
			}
		}

		// Count returned orders for the current month
		long currentMonthReturnedOrders = currentMonthOrders.stream()
				.filter(order -> "RETURNED".equals(order.getStatus())).count();

		long currentMonthTotalOrders = currentMonthOrders.size();

		// Calculate Customer Retention Rate (avoid division by zero)
		double customerRetentionRate = currentMonthTotalOrders > 0
				? ((double) currentMonthReturnedOrders / currentMonthTotalOrders) * 100
				: 0.0;

		// Format the retention rate to 2 decimal places
		customerRetentionRate = Math.round(customerRetentionRate * 100.0) / 100.0;
		// System.out.println(customerRetentionRate);
		// Count returned orders for the current month
		long PrevMonthReturnedOrders = previousMonthOrders.stream()
				.filter(order -> "RETURNED".equals(order.getStatus())).count();

		long PrevMonthTotalOrders = previousMonthOrders.size();

		// Calculate Customer Retention Rate (avoid division by zero)
		double customerRetentionRatePrev = PrevMonthTotalOrders > 0
				? ((double) PrevMonthReturnedOrders / PrevMonthTotalOrders) * 100
				: 0.0;

		// Format the retention rate to 2 decimal places
		customerRetentionRatePrev = Math.round(customerRetentionRatePrev * 100.0) / 100.0;
		// System.out.println(customerRetentionRatePrev);

		double returnOrderChange = customerRetentionRate - customerRetentionRatePrev;
		// returnOrderChange = returnOrderChange * -1;

		returnOrderChange = Double.parseDouble(String.format("%.2f", returnOrderChange));

		m.addAttribute("currentMonthReturnedOrders", currentMonthReturnedOrders);
		m.addAttribute("currentMonthTotalOrders", currentMonthTotalOrders);
		m.addAttribute("PrevMonthReturnedOrders", PrevMonthReturnedOrders);
		m.addAttribute("PrevMonthTotalOrders", PrevMonthTotalOrders);

		// System.out.println("Return Order Change: " + returnOrderChange + "%");
		m.addAttribute("returnOrderChange", returnOrderChange);
		// CURRENtMONTH Calculate Total Revenue from Orders (Handle Null
		// Values)
		double CurrentMonthorderTotalPRice = currentMonthOrders.stream()
				.mapToDouble(order -> order.getTotalPrice() != null ? order.getTotalPrice() : 0.0) // Handle null
				.sum();

		Double avgOrderValue = CurrentMonthorderTotalPRice / currentMonthTotalOrders;

		m.addAttribute("avgOrderValue", avgOrderValue);

		avgOrderValue = Double.parseDouble(String.format("%.2f", avgOrderValue));

		// PREVIOUSMONTH Calculate Total Revenue from Delivered Orders (Handle Null
		// Values)
		double PrveiousMonthorderTotalPRice = previousMonthOrders.stream()
				.mapToDouble(order -> order.getTotalPrice() != null ? order.getTotalPrice() : 0.0) // Handle null
				.sum();

		Double PrevMonthAvgOrderValue = PrveiousMonthorderTotalPRice / PrevMonthTotalOrders;
		m.addAttribute("avgOrderValue", avgOrderValue);
		PrevMonthAvgOrderValue = Double.parseDouble(String.format("%.2f", PrevMonthAvgOrderValue));
		m.addAttribute("PrevMonthAvgOrderValue", PrevMonthAvgOrderValue);
		PrevMonthAvgOrderValue = Double.parseDouble(String.format("%.2f", PrevMonthAvgOrderValue));

		double percentageChange = 0.0;

		if (PrevMonthAvgOrderValue != 0) { // Divide by zero ટાળવા માટે
			percentageChange = ((avgOrderValue - PrevMonthAvgOrderValue) / PrevMonthAvgOrderValue) * 100;
		}

		// Decimal Format માટે
		percentageChange = Double.parseDouble(String.format("%.2f", percentageChange));

		LocalDate now = LocalDate.now(); // Get current date

		// Previous month calculation
		LocalDate previousMonthDate = now.minusMonths(1);
		previousMonthDate.getMonth();

		// Filter new users created in the current month
		List<UserDtls> currMonthCustomers = allUsers.stream().filter(user -> "ROLE_USER".equals(user.getRole()))
				.filter(user -> {
					LocalDate userCreationDate = user.getJoinedDate();
					return YearMonth.from(userCreationDate).equals(currentMonth);
				}).collect(Collectors.toList());

		// Filter new users created in the previous month
		List<UserDtls> previousMonthCustomers = allUsers.stream().filter(user -> "ROLE_USER".equals(user.getRole()))
				.filter(user -> {
					LocalDate joinedDate = user.getJoinedDate();
					return YearMonth.from(joinedDate).equals(previousMonth);
				}).collect(Collectors.toList());

		// Count new users
		long newCustomerCount = currMonthCustomers.size();
		long oldCustomerCount = previousMonthCustomers.size();
		m.addAttribute("newCustomerCount", newCustomerCount);
		m.addAttribute("oldCustomerCount", oldCustomerCount);
		// Calculate Growth Percentage
		double customerGrowth = 0;
		if (oldCustomerCount > 0) {
			customerGrowth = ((double) (newCustomerCount - oldCustomerCount) / oldCustomerCount) * 100;
		}

		customerGrowth = Double.parseDouble(String.format("%.2f", customerGrowth));
		m.addAttribute("customerGrowth", customerGrowth);

		m.addAttribute("newCustomerCount", newCustomerCount);
		m.addAttribute("avgOrderValue", avgOrderValue);
		m.addAttribute("percentageChange", percentageChange);
		m.addAttribute("customerRetentionRate", customerRetentionRate);
		m.addAttribute("allOrders", allOrders.size());

		// Add attributes to model
		m.addAttribute("category", allActiveCategory);
		m.addAttribute("products", allActiveProducts);
		m.addAttribute("productCount", allActiveProducts != null ? allActiveProducts.size() : 0);
		m.addAttribute("userCount", userCount);
		m.addAttribute("totalRevenue", totalRevenue); // Add total revenue

		// ------------ SALES OVERVIEW SECTION----------------------------------
		// Calculate Total Revenue from Delivered---------------------------------
		// Orders (Handle Null
		// Values)---------------------------------------------------------------------------------

		double totalMonthlyRevenue = currentMonthOrders.stream().filter(order -> "DELIVERED".equals(order.getStatus()))
				.mapToDouble(order -> order.getTotalPrice() != null ? order.getTotalPrice() : 0.0) // Handle null
				.sum();
		totalMonthlyRevenue = Double.parseDouble(String.format("%.2f", totalMonthlyRevenue));
		// System.out.println(totalMonthlyRevenue);
		m.addAttribute("totalMonthlyRevenue", totalMonthlyRevenue);

		double totalPrevMonthlyRevenue = previousMonthOrders.stream()
				.filter(order -> "DELIVERED".equals(order.getStatus()))
				.mapToDouble(order -> order.getTotalPrice() != null ? order.getTotalPrice() : 0.0) // Handle null
				.sum();
		// System.out.println(totalPrevMonthlyRevenue);
		m.addAttribute("totalPrevMonthlyRevenue", totalPrevMonthlyRevenue);
		double monthGrowth = ((totalMonthlyRevenue - totalPrevMonthlyRevenue) / totalPrevMonthlyRevenue) * 100;
		monthGrowth = Double.parseDouble(String.format("%.2f", monthGrowth));
		// System.out.println(monthGrowth);

		// ------------------------------------------- SALES OVERVIEW SECTION (ORDER
		// COUNT)---------------------------------------------------------------------------------------
		// ----------------------------------

		long totalMonthlyOrders = currentMonthOrders.size();
		long totalPrevMonthlyOrders = previousMonthOrders.size();

		m.addAttribute("totalMonthlyOrders", totalMonthlyOrders);
		m.addAttribute("totalPrevMonthlyOrders", totalPrevMonthlyOrders);

		double monthOrderGrowth = ((totalMonthlyOrders - totalPrevMonthlyOrders) / (double) totalPrevMonthlyOrders)
				* 100;

		monthOrderGrowth = Double.parseDouble(String.format("%.2f", monthOrderGrowth));

		// 20% નફો ગણતરી
		double currentMonthProfit = (totalMonthlyRevenue * 20) / 100;
		double prevMonthProfit = (totalPrevMonthlyRevenue * 20) / 100;

		// નફો તફાવત (Growth)
		double profitGrowth = ((currentMonthProfit - prevMonthProfit) / prevMonthProfit) * 100;
		profitGrowth = Double.parseDouble(String.format("%.2f", profitGrowth));

		/*
		 * System.out.println("Current Month Profit: " + currentMonthProfit);
		 * System.out.println("Previous Month Profit: " + prevMonthProfit);
		 * System.out.println("Profit Growth: " + profitGrowth + "%");
		 */

		m.addAttribute("currentMonthProfit", currentMonthProfit);
		m.addAttribute("prevMonthProfit", prevMonthProfit);

		m.addAttribute("profitGrowth", profitGrowth);
		m.addAttribute("totalMonthlyRevenue", totalMonthlyRevenue);
		m.addAttribute("monthGrowth", monthGrowth);

		m.addAttribute("totalMonthlyOrders", totalMonthlyOrders);
		m.addAttribute("monthOrderGrowth", monthOrderGrowth);

		// Map to store sales data (Month -> Total Sales)
		Map<YearMonth, Double> monthlySales = new HashMap<>();

		// Iterate over all orders and calculate sales per month
		for (ProductOrder order : allOrders) {
			if (order.getOrderDate() != null) {
				YearMonth orderMonth = YearMonth.from(order.getOrderDate());
				double orderTotal = order.getTotalPrice() != null ? order.getTotalPrice() : 0.0;

				// Aggregate sales per month
				monthlySales.put(orderMonth, monthlySales.getOrDefault(orderMonth, 0.0) + orderTotal);
			}
		}

		/*--------------------------------Sales Overview----------------*/
		Map<String, Double> formattedSales = new LinkedHashMap<>();

		// Define all months (Jan to Dec) with initial sales as 0.0
		for (Month month : Month.values()) {
			formattedSales.put(month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), 0.0);
		}

		// Update sales values from the input map
		monthlySales.entrySet().stream().sorted(Map.Entry.comparingByKey()) // Sort by YearMonth
				.forEach(entry -> {
					YearMonth yearMonth = entry.getKey(); // Get YearMonth object
					Month month = yearMonth.getMonth(); // Extract Month from YearMonth
					String monthName = month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

					formattedSales.put(monthName, Double.parseDouble(String.format("%.0f", entry.getValue())) // Ensure
																												// 2
					);
				});
		// System.out.println(formattedSales + " ");
		// Add sales data to the model for the frontend
		m.addAttribute("monthlySales", formattedSales);

		/*--------------------------------TOP PRODUCTS----------------*/
		// Find Top Selling Products (Based on Total Quantity Sold)
		Map<Product, Long> productSalesCount = allOrders.stream().collect(
				Collectors.groupingBy(ProductOrder::getProduct, Collectors.summingLong(ProductOrder::getQuantity)));

		// Sort products by total quantity sold in descending order and limit to top 5
		List<Map.Entry<Product, Long>> topSellingProducts = productSalesCount.entrySet().stream()
				.sorted(Map.Entry.<Product, Long>comparingByValue().reversed()) // Descending Order
				.limit(6) // Top 5 Products
				.toList();

		/*
		 * // Debugging (Optional) topSellingProducts.forEach(entry ->
		 * System.out.println("Product: " + entry.getKey().getTitle() +
		 * ", Quantity Sold: " + entry.getValue() + "  Product ID  " +
		 * entry.getKey().getId()));
		 */

		// Add to Model
		m.addAttribute("topSellingProducts", topSellingProducts);

		// Find Top Selling Categories (Summing all product quantities per category)
		Map<String, Long> categorySalesCount = allOrders.stream().collect(Collectors.groupingBy(
				order -> order.getProduct().getCategory(), Collectors.summingLong(ProductOrder::getQuantity)));

		// Sort categories by total quantity sold in descending order
		List<Map.Entry<String, Long>> topSellingCategories = categorySalesCount.entrySet().stream()
				.sorted(Map.Entry.<String, Long>comparingByValue().reversed()) // Descending Order
				.limit(5) // Top 5 Categories
				.toList();
		// Convert Map.Entry<String, Long> into a List of JSON-friendly objects
		List<Map<String, Object>> formattedCategories = topSellingCategories.stream().map(entry -> {
			Map<String, Object> map = new HashMap<>();
			map.put("category", entry.getKey());
			map.put("sales", entry.getValue());
			return map;
		}).collect(Collectors.toList());

		m.addAttribute("topSellingCategories", formattedCategories);
		// Define category colors
		List<String> legendColors = List.of("#4361ee", "#2ecc71", "#f39c12", "#e74c3c", "#9b59b6");

		m.addAttribute("legendColors", legendColors);

		return "admin/index";

	}

	/*-----------------------------------------------	COUPON STARTS  -------------------------------------------------------	*/

	@PostMapping("/saveCoupons")
	public String saveCoupons(@ModelAttribute Coupons coupons, HttpSession session) {

		Boolean existCoupons = couponsService.existCoupons(coupons.getCode());

		if (existCoupons) {
			session.setAttribute("errorMsg", "Coupons already exists");
		} else {

			Coupons saveCoupons = couponsService.saveCoupons(coupons);

			if (ObjectUtils.isEmpty(saveCoupons)) {
				session.setAttribute("errorMsg", "Not saved ! internal server error");
			} else {

				session.setAttribute("succMsg", "Saved successfully");
			}
		}

		return "redirect:/admin/coupons";
	}

	@GetMapping("/coupons")
	public String coupons(Model m, @RequestParam(defaultValue = "0") Integer pageNo,
			@RequestParam(defaultValue = "7") Integer pageSize) {

		Page<Coupons> page = couponsService.getAllCouponsPagination(pageNo, pageSize);
		List<Coupons> coupons = page.getContent();
		m.addAttribute("coupons", coupons);

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());
		// m.addAttribute("categorys", categoryService.getAllCategory());

		return "admin/coupons";
	}

	@GetMapping("/loadEditCoupons/{id}")
	public String loadCoupon(@PathVariable int id, Model m) {
		m.addAttribute("coupon", couponsService.getCouponsById(id));
		return "admin/update_coupons";
	}

	@PostMapping("/updateCoupon")
	public String updateCoupon(@ModelAttribute Coupons coupons, HttpSession session) throws IOException {

		Coupons old = couponsService.getCouponsById(coupons.getId());

		if (!ObjectUtils.isEmpty(coupons)) {
			old.setCode(coupons.getCode());
			old.setApplyOn(coupons.getApplyOn());
			old.setCategory(coupons.getCategory());
			old.setDiscount(old.getDiscount());
			old.setExpiryDate(old.getExpiryDate());
			old.setMinAmount(coupons.getMinAmount());
			old.setName(coupons.getName());
			old.setStartDate(coupons.getStartDate());
			old.setStatus(coupons.getStatus());
			old.setExpiryDate(coupons.getExpiryDate());

		}
		Coupons updateCoupons = couponsService.saveCoupons(old);

		if (!ObjectUtils.isEmpty(updateCoupons)) {

			session.setAttribute("succMsg", "Coupons updated successfully");
		} else {
			session.setAttribute("errorMsg", "something wrong on server");
		}

		return "redirect:/admin/loadEditCoupons/" + coupons.getId();
	}

	@GetMapping("/updateCouponsStatus")
	public String updateCouponsStatus(@RequestParam Boolean status, @RequestParam Integer id, HttpSession session,
			@RequestParam(defaultValue = "0") int pageNo, RedirectAttributes redirectAttributes) {
		Boolean f = couponsService.updateCouponsStatus(id, status);
		if (f == true) {
			if (status == true) {
				session.setAttribute("succMsg", "COUPON UPDATED (ACTIVATED)");
			} else {
				session.setAttribute("succMsg2", "COUPON UPDATED (DEACTIVATED)");
			}
		} else {
			session.setAttribute("errorMsg", "UPDATE FAILED SERVER ERROR");
		}

		return "redirect:/admin/coupons?pageNo=" + pageNo;
	}
	/*-----------------------------------	COUPON END -----------------------------------------------------------------	*/

	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {

		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user", userDtls);
			Integer countCart = cartService.getCountCart(userDtls.getId());
			m.addAttribute("countCart", countCart);
		}
		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);
	}

	@GetMapping("/loadAddProduct")
	public String loadAddProduct(Model m) {
		List<Category> categories = categoryService.getAllCategory();

		m.addAttribute("categories", categories);

		return "admin/add_product";
	}

	@GetMapping("/category")
	public String category(Model m, @RequestParam(defaultValue = "0") Integer pageNo,
			@RequestParam(defaultValue = "3") Integer pageSize) {

		Page<Category> page = categoryService.getAllCategoryPagination(pageNo, pageSize);
		List<Category> categorys = page.getContent();
		m.addAttribute("categorys", categorys);

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());
		// m.addAttribute("categorys", categoryService.getAllCategory());

		return "admin/category";
	}

	@PostMapping("/saveCategory")
	public String saveCategory(@ModelAttribute Category category, @RequestParam MultipartFile file, HttpSession session)
			throws IOException {

		String imageName = file != null ? file.getOriginalFilename() : "default.jpg";
		category.setImageName(imageName);

		Boolean existCategory = categoryService.existCategory(category.getName());

		if (existCategory) {
			session.setAttribute("errorMsg", "Category Name already exists");
		} else {

			Category saveCategory = categoryService.saveCategory(category);

			if (ObjectUtils.isEmpty(saveCategory)) {
				session.setAttribute("errorMsg", "Not saved ! internal server error");
			} else {

				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "category_img" + File.separator
						+ file.getOriginalFilename());

				System.out.println(path);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				session.setAttribute("succMsg", "Saved successfully");
			}
		}

		return "redirect:/admin/category";
	}

	@GetMapping("/deleteCategory/{id}")
	public String deleteCategory(@PathVariable int id, HttpSession session) {
		Boolean deleteCategory = categoryService.deleteCategory(id);
		if (deleteCategory) {

			session.setAttribute("succMsg", "Category Deleted");

		} else {

			session.setAttribute("errorMsg", "Same Wrong On Server");

		}

		return "redirect:/admin/category";
	}

	@GetMapping("/loadEditCategory/{id}")
	public String loadEditCategory(@PathVariable int id, Model m) {
		m.addAttribute("category", categoryService.getCategoryById(id));
		return "admin/edit_category";
	}

	@PostMapping("/updateCategory")
	public String updateCategory(@ModelAttribute Category category, @RequestParam MultipartFile file,
			HttpSession session) throws IOException {

		Category oldCategory = categoryService.getCategoryById(category.getId());
		String imageName = file.isEmpty() ? oldCategory.getImageName() : file.getOriginalFilename();

		if (!ObjectUtils.isEmpty(category)) {
			oldCategory.setName(category.getName());
			oldCategory.setIsActive(category.getIsActive());
			oldCategory.setImageName(imageName);

		}
		Category updateCategory = categoryService.saveCategory(oldCategory);

		if (!ObjectUtils.isEmpty(updateCategory)) {

			if (!file.isEmpty()) {

				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "category_img" + File.separator
						+ file.getOriginalFilename());

				System.out.println(path);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}

			session.setAttribute("succMsg", "Category update success");
		} else {
			session.setAttribute("errorMsg", "something wrong on server");
		}

		return "redirect:/admin/loadEditCategory/" + category.getId();
	}

	@PostMapping("/saveProduct")
	public String saveProduct(@ModelAttribute Product product, HttpSession session,
			@RequestParam("file") MultipartFile image) throws IOException {

		String imageName = image.isEmpty() ? "Default.jpg" : image.getOriginalFilename();
		product.setImage(imageName);

		product.setDiscount(0);
		product.setDiscountPrice(product.getPrice());

		Product saveProduct = productService.saveProduct(product);

		ProductElastic productElastic = new ProductElastic(product);
		elasticsearchClient
				.index(builder -> builder.index("product").id(product.getId().toString()).document(productElastic));
		// Product to Elastic Save
		elasticSearchQuery.createOrUpdateDocument(product);
		System.out.print(product);

		if (!ObjectUtils.isEmpty(saveProduct)) {
			File saveFile = new ClassPathResource("static/img").getFile();

			Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "product_img" + File.separator
					+ image.getOriginalFilename());

			System.out.println(path);
			Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			session.setAttribute("succMsg", "Product Saved Successfull");

		} else {
			session.setAttribute("errorMsg", "Something Error in Server");
		}
		return "redirect:/admin/loadAddProduct";

	}

	@GetMapping("/products")
	public String loadViewProducts(Model m, @RequestParam(defaultValue = "") String ch,
			@RequestParam(defaultValue = "0") Integer pageNo, @RequestParam(defaultValue = "10") Integer pageSize) {

		/*
		 * List<Product> products = null; if (ch != null && ch.length() > 0) {
		 * 
		 * products = productService.searchProduct(ch); } else { products =
		 * productService.getAllProducts(); }
		 * 
		 * m.addAttribute("products", products);
		 */

		Page<Product> page = null;
		if (ch != null && ch.length() > 0) {

			page = productService.searchProductPagination(ch, pageNo, pageSize);
		} else {
			page = productService.getAllProductsPagination(pageNo, pageSize);
		}

		m.addAttribute("products", page.getContent());

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "/admin/products";

	}

	@GetMapping("/deleteProduct/{id}")
	public String deleteProduct(@PathVariable int id, HttpSession session) {
		Boolean deleteProduct = productService.deleteProduct(id);
		if (deleteProduct) {
			session.setAttribute("succMsg", "Product Deleted successfully");
		} else {
			session.setAttribute("errorMsg", "Server Error");
		}
		return "redirect:/admin/products";

	}

	@GetMapping("/editProduct/{id}")
	public String editProduct(@PathVariable int id, Model m) {
		m.addAttribute("product", productService.getProductById(id));
		m.addAttribute("categories", categoryService.getAllCategory());
		return "/admin/edit_product";

	}

	@PostMapping("/updateProduct")
	public String updateProduct(@ModelAttribute Product product, Model m, HttpSession session,
			@RequestParam("file") MultipartFile image) throws IOException {

		if (product.getDiscount() < 0 || product.getDiscount() > 100) {
			session.setAttribute("errorMsg", "invalid Discount");
		} else {
			Product updateProduct = productService.updateProduct(product, image);
			if (!ObjectUtils.isEmpty(updateProduct)) {

				session.setAttribute("succMsg", "Product update success");
			} else {
				session.setAttribute("errorMsg", "Something wrong on server");
			}
		}
		return "redirect:/admin/editProduct/" + product.getId();

	}

	@GetMapping("/users")
	public String getAllUsers(Model m, @RequestParam Integer type) {
		List<UserDtls> users = null;
		if (type == 1) {
			users = userService.getUsers("ROLE_USER");
		} else {
			users = userService.getUsers("ROLE_ADMIN");
		}
		m.addAttribute("userType", type);
		m.addAttribute("users", users);
		return "/admin/users";

	}

	@GetMapping("/updateStatus")
	public String updateAccountStatus(@RequestParam Boolean status, @RequestParam Integer id, HttpSession session,
			@RequestParam Integer type) {
		Boolean f = userService.updateAccountStatus(id, status);
		if (f == true) {
			if (status == true) {
				session.setAttribute("succMsg", "ACCOUNT UPDATED (ACTIVATED)");
			} else {
				session.setAttribute("succMsg2", "ACCOUNT UPDATED (DEACTIVATED)");
			}
		} else {
			session.setAttribute("errorMsg", "UPDATE FAILED SERVER ERROR");
		}

		return "redirect:/admin/users?type=" + type;
	}

	@GetMapping("/orders")
	public String getAllOrders(Model m, @RequestParam(defaultValue = "0") Integer pageNo,
			@RequestParam(defaultValue = "5") Integer pageSize) {

		Page<ProductOrder> page = orderService.getAllOrdersPagination(pageNo, pageSize);

		m.addAttribute("orders", page.getContent());
		m.addAttribute("srch", false);

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "/admin/orders";
	}

	@PostMapping("/update-order-status")
	public String updateOrderStatus(@RequestParam Integer id, @RequestParam Integer st,
			@RequestParam(value = "pageNo", required = false) Integer pageNo, HttpSession session) {

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
			session.setAttribute("errorMsg", "Status not updated");
		}

		// Redirect based on pageNo
		if (pageNo != null) {
			return "redirect:/admin/orders?pageNo=" + pageNo;
		} else {
			return "redirect:/admin/orders";
		}
	}

	@GetMapping("/search-order")
	public String searchOrder(@RequestParam String orderId, Model m, HttpSession session,
			@RequestParam(defaultValue = "0") Integer pageNo, @RequestParam(defaultValue = "5") Integer pageSize) {

		if (orderId != null && orderId.length() > 0) {

			ProductOrder order = orderService.getOrdersByOrdersId(orderId);

			if (ObjectUtils.isEmpty(order)) {

				session.setAttribute("errorMsg", "Incorrect OrderId");
				m.addAttribute("orderDtls", null);
			} else {
				m.addAttribute("orderDtls", order);
			}
			m.addAttribute("srch", true);

		} else {
			/*
			 * List<ProductOrder> allOrders = orderService.getAllOrders();
			 * m.addAttribute("orders", allOrders); m.addAttribute("srch", false);
			 */

			Page<ProductOrder> page = orderService.getAllOrdersPagination(pageNo, pageSize);
			m.addAttribute("orders", page);
			m.addAttribute("srch", false);

			m.addAttribute("pageNo", page.getNumber());
			m.addAttribute("totalElements", page.getTotalElements());
			m.addAttribute("pageSize", pageSize);
			m.addAttribute("totalPages", page.getTotalPages());
			m.addAttribute("isFirst", page.isFirst());
			m.addAttribute("isLast", page.isLast());

		}
		return "/admin/orders";
	}

	@GetMapping("/add-admin")
	public String loadAdminAdd() {

		return "/admin/add_admin";
	}

	@PostMapping("/save-admin")
	public String saveAdmin(@ModelAttribute UserDtls user, @RequestParam("img") MultipartFile file, HttpSession session)
			throws IOException {

		String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
		user.setProfleImage(imageName);
		UserDtls saveUser = userService.saveAdmin(user);

		if (!ObjectUtils.isEmpty(saveUser)) {
			if (!file.isEmpty()) {
				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator
						+ file.getOriginalFilename());

				System.out.println(path);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

			}
			session.setAttribute("succMsg", "Registered Successfull ");

		} else {
			session.setAttribute("errorMsg", "Something wrong on server");
		}
		return "redirect:/admin/add-admin";

	}

	@GetMapping("/profile")
	public String profile() {
		return "/admin/profile";
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
		return "redirect:/admin/profile";
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam String newPassword, @RequestParam String currentPassword, Principal p,
			HttpSession session) {
		UserDtls loggedInUserDetails = commonUtils.getLoggedInUserDetails(p);
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
		return "redirect:/admin/profile";
	}

}
