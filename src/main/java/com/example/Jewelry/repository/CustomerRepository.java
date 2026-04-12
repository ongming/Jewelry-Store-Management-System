package com.example.Jewelry.repository;

import com.example.Jewelry.model.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

	List<Customer> findAllByOrderByCustomerNameAsc();

	List<Customer> findByCustomerNameContainingIgnoreCaseOrPhoneContainingIgnoreCaseOrderByCustomerNameAsc(String customerName,
																											 String phone);
}
