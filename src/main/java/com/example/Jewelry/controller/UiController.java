package com.example.Jewelry.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.Jewelry.model.entity.Customer;
import com.example.Jewelry.model.entity.Inventory;
import com.example.Jewelry.model.entity.Order;
import com.example.Jewelry.model.entity.OrderDetail;
import com.example.Jewelry.model.entity.Product;
import com.example.Jewelry.model.entity.ProductAttribute;
import com.example.Jewelry.model.entity.ProductImage;
import com.example.Jewelry.model.entity.Staff;
import com.example.Jewelry.service.CategoryService;
import com.example.Jewelry.service.CustomerService;
import com.example.Jewelry.service.InventoryService;
import com.example.Jewelry.service.OrderService;
import com.example.Jewelry.service.ProductService;
import com.example.Jewelry.service.StaffService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class UiController {

    private static final int PRODUCTS_PAGE_SIZE = 6;
    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CustomerService customerService;
    private final OrderService orderService;
    private final StaffService staffService;
    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    public UiController(ProductService productService,
                        CategoryService categoryService,
                        CustomerService customerService,
                        OrderService orderService,
                        StaffService staffService,
                        InventoryService inventoryService,
                        ObjectMapper objectMapper) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.customerService = customerService;
        this.orderService = orderService;
        this.staffService = staffService;
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
    }

    @GetMapping({"/", "/guest/home"})
    public String home(Model model) {
        List<ProductView> productViews = productService.findAll().stream()
            .map(this::toProductView)
            .toList();
        model.addAttribute("featuredProducts", productViews.stream().limit(4).toList());
        model.addAttribute("categories", categoryService.findAll().stream()
            .map(category -> category.getCategoryName())
            .toList());
        return "guest/home";
    }

    @GetMapping("/guest/products")
    public String products(@RequestParam(required = false) String category,
                           @RequestParam(required = false) Integer minPrice,
                           @RequestParam(required = false) Integer maxPrice,
                           @RequestParam(defaultValue = "priceAsc") String sort,
                           @RequestParam(defaultValue = "1") Integer page,
                           Model model) {
        List<ProductView> filtered = new ArrayList<>(productService.findAll().stream()
            .map(this::toProductView)
            .toList());
        if (category != null && !category.isBlank()) {
            filtered.removeIf(product -> !product.category().equalsIgnoreCase(category));
        }
        if (minPrice != null) {
            filtered.removeIf(product -> product.price() < minPrice);
        }
        if (maxPrice != null) {
            filtered.removeIf(product -> product.price() > maxPrice);
        }

        if ("priceDesc".equalsIgnoreCase(sort)) {
            filtered.sort(Comparator.comparingInt(ProductView::price).reversed());
        } else {
            filtered.sort(Comparator.comparingInt(ProductView::price));
            sort = "priceAsc";
        }

        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / PRODUCTS_PAGE_SIZE));
        int currentPage = Math.max(1, Math.min(page, totalPages));
        int fromIndex = (currentPage - 1) * PRODUCTS_PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PRODUCTS_PAGE_SIZE, totalItems);
        List<ProductView> pageItems = filtered.subList(fromIndex, toIndex);

        model.addAttribute("products", pageItems);
        model.addAttribute("selectedCategory", category == null ? "" : category);
        model.addAttribute("categories", categoryService.findAll().stream()
            .map(cat -> cat.getCategoryName())
            .collect(Collectors.toList()));
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("hasPrev", currentPage > 1);
        model.addAttribute("hasNext", currentPage < totalPages);
        model.addAttribute("pageItems", buildPageItems(currentPage, totalPages));
        return "guest/products";
    }

    @GetMapping("/guest/product-detail/{id}")
    public String productDetail(@PathVariable Integer id, Model model) {
        Optional<Product> product = productService.findById(id);
        if (product.isEmpty()) {
            return "redirect:/guest/products";
        }

        Product selectedProduct = product.get();
        List<ProductImage> detailImages = selectedProduct.getImages() == null
            ? List.of()
            : selectedProduct.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::isPrimary).reversed()
                    .thenComparingInt(ProductImage::getDisplayOrder))
                .limit(3)
                .toList();

        model.addAttribute("product", selectedProduct);
        model.addAttribute("detailImages", detailImages);
        model.addAttribute("mainImageUrl", resolveMainImageUrl(detailImages));
        return "guest/product-detail";
    }

    @GetMapping("/guest/cart")
    public String cart(Model model) {
        List<ProductView> cartItems = productService.findAll().stream()
            .map(this::toProductView)
            .limit(2)
            .toList();
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalPrice", cartItems.stream().mapToInt(ProductView::price).sum());
        return "guest/cart";
    }

    @GetMapping("/guest/checkout")
    public String checkout(Model model) {
        int orderTotal = productService.findAll().stream()
            .map(this::toProductView)
            .limit(2)
            .mapToInt(ProductView::price)
            .sum();
        model.addAttribute("orderTotal", orderTotal);
        return "guest/checkout";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("ordersToday", 18);
        model.addAttribute("revenueToday", 122_500_000);
        model.addAttribute("lowStock", 7);
        model.addAttribute("recentOrders", List.of(
                new OrderView("OD-2401", "Nguyễn Minh Châu", "Đã xác nhận", 14_800_000),
                new OrderView("OD-2402", "Trần Khánh Linh", "Đang giao", 9_900_000),
                new OrderView("OD-2403", "Lê Hoàng Gia", "Chờ xác nhận", 5_400_000)
        ));
        return "admin/dashboard";
    }

    @GetMapping("/staff/dashboard")
    public String staffDashboard(Model model, HttpSession session) {
        Integer accountId = (Integer) session.getAttribute("accountId");
        String roleName = (String) session.getAttribute("roleName");
        addStaffSummary(model, accountId, roleName);
        return "staff/dashboard";
    }

    @GetMapping("/staff/products")
    public String staffProducts(Model model) {
        model.addAttribute("products", productService.findAll());
        return "staff/products";
    }

    @GetMapping("/staff/orders")
    public String staffOrders(Model model, HttpSession session) {
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("staffProducts", productService.findAll().stream()
            .map(this::toStaffProductView)
            .sorted(Comparator.comparing(StaffProductView::productName))
            .toList());
        model.addAttribute("lowStockThreshold", LOW_STOCK_THRESHOLD);
        model.addAttribute("staffOrderViews", buildStaffOrderViews(session));
        return "staff/orders";
    }

    @GetMapping("/staff/customers")
    public String staffCustomers(Model model) {
        model.addAttribute("customers", customerService.findAll());
        return "staff/customers";
    }

    @PostMapping("/staff/customers")
    public String createCustomer(@RequestParam String customerName,
                                 @RequestParam(required = false) String phone,
                                 @RequestParam(required = false) String address,
                                 RedirectAttributes redirectAttributes) {
        Customer customer = new Customer();
        customer.setCustomerName(customerName.trim());
        customer.setPhone(phone == null ? null : phone.trim());
        customer.setAddress(address == null ? null : address.trim());
        customerService.save(customer);
        redirectAttributes.addFlashAttribute("success", "Đã thêm khách hàng mới.");
        return "redirect:/staff/customers";
    }

    @PostMapping("/staff/orders")
    @Transactional
    public String createOrder(@RequestParam Integer customerId,
                              @RequestParam String cartPayload,
                              @RequestParam(defaultValue = "checkout") String orderAction,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Staff staff = resolveCurrentStaff(session);
        if (staff == null) {
            redirectAttributes.addFlashAttribute("error", "Phiên đăng nhập đã hết hạn hoặc tài khoản không hợp lệ.");
            return "redirect:/login";
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

    @PostMapping("/staff/orders/{orderId}/cancel")
    @Transactional
    public String cancelOrder(@PathVariable Integer orderId,
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

    private void addStaffSummary(Model model, Integer accountId, String roleName) {
        List<Order> allOrders = orderService.findAll().stream()
            .filter(order -> canManageOrder(order, accountId, roleName))
            .toList();
        LocalDate today = LocalDate.now();
        long todayOrders = allOrders.stream()
            .filter(order -> order.getOrderDate() != null)
            .filter(order -> order.getOrderDate().toLocalDate().isEqual(today))
            .count();

        BigDecimal revenueToday = allOrders.stream()
            .filter(order -> order.getOrderDate() != null)
            .filter(order -> order.getOrderDate().toLocalDate().isEqual(today))
            .map(Order::getFinalTotal)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("todayOrders", todayOrders);
        model.addAttribute("todayRevenue", revenueToday);
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

        return new StaffOrderView(
            order.getOrderId(),
            order.getOrderNumber(),
            customerName,
            status,
            order.getFinalTotal() == null ? BigDecimal.ZERO : order.getFinalTotal(),
            dateDisplay,
            canCancel,
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

    public record ProductView(Long id, String category, String productName, int price,
                              String description, String material, String weight, String imageUrl) {
    }

    public record OrderView(String code, String customer, String status, int amount) {
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

    private ProductView toProductView(Product product) {
        String categoryName = product.getCategory() != null ? product.getCategory().getCategoryName() : "Khác";
        String imageUrl = (product.getImageUrl() == null || product.getImageUrl().isBlank())
            ? "https://placehold.co/800x600?text=No+Image"
            : product.getImageUrl();

        String description = readAttribute(product, "Description", "Mo ta", "Mô tả")
            .orElse("Sản phẩm trang sức cao cấp.");
        String material = readAttribute(product, "Material", "Chat lieu", "Chất liệu")
            .orElse("Đang cập nhật");
        String weight = readAttribute(product, "Weight", "Trong luong", "Trọng lượng")
            .orElse("Đang cập nhật");

        return new ProductView(
            (long) product.getProductId(),
            categoryName,
            product.getProductName(),
            product.getBasePrice().intValue(),
            description,
            material,
            weight,
            imageUrl
        );
    }

    private Optional<String> readAttribute(Product product, String... names) {
        if (product.getAttributes() == null || product.getAttributes().isEmpty()) {
            return Optional.empty();
        }

        for (ProductAttribute attribute : product.getAttributes()) {
            if (attribute.getAttributeName() == null) {
                continue;
            }
            for (String name : names) {
                if (attribute.getAttributeName().equalsIgnoreCase(name) && attribute.getAttributeValue() != null) {
                    return Optional.of(attribute.getAttributeValue());
                }
            }
        }
        return Optional.empty();
    }

    private List<PageItem> buildPageItems(int currentPage, int totalPages) {
        List<PageItem> items = new ArrayList<>();
        if (totalPages <= 7) {
            for (int page = 1; page <= totalPages; page++) {
                items.add(PageItem.page(page, page == currentPage));
            }
            return items;
        }

        items.add(PageItem.page(1, currentPage == 1));

        if (currentPage > 3) {
            items.add(PageItem.ofEllipsis());
        }

        int start = Math.max(2, currentPage - 1);
        int end = Math.min(totalPages - 1, currentPage + 1);

        if (currentPage <= 3) {
            start = 2;
            end = 4;
        }
        if (currentPage >= totalPages - 2) {
            start = totalPages - 3;
            end = totalPages - 1;
        }

        for (int page = start; page <= end; page++) {
            items.add(PageItem.page(page, page == currentPage));
        }

        if (currentPage < totalPages - 2) {
            items.add(PageItem.ofEllipsis());
        }

        items.add(PageItem.page(totalPages, currentPage == totalPages));
        return items;
    }

    private String resolveMainImageUrl(List<ProductImage> images) {
        if (images == null || images.isEmpty()) {
            return "https://placehold.co/800x600?text=No+Image";
        }

        return images.stream()
            .filter(ProductImage::isPrimary)
            .findFirst()
            .or(() -> images.stream().findFirst())
            .map(ProductImage::getImageUrl)
            .filter(url -> url != null && !url.isBlank())
            .orElse("https://placehold.co/800x600?text=No+Image");
    }

    public record PageItem(Integer page, boolean active, boolean ellipsis) {
        public static PageItem page(int page, boolean active) {
            return new PageItem(page, active, false);
        }

        public static PageItem ofEllipsis() {
            return new PageItem(null, false, true);
        }
    }
}
