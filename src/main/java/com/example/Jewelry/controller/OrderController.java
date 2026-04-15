package com.example.Jewelry.controller;

import com.example.Jewelry.model.entity.Customer;
import com.example.Jewelry.model.entity.Inventory;
import com.example.Jewelry.model.entity.Order;
import com.example.Jewelry.model.entity.OrderDetail;
import com.example.Jewelry.model.entity.Product;
import com.example.Jewelry.model.entity.Staff;
import com.example.Jewelry.model.entity.Voucher;
import com.example.Jewelry.payment.strategy.BankTransferPaymentStrategy;
import com.example.Jewelry.payment.strategy.PaymentExecutionResult;
import com.example.Jewelry.payment.strategy.MomoVnpayPaymentStrategy;
import com.example.Jewelry.service.CustomerService;
import com.example.Jewelry.service.InventoryService;
import com.example.Jewelry.service.LowStockAlertService;
import com.example.Jewelry.service.OrderService;
import com.example.Jewelry.service.PaymentService;
import com.example.Jewelry.service.ProductService;
import com.example.Jewelry.service.StaffService;
import com.example.Jewelry.service.VoucherService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping({"/orders", "/staff/orders"})
public class OrderController {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductService productService;
    private final CustomerService customerService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final StaffService staffService;
    private final InventoryService inventoryService;
    private final VoucherService voucherService;
    private final LowStockAlertService lowStockAlertService;
    private final ObjectMapper objectMapper;

