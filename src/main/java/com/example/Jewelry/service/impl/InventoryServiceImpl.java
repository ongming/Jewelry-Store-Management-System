package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.Inventory;
import com.example.Jewelry.model.entity.ImportReceipt;
import com.example.Jewelry.model.entity.ImportDetail;
import com.example.Jewelry.model.entity.Supplier;
import com.example.Jewelry.model.entity.Staff;
import com.example.Jewelry.repository.InventoryRepository;
import com.example.Jewelry.repository.ImportReceiptRepository;
import com.example.Jewelry.repository.ImportDetailRepository;
import com.example.Jewelry.repository.SupplierRepository;
import com.example.Jewelry.repository.StaffRepository;
import com.example.Jewelry.repository.ProductRepository;
import com.example.Jewelry.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ImportReceiptRepository importReceiptRepository;
    private final ImportDetailRepository importDetailRepository;
    private final SupplierRepository supplierRepository;
    private final StaffRepository staffRepository;
    private final ProductRepository productRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                ImportReceiptRepository importReceiptRepository,
                                ImportDetailRepository importDetailRepository,
                                SupplierRepository supplierRepository,
                                StaffRepository staffRepository,
                                ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.importReceiptRepository = importReceiptRepository;
        this.importDetailRepository = importDetailRepository;
        this.supplierRepository = supplierRepository;
        this.staffRepository = staffRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Optional<Inventory> findByProductId(Integer productId) {
        return inventoryRepository.findByProductProductId(productId);
    }

    @Override
    @Transactional
    public void addStock(Integer productId, int quantity, Integer supplierId, BigDecimal importPrice, Integer staffAccountId) {
        // Cập nhật tồn kho
        Optional<Inventory> optionalInventory = inventoryRepository.findByProductProductId(productId);
        com.example.Jewelry.model.entity.Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));

        if (optionalInventory.isPresent()) {
            Inventory inventory = optionalInventory.get();
            inventory.setQuantityStock(inventory.getQuantityStock() + quantity);
            inventoryRepository.save(inventory);
        } else {
            Inventory inventory = new Inventory();
            inventory.setProduct(product);
            inventory.setQuantityStock(quantity);
            inventoryRepository.save(inventory);
        }

        // Tạo phiếu nhập kho (ImportReceipt)
        Supplier supplier = supplierRepository.findById(supplierId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà cung cấp."));
            
        Staff staff = staffRepository.findById(staffAccountId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin nhân viên nhập hàng."));

        ImportReceipt receipt = new ImportReceipt();
        receipt.setImportDate(LocalDateTime.now());
        receipt.setSupplier(supplier);
        receipt.setStaff(staff);
        
        BigDecimal totalCost = importPrice.multiply(BigDecimal.valueOf(quantity));
        receipt.setTotalCost(totalCost);
        
        ImportReceipt savedReceipt = importReceiptRepository.save(receipt);

        // Tạo chi tiết nhập kho (ImportDetail)
        ImportDetail detail = new ImportDetail();
        detail.setImportReceipt(savedReceipt);
        detail.setProduct(product);
        detail.setQuantity(quantity);
        detail.setImportPrice(importPrice);
        importDetailRepository.save(detail);
    }

    @Override
    @Transactional
    public void importStock(Integer supplierId, Integer staffAccountId, List<Integer> productIds, List<Integer> quantities, List<BigDecimal> importPrices) {
        if (productIds == null || quantities == null || importPrices == null || 
            productIds.isEmpty() || productIds.size() != quantities.size() || quantities.size() != importPrices.size()) {
            throw new IllegalArgumentException("Dữ liệu sản phẩm nhập kho không hợp lệ.");
        }

        Supplier supplier = supplierRepository.findById(supplierId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà cung cấp."));
            
        Staff staff = staffRepository.findById(staffAccountId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin nhân viên nhập hàng."));

        ImportReceipt receipt = new ImportReceipt();
        receipt.setImportDate(LocalDateTime.now());
        receipt.setSupplier(supplier);
        receipt.setStaff(staff);
        
        BigDecimal totalCost = BigDecimal.ZERO;
        for (int i = 0; i < productIds.size(); i++) {
            BigDecimal price = importPrices.get(i);
            int quantity = quantities.get(i);
            totalCost = totalCost.add(price.multiply(BigDecimal.valueOf(quantity)));
        }
        receipt.setTotalCost(totalCost);
        
        ImportReceipt savedReceipt = importReceiptRepository.save(receipt);

        for (int i = 0; i < productIds.size(); i++) {
            Integer productId = productIds.get(i);
            int quantity = quantities.get(i);
            BigDecimal price = importPrices.get(i);

            com.example.Jewelry.model.entity.Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm có ID: " + productId));

            Optional<Inventory> optionalInventory = inventoryRepository.findByProductProductId(productId);
            if (optionalInventory.isPresent()) {
                Inventory inventory = optionalInventory.get();
                inventory.setQuantityStock(inventory.getQuantityStock() + quantity);
                inventoryRepository.save(inventory);
            } else {
                Inventory inventory = new Inventory();
                inventory.setProduct(product);
                inventory.setQuantityStock(quantity);
                inventoryRepository.save(inventory);
            }

            ImportDetail detail = new ImportDetail();
            detail.setImportReceipt(savedReceipt);
            detail.setProduct(product);
            detail.setQuantity(quantity);
            detail.setImportPrice(price);
            importDetailRepository.save(detail);

            savedReceipt.getDetails().add(detail);
        }
    }

    @Override
    public Optional<Inventory> findById(Integer id) {
        return inventoryRepository.findById(id);
    }

    @Override
    public List<Inventory> findAll() {
        return inventoryRepository.findAll();
    }

    @Override
    public Inventory save(Inventory entity) {
        return inventoryRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        inventoryRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return inventoryRepository.existsById(id);
    }

    @Override
    public long count() {
        return inventoryRepository.count();
    }
}