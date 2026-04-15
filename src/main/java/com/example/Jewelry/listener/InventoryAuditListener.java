package com.example.Jewelry.listener;

import com.example.Jewelry.event.InventoryStockChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


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
    }
}
