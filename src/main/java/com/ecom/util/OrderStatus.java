package com.ecom.util;

public enum OrderStatus {

	IN_PROGRESS(1, "IN_PROGRESS"), ORDER_RECEIVED(2, "ORDER_RECEIVED"), PRODUCT_PACKED(3, "PRODUCT_PACKED"),
	OUT_FOR_DELIVERY(4, "OUT_FOR_DELIVERY"), DELIVERED(5, "DELIVERED"), CANCELED(6, "CANCELED"), SUCCESS(7, "SUCCESS"),
	RETURN_PROCCESING(8, "RETURN_PROCCESING"), RETURNED(9, "RETURNED");

	private Integer id;
	private String name;

	private OrderStatus(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
