package com.ecom.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.ecom.model.Cart;
import com.ecom.model.Coupons;
import com.ecom.model.OrderAddress;
import com.ecom.model.OrderAudit;
import com.ecom.model.OrderRequest;
import com.ecom.model.Product;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.repository.CartRepository;
import com.ecom.repository.CouponsRepository;
import com.ecom.repository.ProductOrderRepository;
import com.ecom.service.OrderService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	private CouponsRepository couponsRepository;
	@Autowired
	private ProductOrderRepository orderRepository;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private CommonUtil commonUtils;

	@Override
	public void saveOrder(Integer userid, OrderRequest orderRequest) throws Exception {

		List<Cart> carts = cartRepository.findByUserId(userid);

		for (Cart cart : carts) {
			ProductOrder order = new ProductOrder();

			order.setOrderId(UUID.randomUUID().toString());
			order.setOrderDate(LocalDate.now());
			order.setProduct(cart.getProduct());
			order.setPrice(cart.getProduct().getDiscountPrice());
			order.setQuantity(cart.getQuantity());
			order.setUser(cart.getUser());

			Double discountPrice = cart.getProduct().getDiscountPrice();
			boolean isDiscountApplied = false;

			if (orderRequest.getCouponId() != null) {
				Optional<Coupons> optionalCoupon = couponsRepository.findById(orderRequest.getCouponId());

				if (optionalCoupon.isPresent()) {
					Coupons appliedCoupon = optionalCoupon.get();

					if ("All".equalsIgnoreCase(appliedCoupon.getApplyOn())) {
						discountPrice = discountPrice - (discountPrice * appliedCoupon.getDiscount() / 100);
						isDiscountApplied = true;
					}

					else if (cart.getProduct().getCategory().equalsIgnoreCase(appliedCoupon.getCategory())) {
						discountPrice = discountPrice - (discountPrice * appliedCoupon.getDiscount() / 100);
						isDiscountApplied = true;
					}

					if (isDiscountApplied) {
						order.setCouponsApplied(appliedCoupon); // ✅ કૂપન સ્ટોર કરવું
					}
				}
			}
			discountPrice = Double.valueOf(String.format("%.0f", discountPrice));
			order.setDiscountPrice(discountPrice);

			order.setTotalPrice(order.getQuantity() * discountPrice);
			order.setStatus(OrderStatus.IN_PROGRESS.getName());
			order.setPaymentType(orderRequest.getPaymentType());

			// Store Razorpay Payment ID (if available)
			order.setPaymentId(orderRequest.getPaymentId());

			OrderAddress address = new OrderAddress();
			address.setFirstName(orderRequest.getFirstName());
			address.setLastName(orderRequest.getLastName());
			address.setEmail(orderRequest.getEmail());
			address.setMobileNo(orderRequest.getMobileNo());
			address.setAddress(orderRequest.getAddress());
			address.setCity(orderRequest.getCity());
			address.setState(orderRequest.getState());
			address.setPincode(orderRequest.getPincode());

			order.setOrderAddress(address);

			order = orderRepository.save(order);

			OrderAudit orderAudit = new OrderAudit();
			orderAudit.setProductOrder(order);
			orderAudit.setStatus(order.getStatus());
			orderAudit.setStatusDate(LocalDateTime.now());
			orderRepository.save(orderAudit);

			cartRepository.delete(cart);
			// System.out.println("✅ Order created with ID: " + order.getOrderId());
			commonUtils.sendMailForProductOrder(order, "Successful");
		}
	}

	@Override
	public void purchaseOrder(Product products, UserDtls user, OrderRequest orderRequest) throws Exception {

		// ✅ Step 2: Create & Save ProductOrder
		ProductOrder order = new ProductOrder();
		order.setOrderId(UUID.randomUUID().toString());
		order.setProduct(products);
		order.setOrderDate(LocalDate.now());
		order.setUser(user);
		order.setStatus(OrderStatus.IN_PROGRESS.getName());
		order.setPaymentType(orderRequest.getPaymentType());
		order.setQuantity(1);
		order.setDiscountPrice(products.getDiscountPrice());
		order.setTotalPrice(order.getQuantity() * order.getDiscountPrice());
		order.setPrice(products.getPrice());

		// ✅ Step 5: Save Order and OrderDetails
		ProductOrder savedOrder = orderRepository.save(order);

		// ✅ Step 6: Save OrderAddress
		OrderAddress address = new OrderAddress();
		address.setFirstName(orderRequest.getFirstName());
		address.setLastName(orderRequest.getLastName());
		address.setEmail(orderRequest.getEmail());
		address.setMobileNo(orderRequest.getMobileNo());
		address.setAddress(orderRequest.getAddress());
		address.setCity(orderRequest.getCity());
		address.setState(orderRequest.getState());
		address.setPincode(orderRequest.getPincode());

		savedOrder.setOrderAddress(address);
		orderRepository.save(savedOrder);

		// ✅ Step 7: Save OrderAudit
		OrderAudit orderAudit = new OrderAudit();
		orderAudit.setProductOrder(savedOrder);
		orderAudit.setStatus(savedOrder.getStatus());
		orderAudit.setStatusDate(LocalDateTime.now());

		orderRepository.save(orderAudit);

		// ✅ Step 8: Send Confirmation Email
		// commonUtils.sendMailForProductOrder(savedOrder, "Successful");
	}

	@Override
	public void saveReturnOrder(@PathVariable String orderId, Integer userid, OrderRequest orderRequest)
			throws Exception {

		ProductOrder productOrder = orderRepository.findByOrderId(orderId);

		// ✅ Update order status
		productOrder.setStatus(OrderStatus.RETURN_PROCCESING.getName());
		productOrder.setOrderDate(LocalDate.now());
		productOrder.setPaymentType(orderRequest.getPaymentType());

		// ✅ Update the order address
		OrderAddress address = productOrder.getOrderAddress();
		if (address == null) {
			address = new OrderAddress();
			productOrder.setOrderAddress(address);
		}
		address.setFirstName(orderRequest.getFirstName());
		address.setLastName(orderRequest.getLastName());
		address.setEmail(orderRequest.getEmail());
		address.setMobileNo(orderRequest.getMobileNo());
		address.setAddress(orderRequest.getAddress());
		address.setCity(orderRequest.getCity());
		address.setState(orderRequest.getState());
		address.setPincode(orderRequest.getPincode());

		orderRepository.save(productOrder);

		// ✅ Save the Order Audit
		OrderAudit orderAudit = new OrderAudit();
		orderAudit.setProductOrder(productOrder);
		orderAudit.setStatus(OrderStatus.RETURN_PROCCESING.getName());
		orderAudit.setStatusDate(LocalDateTime.now());

		// ✅ Save orderAudit using orderAuditRepository
		orderRepository.save(orderAudit);

	}

	@Override
	public List<ProductOrder> getOrdersByUser(Integer userId) {
		List<ProductOrder> orders = orderRepository.findByUserId(userId);
		return orders;
	}

	@Override
	public ProductOrder updateOrderStatus(Integer id, String status) {

		Optional<ProductOrder> findById = orderRepository.findById(id);

		if (findById.isPresent()) {

			ProductOrder productOrder = findById.get();
			productOrder.setStatus(status);
			ProductOrder updateOrder = orderRepository.save(productOrder);
			return updateOrder;
		}

		return null;
	}

	@Override
	public List<ProductOrder> getAllOrders() {
		return orderRepository.findAll();

	}

	@Override
	public Page<ProductOrder> getAllOrdersPagination(Integer pageNo, Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return orderRepository.findAll(pageable);
	}

	@Override
	public ProductOrder getOrderId(String orderId) {
		return orderRepository.findByOrderId(orderId);

	}

	@Override
	public ProductOrder getProductOrderById(Integer id) {
		return orderRepository.findById(id).orElse(null); // Returns null if the order is not found
	}

	@Override
	public ProductOrder getOrdersByOrdersId(String orderId) {
		// TODO Auto-generated method stub
		return orderRepository.findByOrderId(orderId);
	}

}
