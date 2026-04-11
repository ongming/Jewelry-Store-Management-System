package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryService {

    Optional<Category> findById(Integer id);

    List<Category> findAll();

    Category save(Category entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();

    Category createCategory(String categoryName);

    Category updateCategory(Integer categoryId, String categoryName);

    void deleteCategory(Integer categoryId);
}
