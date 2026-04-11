package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.ProductAttribute;

import java.util.List;
import java.util.Optional;

public interface ProductAttributeService {

    Optional<ProductAttribute> findById(Integer id);

    List<ProductAttribute> findAll();

    ProductAttribute save(ProductAttribute entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();
}
