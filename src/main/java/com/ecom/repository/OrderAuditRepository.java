package com.ecom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecom.model.OrderAudit;

@Repository
public interface OrderAuditRepository extends JpaRepository<OrderAudit, Integer> {
	List<OrderAudit> findByProductOrderId(Integer productOrderId);
}
