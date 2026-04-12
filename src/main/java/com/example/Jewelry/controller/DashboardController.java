package com.example.Jewelry.controller;

import com.example.Jewelry.model.entity.Order;
import com.example.Jewelry.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Controller
public class DashboardController {

    private final OrderService orderService;

    public DashboardController(OrderService orderService) {
        this.orderService = orderService;
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

    public record OrderView(String code, String customer, String status, int amount) {
    }
}