    public OrderController(ProductService productService,
                           CustomerService customerService,
                           OrderService orderService,
                           PaymentService paymentService,
                           StaffService staffService,
                           InventoryService inventoryService,
                           VoucherService voucherService,
                           LowStockAlertService lowStockAlertService,
                           ObjectMapper objectMapper) {
        this.productService = productService;
        this.customerService = customerService;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.staffService = staffService;
        this.inventoryService = inventoryService;
        this.voucherService = voucherService;
        this.lowStockAlertService = lowStockAlertService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public String list(Model model, HttpSession session) {
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("staffProducts", productService.findAll().stream()
            .map(this::toStaffProductView)
            .sorted(Comparator.comparing(StaffProductView::productName))
            .toList());
        model.addAttribute("lowStockThreshold", LOW_STOCK_THRESHOLD);
        model.addAttribute("staffOrderViews", buildStaffOrderViews(session));

        // Observer alerts panel (always visible on page).
        List<LowStockAlertService.AlertMessage> lowStockAlerts = lowStockAlertService.getAlerts();
        model.addAttribute("lowStockAlerts", lowStockAlerts);

        // One-time toast payload after checkout success.
        if (Boolean.TRUE.equals(model.asMap().get("showLowStockToast"))) {
            int beforeCount = parseAlertCount(model.asMap().get("lowStockAlertCountBefore"));
            List<LowStockAlertService.AlertMessage> checkoutAlerts = collectNewLowStockAlerts(lowStockAlerts, beforeCount, 3);
            model.addAttribute("checkoutLowStockAlerts", checkoutAlerts);
        }
        return "staff/orders";
    }

    @PostMapping
    @Transactional
    public String create(@RequestParam Integer customerId,
                         @RequestParam String cartPayload,
                         @RequestParam(defaultValue = "checkout") String orderAction,
                         @RequestParam(defaultValue = "CASH") String paymentMethod,
                         @RequestParam(required = false) String appliedVoucherCode,
                         @RequestParam(required = false) String currentVoucherCode,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        Staff staff = resolveCurrentStaff(session);
        if (staff == null) {
            redirectAttributes.addFlashAttribute("error", "Phiên đăng nhập đã hết hạn hoặc tài khoản không hợp lệ.");
            return "redirect:/auth/login";
        }

        Customer customer = customerService.findById(customerId).orElse(null);
        if (customer == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy khách hàng.");
            return "redirect:/staff/orders";
        }

        Map<Integer, Integer> normalizedItems;
        try {
            normalizedItems = normalizeCartPayload(cartPayload);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/staff/orders";
        }

        if (normalizedItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Đơn hàng chưa có sản phẩm nào.");
            return "redirect:/staff/orders";
        }

        boolean checkoutNow = "checkout".equalsIgnoreCase(orderAction);
        boolean pendingPaymentMethod = checkoutNow && isPendingPaymentMethod(paymentMethod);
        boolean shouldCaptureImmediately = checkoutNow && !pendingPaymentMethod;
        int lowStockAlertCountBefore = shouldCaptureImmediately ? lowStockAlertService.getAlerts().size() : -1;
        List<PendingOrderLine> pendingLines = new ArrayList<>();
        BigDecimal computedTotal = BigDecimal.ZERO;

        for (Map.Entry<Integer, Integer> item : normalizedItems.entrySet()) {
            Integer productId = item.getKey();
            Integer quantity = item.getValue();

            Product product = productService.findById(productId).orElse(null);
            if (product == null) {
                redirectAttributes.addFlashAttribute("error", "Sản phẩm ID " + productId + " không tồn tại.");
                return "redirect:/staff/orders";
            }
            if (product.getBasePrice() == null || product.getBasePrice().signum() <= 0) {
                redirectAttributes.addFlashAttribute("error", "Giá sản phẩm không hợp lệ: " + product.getProductName());
                return "redirect:/staff/orders";
            }

            Inventory inventory = product.getInventory();
            int currentStock = inventory == null ? 0 : Math.max(0, inventory.getQuantityStock());
            if (checkoutNow && quantity > currentStock) {
                redirectAttributes.addFlashAttribute(
                    "error",
                    "Tồn kho không đủ cho " + product.getProductName() + ". Còn " + currentStock + "."
                );
                return "redirect:/staff/orders";
            }

            BigDecimal lineTotal = product.getBasePrice().multiply(BigDecimal.valueOf(quantity));
            computedTotal = computedTotal.add(lineTotal);
            pendingLines.add(new PendingOrderLine(product, inventory, quantity, product.getBasePrice()));
        }

        Order order = new Order();
        order.setOrderNumber("ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(checkoutNow ? (pendingPaymentMethod ? "PENDING_PAYMENT" : "PAID") : "PENDING");
        order.setCustomer(customer);
        order.setStaff(staff);

        Voucher appliedVoucher = null;
        BigDecimal finalTotal = computedTotal;
        String voucherCode = normalizeVoucherCode(appliedVoucherCode, currentVoucherCode);
        if (voucherCode != null) {
            try {
                appliedVoucher = resolveVoucherByCode(voucherCode);
                finalTotal = applyVoucherDiscount(computedTotal, appliedVoucher);
            } catch (IllegalArgumentException ex) {
                redirectAttributes.addFlashAttribute("error", ex.getMessage());
                return "redirect:/staff/orders";
            }
        }
        order.setVoucher(appliedVoucher);
        order.setFinalTotal(finalTotal);

        for (PendingOrderLine line : pendingLines) {
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(line.product());
            detail.setQuantity(line.quantity());
            detail.setUnitPrice(line.unitPrice());
            order.getOrderDetails().add(detail);

            // [OBSERVER] Gọi deductStockForSale để Observer pattern phát event
            if (shouldCaptureImmediately) {
                inventoryService.deductStockForSale(
                    line.product().getProductId(),
                    line.quantity(),
                    staff.getAccountId(),
                    null  // orderId chưa có vì chưa save
                );
            }
        }

        PaymentExecutionResult paymentResult = null;
        if (checkoutNow) {
            try {
                String orderInfo = "Thanh toan don " + order.getOrderNumber();
                paymentResult = paymentService.applyPaymentForOrder(order, paymentMethod, finalTotal, orderInfo);
            } catch (IllegalArgumentException ex) {
                redirectAttributes.addFlashAttribute("error", ex.getMessage());
                return "redirect:/staff/orders";
            } catch (IllegalStateException ex) {
                redirectAttributes.addFlashAttribute("error", ex.getMessage());
                return "redirect:/staff/orders";
            }
        }

        Order savedOrder = orderService.save(order);
        if (checkoutNow) {
            if (paymentResult != null && paymentResult.externalPaymentRequired()) {
                String qrUrl = resolveQrUrl(paymentResult);
                redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã tạo mã QR cho đơn " + savedOrder.getOrderNumber()
                        + " (" + displayPaymentMethod(paymentMethod) + "). Vui lòng quét mã để thanh toán."
                );
                redirectAttributes.addFlashAttribute("paymentQrUrl", qrUrl);
                redirectAttributes.addFlashAttribute("paymentPayUrl", paymentResult.payUrl());
                redirectAttributes.addFlashAttribute("paymentOrderInfo", paymentResult.orderInfo());
                redirectAttributes.addFlashAttribute("paymentAmount", savedOrder.getFinalTotal());
                redirectAttributes.addFlashAttribute("paymentMethodLabel", displayPaymentMethod(paymentMethod));
                redirectAttributes.addFlashAttribute("paymentOrderId", savedOrder.getOrderId());
            } else {
                redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã thanh toán đơn " + savedOrder.getOrderNumber()
                        + " bằng " + displayPaymentMethod(paymentMethod)
                        + " - " + formatCurrency(savedOrder.getFinalTotal())
                );
            }
        } else {
            redirectAttributes.addFlashAttribute("success", "Đã lưu đơn chờ thanh toán: " + savedOrder.getOrderNumber());
        }

        if (shouldCaptureImmediately) {
            redirectAttributes.addFlashAttribute("showLowStockToast", true);
            redirectAttributes.addFlashAttribute("lowStockAlertCountBefore", lowStockAlertCountBefore);
        }
        return "redirect:/staff/orders";
    }

