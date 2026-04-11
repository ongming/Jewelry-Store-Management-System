package com.example.Jewelry.repository;

import com.example.Jewelry.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

	Optional<Product> findByProductCodeIgnoreCase(String productCode);

	boolean existsByCategory_CategoryId(Integer categoryId);
}
