package com.example.Jewelry.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service lưu trữ danh sách cảnh báo tồn kho thấp trong bộ nhớ.
 *
 * Tại sao cần service này?
 * - LowStockAlertListener (Observer) phát hiện tồn kho thấp nhưng chỉ in console.
 * - Service này nhận cảnh báo từ listener và lưu lại để controller đẩy ra UI.
 * - Dùng CopyOnWriteArrayList để thread-safe (nhiều request có thể ghi/đọc cùng lúc).
 */
@Service
public class LowStockAlertService {

    private final List<AlertMessage> alerts = new CopyOnWriteArrayList<>();

    /**
     * Thêm một cảnh báo mới (được gọi từ LowStockAlertListener).
     */
    public void addAlert(Integer productId, String productName, int currentStock, int threshold, String changeType) {
        String message = "Sản phẩm \"" + productName + "\" (ID=" + productId + ") tồn kho còn "
            + currentStock + " (ngưỡng an toàn: " + threshold + "). Biến động: " + changeType;
        alerts.add(0, new AlertMessage(productId, message, LocalDateTime.now()));

        // Giữ tối đa 50 cảnh báo gần nhất
        while (alerts.size() > 50) {
            alerts.remove(alerts.size() - 1);
        }
    }

    /**
     * Lấy danh sách tất cả cảnh báo (controller gọi để đẩy ra view).
     */
    public List<AlertMessage> getAlerts() {
        return Collections.unmodifiableList(alerts);
    }

    /**
     * Xóa tất cả cảnh báo.
     */
    public void clearAlerts() {
        alerts.clear();
    }

    /**
     * Record chứa thông tin 1 cảnh báo.
     */
    public static class AlertMessage {
        private final Integer productId;
        private final String message;
        private final LocalDateTime timestamp;

        public AlertMessage(Integer productId, String message, LocalDateTime timestamp) {
            this.productId = productId;
            this.message = message;
            this.timestamp = timestamp;
        }

        public Integer getProductId() { return productId; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getFormattedTime() {
            return timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        }
    }
}
