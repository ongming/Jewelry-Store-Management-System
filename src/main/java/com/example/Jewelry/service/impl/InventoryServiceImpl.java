package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.Inventory;
import com.example.Jewelry.repository.InventoryRepository;
import com.example.Jewelry.service.InventoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository InventoryRepository;

    public InventoryServiceImpl(InventoryRepository InventoryRepository) {
        this.InventoryRepository = InventoryRepository;
    }

    @Override
    public Optional<Inventory> findById(Integer id) {
        return InventoryRepository.findById(id);
    }

    @Override
    public List<Inventory> findAll() {
        return InventoryRepository.findAll();
    }

    @Override
    public Inventory save(Inventory entity) {
        return InventoryRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        InventoryRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return InventoryRepository.existsById(id);
    }

    @Override
    public long count() {
        return InventoryRepository.count();
    }
}
