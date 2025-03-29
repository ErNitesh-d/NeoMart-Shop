package com.ecom.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecom.model.Coupons;

@Repository
public interface CouponsRepository extends JpaRepository<Coupons, Integer> {

	public Boolean existsByCode(String code);

	@Override
	public Optional<Coupons> findById(Integer id);

	Optional<Coupons> findByCode(String code);

}
