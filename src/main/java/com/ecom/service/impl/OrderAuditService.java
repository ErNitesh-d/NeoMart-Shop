package com.ecom.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecom.model.OrderAudit;
import com.ecom.repository.OrderAuditRepository;

@Service
public class OrderAuditService {

	@Autowired
	private OrderAuditRepository orderAuditRepository;

	public List<OrderAudit> getOrderAuditsByProductOrderId(Integer productOrderId) {
		return orderAuditRepository.findByProductOrderId(productOrderId);
	}
}
