package com.example.Jewelry.template.pattern;

import com.example.Jewelry.model.entity.Inventory;
import com.example.Jewelry.repository.InventoryRepository;
import com.example.Jewelry.template.ReportRow;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryReportTemplate extends ReportTemplate {

    private final InventoryRepository inventoryRepository;

    public InventoryReportTemplate(InventoryRepository inventoryRepository) {
        super("Inventory report");
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    protected List<ReportRow> loadRawData(LocalDate start, LocalDate end) {
        LocalDateTime snapshotTime = LocalDateTime.now();
        return inventoryRepository.findAll().stream()
            .map(inventory -> toRow(inventory, snapshotTime))
            .collect(Collectors.toList());
    }

    private ReportRow toRow(Inventory inventory, LocalDateTime snapshotTime) {
        String label = inventory.getProduct() == null ? "Unknown" : inventory.getProduct().getProductName();
        BigDecimal metric = BigDecimal.valueOf(inventory.getQuantityStock());
        return new ReportRow(label, metric, inventory.getQuantityStock(), snapshotTime);
    }
}

