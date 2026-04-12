package com.example.Jewelry.repository;

import com.example.Jewelry.model.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {

	List<Order> findByCustomer_CustomerIdOrderByOrderDateDesc(Integer customerId);

	boolean existsByCustomer_CustomerId(Integer customerId);
}
