package com.ecom.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ecom.model.Coupons;
import com.ecom.repository.CouponsRepository;
import com.ecom.service.CouponsService;

@Service
public class CouponsServiceImpl implements CouponsService {

	@Autowired
	private CouponsRepository couponsRepository;

	@Override
	public Boolean existCoupons(String code) {

		return couponsRepository.existsByCode(code);
	}

	@Override
	public Coupons saveCoupons(Coupons coupons) {

		return couponsRepository.save(coupons);
	}

	@Override
	public Page<Coupons> getAllCouponsPagination(Integer pageNo, Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return couponsRepository.findAll(pageable);
	}

	@Override
	public Boolean updateCouponsStatus(Integer id, Boolean status) {
		Optional<Coupons> findByCoupons = couponsRepository.findById(id);
		if (findByCoupons.isPresent()) {
			Coupons coupons = findByCoupons.get();
			coupons.setStatus(status);
			couponsRepository.save(coupons);

			return true;
		}
		return false;
	}

	@Override
	public Coupons getCouponsById(int id) {
		Coupons coupons = couponsRepository.findById(id).orElse(null);
		return coupons;
	}

	@Override
	public Optional<Coupons> getCouponsByCode(String code) {
		return couponsRepository.findByCode(code);
	}
}
