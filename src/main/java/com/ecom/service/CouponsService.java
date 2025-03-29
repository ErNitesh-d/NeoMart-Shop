package com.ecom.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.ecom.model.Coupons;

@Service
public interface CouponsService {

	public Boolean existCoupons(String code);

	public Coupons saveCoupons(Coupons coupons);

	public Page<Coupons> getAllCouponsPagination(Integer pageNo, Integer pageSize);

	public Boolean updateCouponsStatus(Integer id, Boolean status);

	public Coupons getCouponsById(int id);

	public Optional<Coupons> getCouponsByCode(String code);

}
