package com.ecom.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PathVariable;

import com.ecom.model.OrderRequest;
import com.ecom.model.Product;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;

public interface OrderService {

	public void saveOrder(Integer userid, OrderRequest orderRequest) throws Exception;

	public void purchaseOrder(Product products, UserDtls user, OrderRequest orderRequest) throws Exception;

	public List<ProductOrder> getOrdersByUser(Integer userId);

	public ProductOrder updateOrderStatus(Integer id, String status);

	public List<ProductOrder> getAllOrders();

	public ProductOrder getOrdersByOrdersId(String orderId);

	public void saveReturnOrder(String orderId, Integer integer, @PathVariable OrderRequest request) throws Exception;

	ProductOrder getOrderId(String orderId);

	public Page<ProductOrder> getAllOrdersPagination(Integer pageNo, Integer pageSize);

	// Method to get ProductOrder by ID
	public ProductOrder getProductOrderById(Integer id);
}
