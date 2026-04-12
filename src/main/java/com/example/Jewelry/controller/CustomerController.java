package com.example.Jewelry.controller;

import com.example.Jewelry.model.entity.Customer;
import com.example.Jewelry.model.entity.Order;
import com.example.Jewelry.model.entity.Product;
import com.example.Jewelry.service.CustomerService;
import com.example.Jewelry.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping({"/customers", "/staff/customers"})
public class CustomerController {

    private final CustomerService customerService;
    private final OrderService orderService;

    public CustomerController(CustomerService customerService,
                              OrderService orderService) {
        this.customerService = customerService;
        this.orderService = orderService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer customerId,
                       @RequestParam(required = false) Integer orderId,
                       @RequestParam(required = false) Integer editId,
                       Model model) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<Customer> customers = customerService.search(normalizedKeyword);
        List<Order> allOrders = orderService.findAll();

        Map<Integer, List<Order>> ordersByCustomer = allOrders.stream()
            .filter(order -> order.getCustomer() != null)
            .collect(Collectors.groupingBy(order -> order.getCustomer().getCustomerId()));

        ordersByCustomer.values().forEach(orders ->
            orders.sort(Comparator.comparing(Order::getOrderDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
        );

        List<CustomerListRowView> customerRows = customers.stream()
            .map(customer -> toCustomerListRowView(customer, ordersByCustomer.getOrDefault(customer.getCustomerId(), List.of())))
            .toList();

        Customer selectedCustomer = null;
        if (!customers.isEmpty()) {
            if (customerId == null) {
                selectedCustomer = customers.get(0);
            } else {
                selectedCustomer = customers.stream()
                    .filter(customer -> customer.getCustomerId() == customerId)
                    .findFirst()
                    .orElse(null);
            }
        }

        List<Order> selectedCustomerOrders = selectedCustomer == null
            ? List.of()
            : ordersByCustomer.getOrDefault(selectedCustomer.getCustomerId(), List.of());

        List<CustomerOrderSummaryView> orderRows = selectedCustomerOrders.stream()
            .map(this::toCustomerOrderSummaryView)
            .toList();

        Order selectedOrder = null;
        if (!selectedCustomerOrders.isEmpty()) {
            if (orderId == null) {
                selectedOrder = selectedCustomerOrders.get(0);
            } else {
                selectedOrder = selectedCustomerOrders.stream()
                    .filter(order -> order.getOrderId() == orderId)
                    .findFirst()
                    .orElse(selectedCustomerOrders.get(0));
            }
        }

        List<CustomerOrderDetailView> selectedOrderItems = selectedOrder == null
            ? List.of()
            : toCustomerOrderDetailViews(selectedOrder);

        BigDecimal selectedOrderTotal = selectedOrder == null
            ? BigDecimal.ZERO
            : calculateOrderTotalFromDetails(selectedOrder);

        Customer editCustomer = null;
        if (editId != null) {
            editCustomer = customerService.findById(editId).orElse(null);
        }

        BigDecimal totalRevenue = allOrders.stream()
            .map(this::calculateOrderTotalFromDetails)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("keyword", normalizedKeyword);
        model.addAttribute("customerRows", customerRows);
        model.addAttribute("selectedCustomer", selectedCustomer);
        model.addAttribute("selectedCustomerId", selectedCustomer == null ? null : selectedCustomer.getCustomerId());
        model.addAttribute("orderRows", orderRows);
        model.addAttribute("selectedOrder", selectedOrder == null ? null : toCustomerOrderSummaryView(selectedOrder));
        model.addAttribute("selectedOrderItems", selectedOrderItems);
        model.addAttribute("selectedOrderTotal", selectedOrderTotal);
        model.addAttribute("editCustomer", editCustomer);
        model.addAttribute("totalCustomers", customerRows.size());
        model.addAttribute("totalOrders", allOrders.size());
        model.addAttribute("totalRevenue", totalRevenue);
        return "staff/customers";
    }

    @PostMapping
    public String create(@RequestParam String customerName,
                         @RequestParam(required = false) String phone,
                         @RequestParam(required = false) String address,
                         RedirectAttributes redirectAttributes) {
        String normalizedName = normalizeRequired(customerName, 150);
        if (normalizedName == null) {
            redirectAttributes.addFlashAttribute("error", "Tên khách hàng không được để trống và tối đa 150 ký tự.");
            return "redirect:/staff/customers";
        }

        Customer customer = new Customer();
        customer.setCustomerName(normalizedName);
        customer.setPhone(normalizeOptional(phone, 20));
        customer.setAddress(normalizeOptional(address, 255));
        Customer saved = customerService.save(customer);
        redirectAttributes.addFlashAttribute("success", "Đã thêm khách hàng mới.");
        return "redirect:/staff/customers?customerId=" + saved.getCustomerId();
    }

    @PostMapping("/{customerId}/update")
    public String update(@PathVariable Integer customerId,
                         @RequestParam String customerName,
                         @RequestParam(required = false) String phone,
                         @RequestParam(required = false) String address,
                         RedirectAttributes redirectAttributes) {
        Customer existing = customerService.findById(customerId).orElse(null);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy khách hàng cần cập nhật.");
            return "redirect:/staff/customers";
        }

        String normalizedName = normalizeRequired(customerName, 150);
        if (normalizedName == null) {
            redirectAttributes.addFlashAttribute("error", "Tên khách hàng không được để trống và tối đa 150 ký tự.");
            return "redirect:/staff/customers?customerId=" + customerId + "&editId=" + customerId;
        }

        existing.setCustomerName(normalizedName);
        existing.setPhone(normalizeOptional(phone, 20));
        existing.setAddress(normalizeOptional(address, 255));
        customerService.save(existing);

        redirectAttributes.addFlashAttribute("success", "Đã cập nhật thông tin khách hàng.");
        return "redirect:/staff/customers?customerId=" + customerId;
    }

