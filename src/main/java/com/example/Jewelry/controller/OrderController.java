package com.example.Jewelry.controller;

import com.example.Jewelry.model.entity.Customer;
import com.example.Jewelry.model.entity.Inventory;
import com.example.Jewelry.model.entity.Order;
import com.example.Jewelry.model.entity.OrderDetail;
import com.example.Jewelry.model.entity.Product;
import com.example.Jewelry.model.entity.Staff;
import com.example.Jewelry.service.CustomerService;
import com.example.Jewelry.service.InventoryService;
import com.example.Jewelry.service.OrderService;
import com.example.Jewelry.service.ProductService;
import com.example.Jewelry.service.StaffService;
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
    private final StaffService staffService;
    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    public OrderController(ProductService productService,
                           CustomerService customerService,
                           OrderService orderService,
                           StaffService staffService,
                           InventoryService inventoryService,
                           ObjectMapper objectMapper) {
        this.productService = productService;
        this.customerService = customerService;
        this.orderService = orderService;
        this.staffService = staffService;
        this.inventoryService = inventoryService;
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
        return "staff/orders";
    }

    @PostMapping
    @Transactional
    public String create(@RequestParam Integer customerId,
                         @RequestParam String cartPayload,
                         @RequestParam(defaultValue = "checkout") String orderAction,
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
        order.setStatus(checkoutNow ? "PAID" : "PENDING");
        order.setFinalTotal(computedTotal);
        order.setCustomer(customer);
        order.setStaff(staff);

        for (PendingOrderLine line : pendingLines) {
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(line.product());
            detail.setQuantity(line.quantity());
            detail.setUnitPrice(line.unitPrice());
            order.getOrderDetails().add(detail);

            if (checkoutNow && line.inventory() != null) {
                line.inventory().updateStock(line.inventory().getQuantityStock() - line.quantity());
                inventoryService.save(line.inventory());
            }
        }

        Order savedOrder = orderService.save(order);
        if (checkoutNow) {
            redirectAttributes.addFlashAttribute(
                "success",
                "Đã thanh toán đơn " + savedOrder.getOrderNumber() + " - " + formatCurrency(savedOrder.getFinalTotal())
            );
        } else {
            redirectAttributes.addFlashAttribute("success", "Đã lưu đơn chờ thanh toán: " + savedOrder.getOrderNumber());
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

        BigDecimal total = BigDecimal.ZERO;
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
            inventory.updateStock(inventory.getQuantityStock() - detail.getQuantity());
            inventoryService.save(inventory);

            BigDecimal unitPrice = detail.getUnitPrice() == null ? BigDecimal.ZERO : detail.getUnitPrice();
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(Math.max(0, detail.getQuantity()))));
        }

        order.setFinalTotal(total);
        order.setStatus("PAID");
        orderService.save(order);

        redirectAttributes.addFlashAttribute(
            "success",
            "Đã thanh toán đơn " + order.getOrderNumber() + " - " + formatCurrency(order.getFinalTotal())
        );
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
        boolean canCancel = "PENDING".equals(status);
        boolean canCheckout = "PENDING".equals(status);

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

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 VND";
        }
        return String.format("%,.0f VND", amount);
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
