package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.Category;
import com.example.Jewelry.repository.CategoryRepository;
import com.example.Jewelry.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository CategoryRepository;

    public CategoryServiceImpl(CategoryRepository CategoryRepository) {
        this.CategoryRepository = CategoryRepository;
    }

    @Override
    public Optional<Category> findById(Integer id) {
        return CategoryRepository.findById(id);
    }

    @Override
    public List<Category> findAll() {
        return CategoryRepository.findAll();
    }

    @Override
    public Category save(Category entity) {
        return CategoryRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        CategoryRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return CategoryRepository.existsById(id);
    }

    @Override
    public long count() {
        return CategoryRepository.count();
    }
}