    @PostMapping("/{customerId}/delete")
    public String delete(@PathVariable Integer customerId,
                         RedirectAttributes redirectAttributes) {
        if (!customerService.existsById(customerId)) {
            redirectAttributes.addFlashAttribute("error", "Khách hàng không tồn tại.");
            return "redirect:/staff/customers";
        }

        if (orderService.existsByCustomerId(customerId)) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa khách hàng đã có đơn hàng.");
            return "redirect:/staff/customers?customerId=" + customerId;
        }

        customerService.deleteById(customerId);
        redirectAttributes.addFlashAttribute("success", "Đã xóa khách hàng.");
        return "redirect:/staff/customers";
    }

    private CustomerListRowView toCustomerListRowView(Customer customer, List<Order> orders) {
        BigDecimal totalSpent = orders.stream()
            .map(this::calculateOrderTotalFromDetails)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        String latestOrderDate = orders.isEmpty() || orders.get(0).getOrderDate() == null
            ? "--"
            : orders.get(0).getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        return new CustomerListRowView(
            customer.getCustomerId(),
            customer.getCustomerName(),
            customer.getPhone(),
            customer.getAddress(),
            orders.size(),
            totalSpent,
            latestOrderDate
        );
    }

    private CustomerOrderSummaryView toCustomerOrderSummaryView(Order order) {
        String orderDate = order.getOrderDate() == null
            ? "--"
            : order.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        String normalizedStatus = order.getStatus() == null
            ? "PENDING"
            : order.getStatus().trim().toUpperCase(Locale.ROOT);

        String statusLabel = switch (normalizedStatus) {
            case "PAID" -> "Đã thanh toán";
            case "CANCELLED" -> "Đã hủy";
            default -> "Chờ thanh toán";
        };

        return new CustomerOrderSummaryView(
            order.getOrderId(),
            order.getOrderNumber(),
            orderDate,
            normalizedStatus,
            statusLabel,
            calculateOrderTotalFromDetails(order)
        );
    }

    private List<CustomerOrderDetailView> toCustomerOrderDetailViews(Order order) {
        if (order.getOrderDetails() == null) {
            return List.of();
        }

        return order.getOrderDetails().stream()
            .map(detail -> {
                Product product = detail.getProduct();
                BigDecimal unitPrice = detail.getUnitPrice() == null ? BigDecimal.ZERO : detail.getUnitPrice();
                int quantity = Math.max(0, detail.getQuantity());
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

                return new CustomerOrderDetailView(
                    product == null ? "--" : product.getProductCode(),
                    product == null ? "Sản phẩm" : product.getProductName(),
                    product == null || product.getCategory() == null ? "Khác" : product.getCategory().getCategoryName(),
                    quantity,
                    unitPrice,
                    lineTotal
                );
            })
            .toList();
    }

    private BigDecimal calculateOrderTotalFromDetails(Order order) {
        if (order == null || order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            return order == null || order.getFinalTotal() == null ? BigDecimal.ZERO : order.getFinalTotal();
        }

        return order.getOrderDetails().stream()
            .map(detail -> {
                BigDecimal unitPrice = detail.getUnitPrice() == null ? BigDecimal.ZERO : detail.getUnitPrice();
                int quantity = Math.max(0, detail.getQuantity());
                return unitPrice.multiply(BigDecimal.valueOf(quantity));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizeOptional(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            return normalized.substring(0, maxLength);
        }
        return normalized;
    }

    private String normalizeRequired(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            return null;
        }
        return normalized;
    }

    public record CustomerListRowView(Integer customerId,
                                      String customerName,
                                      String phone,
                                      String address,
                                      int orderCount,
                                      BigDecimal totalSpent,
                                      String latestOrderDate) {
    }

    public record CustomerOrderSummaryView(Integer orderId,
                                           String orderNumber,
                                           String orderDate,
                                           String status,
                                           String statusLabel,
                                           BigDecimal orderTotal) {
    }

    public record CustomerOrderDetailView(String productCode,
                                          String productName,
                                          String categoryName,
                                          int quantity,
                                          BigDecimal unitPrice,
                                          BigDecimal lineTotal) {
    }
}