    @PostMapping("/{orderId}/cancel")
    @Transactional
    public String cancel(@PathVariable Integer orderId,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        Order order = orderService.findById(orderId).orElse(null);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng cần hủy.");
            return "redirect:/staff/orders";
        }

        Integer accountId = (Integer) session.getAttribute("accountId");
        String roleName = (String) session.getAttribute("roleName");
        if (!canManageOrder(order, accountId, roleName)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thao tác đơn hàng này.");
            return "redirect:/staff/orders";
        }

        if ("PAID".equalsIgnoreCase(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Không thể hủy đơn đã thanh toán.");
            return "redirect:/staff/orders";
        }
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Đơn hàng đã được hủy trước đó.");
            return "redirect:/staff/orders";
        }

        order.setStatus("CANCELLED");
        orderService.save(order);
        redirectAttributes.addFlashAttribute("success", "Đã hủy đơn hàng " + order.getOrderNumber() + ".");
        return "redirect:/staff/orders";
    }

    @PostMapping("/{orderId}/checkout")
    @Transactional
    public String checkout(@PathVariable Integer orderId,
                           @RequestParam(defaultValue = "CASH") String paymentMethod,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        Order order = orderService.findById(orderId).orElse(null);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng cần thanh toán.");
            return "redirect:/staff/orders";
        }

        Integer accountId = (Integer) session.getAttribute("accountId");
        String roleName = (String) session.getAttribute("roleName");
        if (!canManageOrder(order, accountId, roleName)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thao tác đơn hàng này.");
            return "redirect:/staff/orders";
        }

        String currentStatus = order.getStatus() == null ? "PENDING" : order.getStatus().trim().toUpperCase(Locale.ROOT);
        if ("PAID".equals(currentStatus)) {
            redirectAttributes.addFlashAttribute("error", "Đơn hàng này đã thanh toán trước đó.");
            return "redirect:/staff/orders";
        }
        if ("CANCELLED".equals(currentStatus)) {
            redirectAttributes.addFlashAttribute("error", "Không thể thanh toán đơn đã hủy.");
            return "redirect:/staff/orders";
        }

        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Đơn hàng không có chi tiết sản phẩm để thanh toán.");
            return "redirect:/staff/orders";
        }

        for (OrderDetail detail : order.getOrderDetails()) {
            Product product = detail.getProduct();
            if (product == null) {
                redirectAttributes.addFlashAttribute("error", "Đơn hàng có dòng sản phẩm không hợp lệ.");
                return "redirect:/staff/orders";
            }

            Inventory inventory = product.getInventory();
            int stock = inventory == null ? 0 : Math.max(0, inventory.getQuantityStock());
            if (detail.getQuantity() > stock) {
                redirectAttributes.addFlashAttribute(
                    "error",
                    "Không đủ tồn kho cho " + product.getProductName() + ". Còn " + stock + "."
                );
                return "redirect:/staff/orders";
            }
        }

        boolean pendingPaymentMethod = isPendingPaymentMethod(paymentMethod);
        int lowStockAlertCountBefore = !pendingPaymentMethod ? lowStockAlertService.getAlerts().size() : -1;
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderDetail detail : order.getOrderDetails()) {
            Product product = detail.getProduct();
            Inventory inventory = product.getInventory();
            if (inventory == null && !pendingPaymentMethod) {
                redirectAttributes.addFlashAttribute(
                    "error",
                    "Sản phẩm " + product.getProductName() + " chưa có tồn kho để trừ."
                );
                return "redirect:/staff/orders";
            }
            // [OBSERVER] Gọi deductStockForSale để Observer pattern phát event
            if (!pendingPaymentMethod) {
                inventoryService.deductStockForSale(
                    product.getProductId(),
                    detail.getQuantity(),
                    accountId,
                    orderId
                );
            }

            BigDecimal unitPrice = detail.getUnitPrice() == null ? BigDecimal.ZERO : detail.getUnitPrice();
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(Math.max(0, detail.getQuantity()))));
        }

        BigDecimal total = applyVoucherDiscount(subtotal, order.getVoucher());
        order.setFinalTotal(total);
        order.setStatus(pendingPaymentMethod ? "PENDING_PAYMENT" : "PAID");
        PaymentExecutionResult paymentResult;
        try {
            String orderInfo = "Thanh toan don " + order.getOrderNumber();
            paymentResult = paymentService.applyPaymentForOrder(order, paymentMethod, total, orderInfo);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/staff/orders";
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/staff/orders";
        }
        orderService.save(order);

        if (paymentResult.externalPaymentRequired()) {
            String qrUrl = resolveQrUrl(paymentResult);
            redirectAttributes.addFlashAttribute(
                "success",
                "Đã tạo mã QR cho đơn " + order.getOrderNumber()
                    + " (" + displayPaymentMethod(paymentMethod) + "). Vui lòng quét mã để thanh toán."
            );
            redirectAttributes.addFlashAttribute("paymentQrUrl", qrUrl);
            redirectAttributes.addFlashAttribute("paymentPayUrl", paymentResult.payUrl());
            redirectAttributes.addFlashAttribute("paymentOrderInfo", paymentResult.orderInfo());
            redirectAttributes.addFlashAttribute("paymentAmount", order.getFinalTotal());
            redirectAttributes.addFlashAttribute("paymentMethodLabel", displayPaymentMethod(paymentMethod));
            redirectAttributes.addFlashAttribute("paymentOrderId", order.getOrderId());
        } else {
            redirectAttributes.addFlashAttribute(
                "success",
                "Đã thanh toán đơn " + order.getOrderNumber()
                    + " bằng " + displayPaymentMethod(paymentMethod)
                    + " - " + formatCurrency(order.getFinalTotal())
            );
        }

        if (!pendingPaymentMethod) {
            redirectAttributes.addFlashAttribute("showLowStockToast", true);
            redirectAttributes.addFlashAttribute("lowStockAlertCountBefore", lowStockAlertCountBefore);
        }
        return "redirect:/staff/orders";
    }

    @PostMapping("/{orderId}/confirm-payment")
    @Transactional
    public String confirmPendingPayment(@PathVariable Integer orderId,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        Order order = orderService.findById(orderId).orElse(null);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng cần xác nhận thanh toán.");
            return "redirect:/staff/orders";
        }

        Integer accountId = (Integer) session.getAttribute("accountId");
        String roleName = (String) session.getAttribute("roleName");
        if (!canManageOrder(order, accountId, roleName)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thao tác đơn hàng này.");
            return "redirect:/staff/orders";
        }

        String currentStatus = order.getStatus() == null ? "PENDING" : order.getStatus().trim().toUpperCase(Locale.ROOT);
        if ("PAID".equals(currentStatus)) {
            redirectAttributes.addFlashAttribute("error", "Đơn hàng này đã thanh toán trước đó.");
            return "redirect:/staff/orders";
        }
        if ("CANCELLED".equals(currentStatus)) {
            redirectAttributes.addFlashAttribute("error", "Không thể xác nhận đơn đã hủy.");
            return "redirect:/staff/orders";
        }
        if (!"PENDING_PAYMENT".equals(currentStatus)) {
            redirectAttributes.addFlashAttribute("error", "Đơn hàng chưa ở trạng thái chờ xác nhận thanh toán.");
            return "redirect:/staff/orders";
        }
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Đơn hàng không có chi tiết sản phẩm để xác nhận.");
            return "redirect:/staff/orders";
        }

        for (OrderDetail detail : order.getOrderDetails()) {
            Product product = detail.getProduct();
            if (product == null) {
                redirectAttributes.addFlashAttribute("error", "Đơn hàng có dòng sản phẩm không hợp lệ.");
                return "redirect:/staff/orders";
            }
            Inventory inventory = product.getInventory();
            int stock = inventory == null ? 0 : Math.max(0, inventory.getQuantityStock());
            if (detail.getQuantity() > stock) {
                redirectAttributes.addFlashAttribute(
                    "error",
                    "Không đủ tồn kho cho " + product.getProductName() + ". Còn " + stock + "."
                );
                return "redirect:/staff/orders";
            }
        }

        int lowStockAlertCountBefore = lowStockAlertService.getAlerts().size();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderDetail detail : order.getOrderDetails()) {
            Product product = detail.getProduct();
            Inventory inventory = product.getInventory();
            if (inventory == null) {
                redirectAttributes.addFlashAttribute(
                    "error",
                    "Sản phẩm " + product.getProductName() + " chưa có tồn kho để trừ."
                );
                return "redirect:/staff/orders";
            }

            inventoryService.deductStockForSale(
                product.getProductId(),
                detail.getQuantity(),
                accountId,
                orderId
            );

            BigDecimal unitPrice = detail.getUnitPrice() == null ? BigDecimal.ZERO : detail.getUnitPrice();
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(Math.max(0, detail.getQuantity()))));
        }

        BigDecimal total = applyVoucherDiscount(subtotal, order.getVoucher());
        order.setFinalTotal(total);
        order.setStatus("PAID");
        orderService.save(order);

        String paymentMethod = order.getPayment() == null ? null : order.getPayment().getMethod();
        redirectAttributes.addFlashAttribute(
            "success",
            "Đã xác nhận thanh toán đơn " + order.getOrderNumber()
                + " bằng " + displayPaymentMethod(paymentMethod)
                + " - " + formatCurrency(order.getFinalTotal())
        );
        redirectAttributes.addFlashAttribute("showLowStockToast", true);
        redirectAttributes.addFlashAttribute("lowStockAlertCountBefore", lowStockAlertCountBefore);
        return "redirect:/staff/orders";
    }

    private List<StaffOrderView> buildStaffOrderViews(HttpSession session) {
        Integer accountId = (Integer) session.getAttribute("accountId");
        String roleName = (String) session.getAttribute("roleName");

        return orderService.findAll().stream()
            .filter(order -> canManageOrder(order, accountId, roleName))
            .sorted(Comparator.comparing(Order::getOrderDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .limit(25)
            .map(this::toStaffOrderView)
            .toList();
    }

    private StaffOrderView toStaffOrderView(Order order) {
        List<InvoiceLineView> invoiceLines = order.getOrderDetails() == null
            ? List.of()
            : order.getOrderDetails().stream()
                .map(detail -> {
                    BigDecimal unitPrice = detail.getUnitPrice() == null ? BigDecimal.ZERO : detail.getUnitPrice();
                    int quantity = Math.max(0, detail.getQuantity());
                    return new InvoiceLineView(
                        detail.getProduct() == null ? "Sản phẩm" : detail.getProduct().getProductName(),
                        quantity,
                        unitPrice,
                        unitPrice.multiply(BigDecimal.valueOf(quantity))
                    );
                })
                .toList();

        String linesJson;
        try {
            linesJson = objectMapper.writeValueAsString(invoiceLines);
        } catch (IOException ex) {
            linesJson = "[]";
        }

        String customerName = order.getCustomer() == null ? "Không xác định" : order.getCustomer().getCustomerName();
        String dateDisplay = order.getOrderDate() == null
            ? "--"
            : order.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String status = order.getStatus() == null ? "PENDING" : order.getStatus().trim().toUpperCase(Locale.ROOT);
        boolean canCancel = "PENDING".equals(status) || "PENDING_PAYMENT".equals(status);
        boolean canCheckout = "PENDING".equals(status) || "PENDING_PAYMENT".equals(status);

        return new StaffOrderView(
            order.getOrderId(),
            order.getOrderNumber(),
            customerName,
            status,
            order.getFinalTotal() == null ? BigDecimal.ZERO : order.getFinalTotal(),
            dateDisplay,
            canCancel,
            canCheckout,
            linesJson
        );
    }

    private Map<Integer, Integer> normalizeCartPayload(String cartPayload) {
        if (cartPayload == null || cartPayload.isBlank()) {
            return Map.of();
        }

        List<CartPayloadItem> parsedItems;
        try {
            parsedItems = objectMapper.readValue(cartPayload, new TypeReference<List<CartPayloadItem>>() {
            });
        } catch (IOException ex) {
            throw new IllegalArgumentException("Dữ liệu giỏ hàng không hợp lệ.");
        }

        Map<Integer, Integer> normalized = new LinkedHashMap<>();
        for (CartPayloadItem item : parsedItems) {
            if (item == null || item.productId() == null || item.quantity() == null) {
                continue;
            }
            if (item.quantity() <= 0) {
                continue;
            }
            normalized.merge(item.productId(), item.quantity(), Integer::sum);
        }
        return normalized;
    }

    private Staff resolveCurrentStaff(HttpSession session) {
        Integer accountId = (Integer) session.getAttribute("accountId");
        if (accountId == null) {
            return null;
        }
        return staffService.findById(accountId).orElse(null);
    }

    private boolean canManageOrder(Order order, Integer accountId, String roleName) {
        if (order == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(roleName)) {
            return true;
        }
        if (accountId == null || order.getStaff() == null) {
            return false;
        }
        return Objects.equals(order.getStaff().getAccountId(), accountId);
    }

    private StaffProductView toStaffProductView(Product product) {
        int stock = product.getInventory() == null ? 0 : Math.max(0, product.getInventory().getQuantityStock());
        String categoryName = product.getCategory() == null ? "Khác" : product.getCategory().getCategoryName();
        BigDecimal price = product.getBasePrice() == null ? BigDecimal.ZERO : product.getBasePrice();
        return new StaffProductView(
            product.getProductId(),
            product.getProductCode(),
            product.getProductName(),
            categoryName,
            price,
            stock,
            stock < LOW_STOCK_THRESHOLD
        );
    }

    @PostMapping("/{orderId}/apply-voucher")
    @Transactional
    public String applyVoucher(@PathVariable Integer orderId,
                               @RequestParam String voucherCode,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            Order order = orderService.findById(orderId).orElse(null);
            if (order == null) {
                redirectAttributes.addFlashAttribute("error", "Khong tim thay don hang.");
                return "redirect:/staff/orders";
            }

            Integer accountId = (Integer) session.getAttribute("accountId");
            String roleName = (String) session.getAttribute("roleName");
            if (!canManageOrder(order, accountId, roleName)) {
                redirectAttributes.addFlashAttribute("error", "Ban khong co quyen thao tac don hang nay.");
                return "redirect:/staff/orders";
            }

            if (voucherCode == null || voucherCode.isBlank()) {
                redirectAttributes.addFlashAttribute("error", "Vui long nhap ma voucher.");
                return "redirect:/staff/orders";
            }

            Voucher voucher = voucherService.findByCode(voucherCode).orElse(null);
            if (voucher == null) {
                redirectAttributes.addFlashAttribute("error", "Ma voucher khong hop le hoac khong ton tai.");
                return "redirect:/staff/orders";
            }
            if (!isVoucherActive(voucher)) {
                redirectAttributes.addFlashAttribute("error", "Voucher chua den thoi gian ap dung hoac da het han.");
                return "redirect:/staff/orders";
            }

            order.setVoucher(voucher);
            BigDecimal currentTotal = order.getFinalTotal() == null ? BigDecimal.ZERO : order.getFinalTotal();
            BigDecimal discountValue = voucher.getDiscountValue() == null ? BigDecimal.ZERO : voucher.getDiscountValue();
            BigDecimal newTotal = currentTotal.subtract(discountValue);
            if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
                newTotal = BigDecimal.ZERO;
            }

            order.setFinalTotal(newTotal);
            orderService.save(order);

            String successMessage = "Ap dung voucher thanh cong! Giam: "
                + formatCurrency(discountValue)
                + " | Tong tien sau giam: "
                + formatCurrency(newTotal);
            redirectAttributes.addFlashAttribute("success", successMessage);
            return "redirect:/staff/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Loi ap dung voucher: " + e.getMessage());
            return "redirect:/staff/orders";
        }
    }

    @PostMapping("/{orderId}/remove-voucher")
    @Transactional
    public String removeVoucher(@PathVariable Integer orderId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            Order order = orderService.findById(orderId).orElse(null);
            if (order == null) {
                redirectAttributes.addFlashAttribute("error", "Khong tim thay don hang.");
                return "redirect:/staff/orders";
            }

            Integer accountId = (Integer) session.getAttribute("accountId");
            String roleName = (String) session.getAttribute("roleName");
            if (!canManageOrder(order, accountId, roleName)) {
                redirectAttributes.addFlashAttribute("error", "Ban khong co quyen thao tac don hang nay.");
                return "redirect:/staff/orders";
            }

            if (order.getVoucher() == null) {
                redirectAttributes.addFlashAttribute("error", "Don hang nay khong co voucher duoc ap dung.");
                return "redirect:/staff/orders";
            }

            BigDecimal discountValue = order.getVoucher().getDiscountValue() == null
                ? BigDecimal.ZERO
                : order.getVoucher().getDiscountValue();
            BigDecimal currentTotal = order.getFinalTotal() == null ? BigDecimal.ZERO : order.getFinalTotal();
            BigDecimal originalTotal = currentTotal.add(discountValue);

            order.setVoucher(null);
            order.setFinalTotal(originalTotal);
            orderService.save(order);

            redirectAttributes.addFlashAttribute(
                "success",
                "Huy voucher thanh cong! Tong tien tro lai: " + formatCurrency(originalTotal)
            );
            return "redirect:/staff/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Loi huy voucher: " + e.getMessage());
            return "redirect:/staff/orders";
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 VND";
        }
        return String.format("%,.0f VND", amount);
    }

    private String displayPaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return "Tiền mặt";
        }
        return switch (paymentMethod.trim().toUpperCase(Locale.ROOT)) {
            case "BANK_TRANSFER" -> "Chuyển khoản";
            case "MOMO_VNPAY" -> "MoMo/VNPay";
            default -> "Tiền mặt";
        };
    }

    private boolean isPendingPaymentMethod(String paymentMethod) {
        return MomoVnpayPaymentStrategy.METHOD_CODE.equalsIgnoreCase(paymentMethod)
            || BankTransferPaymentStrategy.METHOD_CODE.equalsIgnoreCase(paymentMethod);
    }

    private String resolveQrUrl(PaymentExecutionResult paymentResult) {
        if (paymentResult == null) {
            return null;
        }
        if (paymentResult.qrCodeUrl() != null && !paymentResult.qrCodeUrl().isBlank()) {
            return paymentResult.qrCodeUrl();
        }
        if (paymentResult.payUrl() == null || paymentResult.payUrl().isBlank()) {
            return null;
        }
        return "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data="
            + URLEncoder.encode(paymentResult.payUrl(), StandardCharsets.UTF_8);
    }

    private boolean isVoucherActive(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = voucher.getStartDate();
        LocalDateTime endDate = voucher.getEndDate();

        if (startDate != null && now.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && now.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    private Voucher resolveVoucherByCode(String voucherCode) {
        String normalizedCode = voucherCode == null ? "" : voucherCode.trim();
        if (normalizedCode.isEmpty()) {
            throw new IllegalArgumentException("Mã voucher không hợp lệ.");
        }
        Voucher voucher = voucherService.findByCode(normalizedCode).orElse(null);
        if (voucher == null) {
            throw new IllegalArgumentException("Mã voucher không hợp lệ hoặc không tồn tại.");
        }
        if (!isVoucherActive(voucher)) {
            throw new IllegalArgumentException("Voucher chưa đến thời gian áp dụng hoặc đã hết hạn.");
        }
        return voucher;
    }

    private String normalizeVoucherCode(String appliedVoucherCode, String currentVoucherCode) {
        if (appliedVoucherCode != null && !appliedVoucherCode.isBlank()) {
            return appliedVoucherCode.trim();
        }
        if (currentVoucherCode != null && !currentVoucherCode.isBlank()) {
            return currentVoucherCode.trim();
        }
        return null;
    }

    private BigDecimal applyVoucherDiscount(BigDecimal subtotal, Voucher voucher) {
        BigDecimal baseTotal = subtotal == null ? BigDecimal.ZERO : subtotal.max(BigDecimal.ZERO);
        if (voucher == null) {
            return baseTotal;
        }
        BigDecimal discount = voucher.getDiscountValue() == null ? BigDecimal.ZERO : voucher.getDiscountValue();
        BigDecimal discountedTotal = baseTotal.subtract(discount);
        return discountedTotal.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : discountedTotal;
    }

    private int parseAlertCount(Object rawValue) {
        if (rawValue instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        return 0;
    }

    private List<LowStockAlertService.AlertMessage> collectNewLowStockAlerts(
        List<LowStockAlertService.AlertMessage> alerts,
        int beforeCount,
        int maxItems
    ) {
        if (alerts == null || alerts.isEmpty() || maxItems <= 0) {
            return List.of();
        }
        int safeBefore = Math.max(0, beforeCount);
        int newCount = Math.max(0, alerts.size() - safeBefore);
        if (newCount == 0) {
            return List.of();
        }
        return alerts.stream().limit(Math.min(newCount, maxItems)).toList();
    }


    public record StaffProductView(Integer productId,
                                   String productCode,
                                   String productName,
                                   String categoryName,
                                   BigDecimal basePrice,
                                   int stock,
                                   boolean lowStock) {
    }

    public record StaffOrderView(Integer orderId,
                                 String orderNumber,
                                 String customerName,
                                 String status,
                                 BigDecimal finalTotal,
                                 String orderDate,
                                 boolean canCancel,
                                 boolean canCheckout,
                                 String invoiceLinesJson) {
    }

    public record InvoiceLineView(String productName,
                                  int quantity,
                                  BigDecimal unitPrice,
                                  BigDecimal lineTotal) {
    }

    public record CartPayloadItem(Integer productId, Integer quantity) {
    }

    private record PendingOrderLine(Product product, Inventory inventory, int quantity, BigDecimal unitPrice) {
    }
}
