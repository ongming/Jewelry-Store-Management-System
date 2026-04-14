package com.example.Jewelry.service.impl;

import com.example.Jewelry.event.InventoryChangeType;
import com.example.Jewelry.event.InventoryReferenceType;
import com.example.Jewelry.event.InventoryStockChangedEvent;
import com.example.Jewelry.model.entity.ImportDetail;
import com.example.Jewelry.model.entity.ImportReceipt;
import com.example.Jewelry.model.entity.Inventory;
import com.example.Jewelry.model.entity.Product;
import com.example.Jewelry.model.entity.Staff;
import com.example.Jewelry.model.entity.Supplier;
import com.example.Jewelry.repository.ImportDetailRepository;
import com.example.Jewelry.repository.ImportReceiptRepository;
import com.example.Jewelry.repository.InventoryRepository;
import com.example.Jewelry.repository.ProductRepository;
import com.example.Jewelry.repository.StaffRepository;
import com.example.Jewelry.repository.SupplierRepository;
import com.example.Jewelry.service.InventoryService;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                ImportReceiptRepository importReceiptRepository,
                                ImportDetailRepository importDetailRepository,
                                SupplierRepository supplierRepository,
                                StaffRepository staffRepository,
                                ProductRepository productRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.inventoryRepository = inventoryRepository;
        this.importReceiptRepository = importReceiptRepository;
        this.importDetailRepository = importDetailRepository;
        this.supplierRepository = supplierRepository;
        this.staffRepository = staffRepository;
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Optional<Inventory> findByProductId(Integer productId) {
        return inventoryRepository.findByProductProductId(productId);
    }

    @Override
    @Transactional
    public void addStock(Integer productId, int quantity, Integer supplierId, BigDecimal importPrice, Integer staffAccountId) {
        validatePositiveQuantity(quantity, "Số lượng nhập phải lớn hơn 0.");
        validatePositivePrice(importPrice, "Giá nhập phải lớn hơn 0.");

        Product product = requireProduct(productId);
        Supplier supplier = requireSupplier(supplierId);
        Staff staff = requireStaff(staffAccountId);

        int oldQty = inventoryRepository.findByProductProductId(productId)
            .map(inventory -> Math.max(0, inventory.getQuantityStock()))
            .orElse(0);
        Inventory savedInventory = applyStockDelta(product, quantity);

        ImportReceipt receipt = new ImportReceipt();
        receipt.setImportDate(LocalDateTime.now());
        receipt.setSupplier(supplier);
        receipt.setStaff(staff);
        receipt.setTotalCost(importPrice.multiply(BigDecimal.valueOf(quantity)));
        ImportReceipt savedReceipt = importReceiptRepository.save(receipt);

        ImportDetail detail = new ImportDetail();
        detail.setImportReceipt(savedReceipt);
        detail.setProduct(product);
        detail.setQuantity(quantity);
        detail.setImportPrice(importPrice);
        importDetailRepository.save(detail);

        publishStockChanged(
            productId,
            oldQty,
            quantity,
            savedInventory.getQuantityStock(),
            InventoryChangeType.IMPORT,
            staffAccountId,
            InventoryReferenceType.IMPORT_RECEIPT,
            savedReceipt.getReceiptId()
        );
    }

    @Override
    @Transactional
    public void importStock(Integer supplierId,
                            Integer staffAccountId,
                            List<Integer> productIds,
                            List<Integer> quantities,
                            List<BigDecimal> importPrices) {
        validateImportBatchPayload(productIds, quantities, importPrices);

        Supplier supplier = requireSupplier(supplierId);
        Staff staff = requireStaff(staffAccountId);

        ImportReceipt receipt = new ImportReceipt();
        receipt.setImportDate(LocalDateTime.now());
        receipt.setSupplier(supplier);
        receipt.setStaff(staff);

        BigDecimal totalCost = BigDecimal.ZERO;
        for (int i = 0; i < productIds.size(); i++) {
            int quantity = quantities.get(i);
            BigDecimal price = importPrices.get(i);
            validatePositiveQuantity(quantity, "Số lượng nhập ở dòng " + (i + 1) + " phải lớn hơn 0.");
            validatePositivePrice(price, "Giá nhập ở dòng " + (i + 1) + " phải lớn hơn 0.");
            totalCost = totalCost.add(price.multiply(BigDecimal.valueOf(quantity)));
        }
        receipt.setTotalCost(totalCost);

        ImportReceipt savedReceipt = importReceiptRepository.save(receipt);

        for (int i = 0; i < productIds.size(); i++) {
            Integer productId = productIds.get(i);
            int quantity = quantities.get(i);
            BigDecimal price = importPrices.get(i);

            Product product = requireProduct(productId);
            int oldQty = inventoryRepository.findByProductProductId(productId)
                .map(inventory -> Math.max(0, inventory.getQuantityStock()))
                .orElse(0);
            Inventory savedInventory = applyStockDelta(product, quantity);

            ImportDetail detail = new ImportDetail();
            detail.setImportReceipt(savedReceipt);
            detail.setProduct(product);
            detail.setQuantity(quantity);
            detail.setImportPrice(price);
            importDetailRepository.save(detail);
            savedReceipt.getDetails().add(detail);

            publishStockChanged(
                productId,
                oldQty,
                quantity,
                savedInventory.getQuantityStock(),
                InventoryChangeType.IMPORT,
                staffAccountId,
                InventoryReferenceType.IMPORT_RECEIPT,
                savedReceipt.getReceiptId()
            );
        }
    }

    @Override
    @Transactional
    public void deductStockForSale(Integer productId, int quantity, Integer staffAccountId, Integer orderId) {
        validatePositiveQuantity(quantity, "Số lượng bán phải lớn hơn 0.");

        Product product = requireProduct(productId);
        int oldQty = inventoryRepository.findByProductProductId(productId)
            .map(inventory -> Math.max(0, inventory.getQuantityStock()))
            .orElse(0);

        Inventory savedInventory = applyStockDelta(product, -quantity);

        publishStockChanged(
            productId,
            oldQty,
            -quantity,
            savedInventory.getQuantityStock(),
            InventoryChangeType.SALE,
            staffAccountId,
            InventoryReferenceType.ORDER,
            orderId
        );
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

    private Product requireProduct(Integer productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
    }

    private Supplier requireSupplier(Integer supplierId) {
        return supplierRepository.findById(supplierId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà cung cấp."));
    }

    private Staff requireStaff(Integer staffAccountId) {
        return staffRepository.findById(staffAccountId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin nhân viên nhập hàng."));
    }

    private void validatePositiveQuantity(int quantity, String message) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validatePositivePrice(BigDecimal price, String message) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateImportBatchPayload(List<Integer> productIds,
                                            List<Integer> quantities,
                                            List<BigDecimal> importPrices) {
        if (productIds == null || quantities == null || importPrices == null ||
            productIds.isEmpty() || productIds.size() != quantities.size() || quantities.size() != importPrices.size()) {
            throw new IllegalArgumentException("Dữ liệu sản phẩm nhập kho không hợp lệ.");
        }
    }

    private Inventory applyStockDelta(Product product, int delta) {
        Optional<Inventory> optionalInventory = inventoryRepository.findByProductProductId(product.getProductId());

        Inventory inventory;
        if (optionalInventory.isPresent()) {
            inventory = optionalInventory.get();
        } else {
            if (delta < 0) {
                throw new IllegalArgumentException("Sản phẩm chưa có tồn kho để trừ.");
            }
            inventory = new Inventory();
            inventory.setProduct(product);
            inventory.setQuantityStock(0);
        }

        int oldQty = Math.max(0, inventory.getQuantityStock());
        int newQty = oldQty + delta;
        if (newQty < 0) {
            throw new IllegalArgumentException(
                "Không đủ tồn kho cho sản phẩm ID " + product.getProductId() + ". Còn " + oldQty + "."
            );
        }

        inventory.setQuantityStock(newQty);
        return inventoryRepository.save(inventory);
    }

    private void publishStockChanged(Integer productId,
                                     int oldQty,
                                     int delta,
                                     int newQty,
                                     InventoryChangeType changeType,
                                     Integer actorAccountId,
                                     InventoryReferenceType referenceType,
                                     Integer referenceId) {
        eventPublisher.publishEvent(new InventoryStockChangedEvent(
            productId,
            oldQty,
            delta,
            newQty,
            changeType,
            actorAccountId,
            referenceType,
            referenceId,
            LocalDateTime.now()
        ));
    }
}
