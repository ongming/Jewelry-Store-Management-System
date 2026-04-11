package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.Category;
import com.example.Jewelry.repository.CategoryRepository;
import com.example.Jewelry.repository.ProductRepository;
import com.example.Jewelry.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Optional<Category> findById(Integer id) {
        return categoryRepository.findById(id);
    }

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public Category save(Category entity) {
        return categoryRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        categoryRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return categoryRepository.existsById(id);
    }

    @Override
    public long count() {
        return categoryRepository.count();
    }

    @Override
    public Category createCategory(String categoryName) {
        validateCategoryName(categoryName);
        categoryRepository.findByCategoryNameIgnoreCase(categoryName.trim())
            .ifPresent(found -> {
                throw new IllegalArgumentException("Danh muc da ton tai.");
            });

        Category category = new Category();
        category.setCategoryName(categoryName.trim());
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Integer categoryId, String categoryName) {
        validateCategoryName(categoryName);

        Category existingCategory = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Danh muc khong ton tai."));

        categoryRepository.findByCategoryNameIgnoreCase(categoryName.trim())
            .filter(found -> found.getCategoryId() != categoryId)
            .ifPresent(found -> {
                throw new IllegalArgumentException("Danh muc da ton tai.");
            });

        existingCategory.setCategoryName(categoryName.trim());
        return categoryRepository.save(existingCategory);
    }

    @Override
    public void deleteCategory(Integer categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("Danh muc khong ton tai.");
        }
        if (productRepository.existsByCategory_CategoryId(categoryId)) {
            throw new IllegalStateException("Khong the xoa danh muc dang duoc su dung boi san pham.");
        }
        categoryRepository.deleteById(categoryId);
    }

    private void validateCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("Ten danh muc khong duoc de trong.");
        }
    }
}
