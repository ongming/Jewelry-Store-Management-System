# Next Steps - Jewelry Store Management System

## Current Status
- Done: Phase 1 (Manager) - Product and Category CRUD.
- Done: Business rules in service layer for Product/Category.
- Done: Admin UI pages for product/category management.

## Next Implementation Plan

### Phase 2 - Sales Staff Workflow (Priority High)
1. Product lookup for staff
- Add search by product name/code/category.
- Add stock visibility in product listing for sales.

2. Order creation
- Build order form: customer, staff, items, quantity.
- Auto-generate order number.
- Validate quantity against inventory.

3. Order update and cancel
- Allow add/remove order items before payment.
- Allow cancel only when payment is not completed.

4. Payment and invoice
- Create payment flow (cash/bank transfer).
- Calculate total: sum(unit_price * quantity).
- Add printable invoice view.

### Phase 3 - Inventory Management (Priority High)
1. Import receipt and import detail
- Create import receipt form linked to supplier and staff.
- Create import detail lines for products and import prices.

2. Inventory update rules
- Increase stock after import confirmation.
- Decrease stock after successful order payment.

3. Inventory monitoring
- Build inventory list page.
- Add low-stock warning (default threshold: 10).
- Add stock checking report page.

### Phase 4 - Manager Reports (Priority Medium)
1. Revenue report
- Revenue = sum(final_total) for paid orders.
- Add filter by date range.

2. Best-selling products
- Aggregate sold quantity by product.
- Show top-N selling products.

### Phase 5 - Customer-facing Flow (Priority Medium)
1. Replace mock product data in UiController with DB data.
2. Keep cart/checkout flow aligned with staff order processing.
3. Show finalized invoice information to customer.

## Cross-cutting Tasks
1. Validation and error handling
- Add consistent service exceptions.
- Add controller-level error messages and fallback pages.

2. Security basics
- Add session-based login state.
- Add role checks for admin/staff routes.

3. Testing
- Unit tests for service rules (especially delete constraints and payment logic).
- Integration tests for repository queries.

4. Data script updates
- Keep database.txt aligned with new business logic and constraints.
- Add seed data for order/payment/inventory test scenarios.

## Immediate Next Task (Start Here)
- Implement Sales Staff workflow: Order creation + payment + invoice (Phase 2, items 2 and 4 first).
