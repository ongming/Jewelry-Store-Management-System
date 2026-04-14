package com.example.Jewelry.listener;

import com.example.Jewelry.event.InventoryStockChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observer (Listener) #2: Ghi lịch sử biến động tồn kho (audit log).
 *
 * Tại sao cần audit log?
 * - Truy vết: biết AI đã thay đổi tồn kho, KHI NÀO, thay đổi BAO NHIÊU.
 * - Đối soát: so sánh tồn kho thực tế với lịch sử biến động.
 * - Bảo mật: phát hiện thao tác bất thường.
 *
 * Tại sao dùng @TransactionalEventListener(phase = AFTER_COMMIT)?
 * - Đảm bảo chỉ ghi audit khi dữ liệu tồn kho ĐÃ THỰC SỰ thay đổi
 *   trong database (transaction commit thành công).
 */
@Component
public class InventoryAuditListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(InventoryStockChangedEvent event) {
        System.out.println(
            "[INVENTORY_AUDIT]"
            + " productId=" + event.getProductId()
            + " | " + event.getOldQuantity() + " -> " + event.getNewQuantity()
            + " (delta=" + event.getDelta() + ")"
            + " | type=" + event.getChangeType()
            + " | ref=" + event.getReferenceType() + "#" + event.getReferenceId()
            + " | actor=" + event.getActorAccountId()
            + " | at=" + event.getOccurredAt()
        );
        // TODO tương lai: lưu vào bảng inventory_audit trong database
    }
}
