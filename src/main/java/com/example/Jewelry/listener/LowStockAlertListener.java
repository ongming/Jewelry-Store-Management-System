package com.example.Jewelry.listener;

import com.example.Jewelry.event.InventoryStockChangedEvent;
import com.example.Jewelry.model.entity.Product;
import com.example.Jewelry.repository.ProductRepository;
import com.example.Jewelry.service.LowStockAlertService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class LowStockAlertListener {

    // Ngưỡng cảnh báo tồn kho thấp (có thể đưa ra config sau này)
    private static final int LOW_STOCK_THRESHOLD = 5;

    private final LowStockAlertService lowStockAlertService;
    private final ProductRepository productRepository;

    public LowStockAlertListener(LowStockAlertService lowStockAlertService,
                                  ProductRepository productRepository) {
        this.lowStockAlertService = lowStockAlertService;
        this.productRepository = productRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(InventoryStockChangedEvent event) {
        // Chỉ cảnh báo khi tồn kho mới thấp hơn ngưỡng
        if (event.getNewQuantity() < LOW_STOCK_THRESHOLD) {
            // Lấy tên sản phẩm để hiển thị trên UI
            String productName = productRepository.findById(event.getProductId())
                .map(Product::getProductName)
                .orElse("Không rõ");

            // Lưu cảnh báo vào service để hiển thị trên UI
            lowStockAlertService.addAlert(
                event.getProductId(),
                productName,
                event.getNewQuantity(),
                LOW_STOCK_THRESHOLD,
                event.getChangeType().name()
            );

            // Vẫn giữ log console để theo dõi
            System.out.println(
                "[LOW_STOCK] CẢNH BÁO: Sản phẩm ID=" + event.getProductId()
                + " (" + productName + ")"
                + " tồn kho còn " + event.getNewQuantity()
                + " (ngưỡng=" + LOW_STOCK_THRESHOLD + ")"
                + " | Loại biến động: " + event.getChangeType()
                + " | Người thao tác: " + event.getActorAccountId()
            );
            // TODO tương lai: lưu vào bảng cảnh báo, gửi email/notification
        }
    }
}
