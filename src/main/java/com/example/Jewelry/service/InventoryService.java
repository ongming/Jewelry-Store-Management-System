package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.Inventory;

import java.util.List;
import java.util.Optional;

public interface InventoryService {

    Optional<Inventory> findById(Integer id);

    List<Inventory> findAll();

    Inventory save(Inventory entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();
}
