package com.example.Jewelry.controller;

import com.example.Jewelry.model.entity.Inventory;
import com.example.Jewelry.model.entity.Order;
import com.example.Jewelry.model.entity.OrderDetail;
import com.example.Jewelry.service.InventoryService;
import com.example.Jewelry.service.OrderDetailService;
import com.example.Jewelry.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
public class DashboardController {

    private final OrderService orderService;
    private final OrderDetailService orderDetailService;
    private final InventoryService inventoryService;

    public DashboardController(OrderService orderService,
                               OrderDetailService orderDetailService,
                               InventoryService inventoryService) {
        this.orderService = orderService;
        this.orderDetailService = orderDetailService;
        this.inventoryService = inventoryService;
    }

    @GetMapping("/dashboard")
    public String entry(HttpSession session) {
        String roleName = (String) session.getAttribute("roleName");
        if ("ADMIN".equalsIgnoreCase(roleName)) {
            return "redirect:/admin/dashboard";
        }
        if ("STAFF".equalsIgnoreCase(roleName)) {
            return "redirect:/staff/dashboard";
        }
        return "redirect:/home";
    }

    @GetMapping({"/dashboard/admin", "/admin/dashboard"})
    public String adminDashboard(Model model) {
        List<Order> allOrders = orderService.findAll();
        List<Order> validOrders = allOrders.stream()
            .filter(order -> order.getOrderDate() != null)
            .filter(order -> order.getFinalTotal() != null)
            .filter(order -> !isCancelled(order.getStatus()))
            .toList();

        List<OrderDetail> orderDetails = orderDetailService.findAll();
        List<Inventory> inventories = inventoryService.findAll();

        LocalDate today = LocalDate.now();
        long ordersToday = validOrders.stream()
            .filter(order -> order.getOrderDate().toLocalDate().isEqual(today))
            .count();
        BigDecimal revenueToday = validOrders.stream()
            .filter(order -> order.getOrderDate().toLocalDate().isEqual(today))
            .map(Order::getFinalTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long lowStock = inventories.stream()
            .filter(inventory -> inventory != null)
            .filter(inventory -> inventory.getQuantityStock() <= 12)
            .count();

        Map<String, Object> dashboardData = new LinkedHashMap<>();
        dashboardData.put("orders", toOrderRecords(validOrders));
        dashboardData.put("orderDetails", toOrderDetailRecords(orderDetails));
        dashboardData.put("inventory", toInventoryRecords(inventories));

        model.addAttribute("ordersToday", ordersToday);
        model.addAttribute("revenueToday", revenueToday);
        model.addAttribute("lowStock", lowStock);
        model.addAttribute("dashboardData", dashboardData);
        return "admin/dashboard";
    }

    @GetMapping({"/dashboard/staff", "/staff/dashboard"})
    public String staffDashboard(Model model, HttpSession session) {
        Integer accountId = (Integer) session.getAttribute("accountId");
        String roleName = (String) session.getAttribute("roleName");
        addStaffSummary(model, accountId, roleName);
        return "staff/dashboard";
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
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("todayOrders", todayOrders);
        model.addAttribute("todayRevenue", revenueToday);
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

    private boolean isCancelled(String status) {
        return status != null && "CANCELLED".equalsIgnoreCase(status.trim());
    }

    private List<Map<String, Object>> toOrderRecords(List<Order> orders) {
        List<Map<String, Object>> records = new ArrayList<>();
        for (Order order : orders) {
            if (order == null || order.getOrderDate() == null || order.getFinalTotal() == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("orderId", order.getOrderId());
            item.put("orderDate", order.getOrderDate().toString());
            item.put("status", order.getStatus());
            item.put("finalTotal", order.getFinalTotal());
            records.add(item);
        }
        return records;
    }

    private List<Map<String, Object>> toOrderDetailRecords(List<OrderDetail> details) {
        List<Map<String, Object>> records = new ArrayList<>();
        for (OrderDetail detail : details) {
            if (detail == null || detail.getOrder() == null || detail.getOrder().getOrderDate() == null) {
                continue;
            }
            BigDecimal unitPrice = detail.getUnitPrice() == null ? BigDecimal.ZERO : detail.getUnitPrice();
            BigDecimal revenue = unitPrice.multiply(BigDecimal.valueOf(detail.getQuantity()));

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("orderDate", detail.getOrder().getOrderDate().toString());
            item.put("orderStatus", detail.getOrder().getStatus());
            item.put("productName", detail.getProduct() == null ? "Unknown" : detail.getProduct().getProductName());
            item.put("quantity", detail.getQuantity());
            item.put("unitPrice", unitPrice);
            item.put("revenue", revenue);
            records.add(item);
        }
        return records;
    }

    private List<Map<String, Object>> toInventoryRecords(List<Inventory> inventories) {
        List<Map<String, Object>> records = new ArrayList<>();
        for (Inventory inventory : inventories) {
            if (inventory == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productName", inventory.getProduct() == null ? "Unknown" : inventory.getProduct().getProductName());
            item.put("stock", inventory.getQuantityStock());
            records.add(item);
        }
        return records;
    }
}
