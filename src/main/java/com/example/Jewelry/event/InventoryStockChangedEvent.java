package com.example.Jewelry.event;

import java.time.LocalDateTime;

/**
 * Event object mô tả sự kiện "tồn kho vừa thay đổi".
 *
 * Tại sao dùng class thay vì truyền tham số rời?
 * - Gom tất cả thông tin liên quan vào 1 đối tượng → dễ truyền giữa các tầng.
 * - Listener nhận được ĐẦY ĐỦ ngữ cảnh để xử lý (ai thay đổi, thay đổi bao nhiêu,
 *   từ chứng từ nào, khi nào).
 * - Thêm field mới không ảnh hưởng signature của listener.
 *
 * Giải thích các field:
 * - productId:       ID sản phẩm bị thay đổi tồn kho
 * - oldQuantity:     số lượng tồn TRƯỚC khi thay đổi
 * - delta:           độ chênh lệch (dương = nhập, âm = xuất)
 * - newQuantity:     số lượng tồn SAU khi thay đổi
 * - changeType:      loại biến động (IMPORT, SALE, ADJUSTMENT, RETURN)
 * - actorAccountId:  tài khoản thực hiện thao tác
 * - referenceType:   loại chứng từ nguồn (IMPORT_RECEIPT, ORDER...)
 * - referenceId:     ID chứng từ nguồn
 * - occurredAt:      thời điểm xảy ra
 */
public class InventoryStockChangedEvent {

    private final Integer productId;
    private final int oldQuantity;
    private final int delta;
    private final int newQuantity;
    private final InventoryChangeType changeType;
    private final Integer actorAccountId;
    private final InventoryReferenceType referenceType;
    private final Integer referenceId;
    private final LocalDateTime occurredAt;

    public InventoryStockChangedEvent(Integer productId,
                                       int oldQuantity,
                                       int delta,
                                       int newQuantity,
                                       InventoryChangeType changeType,
                                       Integer actorAccountId,
                                       InventoryReferenceType referenceType,
                                       Integer referenceId,
                                       LocalDateTime occurredAt) {
        this.productId = productId;
        this.oldQuantity = oldQuantity;
        this.delta = delta;
        this.newQuantity = newQuantity;
        this.changeType = changeType;
        this.actorAccountId = actorAccountId;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.occurredAt = occurredAt;
    }

    // --- Getter methods ---
    public Integer getProductId()                    { return productId; }
    public int getOldQuantity()                      { return oldQuantity; }
    public int getDelta()                            { return delta; }
    public int getNewQuantity()                      { return newQuantity; }
    public InventoryChangeType getChangeType()       { return changeType; }
    public Integer getActorAccountId()               { return actorAccountId; }
    public InventoryReferenceType getReferenceType() { return referenceType; }
    public Integer getReferenceId()                  { return referenceId; }
    public LocalDateTime getOccurredAt()             { return occurredAt; }

    // --- Helper methods ---
    /** Tồn kho tăng (nhập kho, trả hàng) */
    public boolean isIncrease() { return delta > 0; }

    /** Tồn kho giảm (bán hàng, điều chỉnh giảm) */
    public boolean isDecrease() { return delta < 0; }
}
