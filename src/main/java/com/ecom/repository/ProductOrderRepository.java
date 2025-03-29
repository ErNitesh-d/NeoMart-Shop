package com.ecom.repository;

import java.util.List;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.ecom.model.OrderAudit;
import com.ecom.model.ProductOrder;

public interface ProductOrderRepository extends JpaRepository<ProductOrder, Integer> {

	List<ProductOrder> findByUserId(Integer userId);

	ProductOrder findByOrderId(String orderId);

	OrderAudit save(OrderAudit orderAudit);
	
	@Query("SELECT po FROM ProductOrder po WHERE po.user.id = :userId AND po.product.id = :productId AND po.status = :status")
	ProductOrder findByUserIdAndProductIdAndStatus(@Param("userId") Integer userId,
	                                               @Param("productId") Integer productId,
	                                               @Param("status") String status);

	ProductOrder findByOrderIdAndProductId(String orderId,Integer pid);

}
