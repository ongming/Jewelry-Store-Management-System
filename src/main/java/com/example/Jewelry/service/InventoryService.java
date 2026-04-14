package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.Inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InventoryService {

    Optional<Inventory> findById(Integer id);

    Optional<Inventory> findByProductId(Integer productId);

    void addStock(Integer productId, int quantity, Integer supplierId, BigDecimal importPrice, Integer staffAccountId);

    void importStock(Integer supplierId, Integer staffAccountId, List<Integer> productIds, List<Integer> quantities, List<BigDecimal> importPrices);


    void deductStockForSale(Integer productId, int quantity, Integer staffAccountId, Integer orderId);

    List<Inventory> findAll();

    Inventory save(Inventory entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();
}