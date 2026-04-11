package com.example.Jewelry.repository;

import com.example.Jewelry.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

	Optional<Category> findByCategoryNameIgnoreCase(String categoryName);
}
