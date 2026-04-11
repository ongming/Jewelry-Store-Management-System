package com.example.Jewelry.repository;

import com.example.Jewelry.model.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

	boolean existsByProduct_ProductId(Integer productId);
}
