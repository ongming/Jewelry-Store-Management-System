package com.example.Jewelry.repository;

import com.example.Jewelry.model.entity.ImportDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportDetailRepository extends JpaRepository<ImportDetail, Integer> {

    boolean existsByProduct_ProductId(Integer productId);
}
