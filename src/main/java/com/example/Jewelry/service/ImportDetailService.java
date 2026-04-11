package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.ImportDetail;

import java.util.List;
import java.util.Optional;

public interface ImportDetailService {

    Optional<ImportDetail> findById(Integer id);

    List<ImportDetail> findAll();

    ImportDetail save(ImportDetail entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();
}
