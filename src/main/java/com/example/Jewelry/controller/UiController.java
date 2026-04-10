package com.example.Jewelry.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class UiController {

    private static final List<ProductView> PRODUCTS = List.of(
            new ProductView(1L, "Nhẫn", "Nhẫn Bloom Vĩnh Cửu", 12500000,
                    "Viên đá chủ đạo nổi bật với thiết kế quầng hoa thanh lịch.", "Vàng trắng 18K", "4.5g",
                    "https://images.unsplash.com/photo-1605100804763-247f67b3557e?auto=format&fit=crop&w=1200&q=80"),
            new ProductView(2L, "Dây chuyền", "Dây chuyền Ngọc Noir", 9800000,
                    "Ngọc trai nước ngọt tinh tế, phù hợp phong cách hiện đại.", "Ngọc trai và vàng 14K", "8.1g",
                    "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?auto=format&fit=crop&w=1200&q=80"),
            new ProductView(3L, "Lắc tay", "Lắc tay Maison", 7600000,
                    "Lắc tay vàng dáng khối, bề mặt bóng tạo vẻ sang trọng.", "Vàng vermeil 18K", "13.0g",
                    "https://images.unsplash.com/photo-1617038220319-276d3cfab638?auto=format&fit=crop&w=1200&q=80"),
            new ProductView(4L, "Bông tai", "Bông tai Luna", 5400000,
                    "Dáng thả mềm mại lấy cảm hứng từ thời trang cao cấp.", "Vàng hồng 14K", "3.2g",
                    "https://images.unsplash.com/photo-1635767798638-3e25273a8236?auto=format&fit=crop&w=1200&q=80"),
            new ProductView(5L, "Nhẫn", "Nhẫn Halo Imperial", 15400000,
                    "Kiểu dáng cổ điển, viên đá nhỏ được đính chính xác.", "Bạch kim", "5.8g",
                    "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?auto=format&fit=crop&w=1200&q=80")
    );

    @GetMapping({"/", "/guest/home"})
    public String home(Model model) {
        model.addAttribute("featuredProducts", PRODUCTS.stream().limit(4).toList());
        model.addAttribute("categories", List.of("Nhẫn", "Dây chuyền", "Lắc tay", "Bông tai"));
        return "guest/home";
    }

    @GetMapping("/guest/products")
    public String products(@RequestParam(required = false) String category,
                           @RequestParam(required = false) Integer minPrice,
                           @RequestParam(required = false) Integer maxPrice,
                           Model model) {
        List<ProductView> filtered = new ArrayList<>(PRODUCTS);
        if (category != null && !category.isBlank()) {
            filtered.removeIf(product -> !product.category().equalsIgnoreCase(category));
        }
        if (minPrice != null) {
            filtered.removeIf(product -> product.price() < minPrice);
        }
        if (maxPrice != null) {
            filtered.removeIf(product -> product.price() > maxPrice);
        }

        model.addAttribute("products", filtered);
        model.addAttribute("selectedCategory", category == null ? "" : category);
        model.addAttribute("categories", List.of("Nhẫn", "Dây chuyền", "Lắc tay", "Bông tai"));
        return "guest/products";
    }

    @GetMapping("/guest/product-detail/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        Optional<ProductView> product = PRODUCTS.stream().filter(item -> item.id().equals(id)).findFirst();
        if (product.isEmpty()) {
            return "redirect:/guest/products";
        }
        model.addAttribute("product", product.get());
        return "guest/product-detail";
    }

    @GetMapping("/guest/cart")
    public String cart(Model model) {
        model.addAttribute("cartItems", PRODUCTS.stream().limit(2).toList());
        model.addAttribute("totalPrice", PRODUCTS.stream().limit(2).mapToInt(ProductView::price).sum());
        return "guest/cart";
    }

    @GetMapping("/guest/checkout")
    public String checkout(Model model) {
        model.addAttribute("orderTotal", PRODUCTS.stream().limit(2).mapToInt(ProductView::price).sum());
        return "guest/checkout";
    }

    @GetMapping("/admin/login")
    public String adminLogin(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("errorMessage", error != null ? "Tên đăng nhập hoặc mật khẩu không đúng." : "");
        return "admin/login";
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

    public record ProductView(Long id, String category, String productName, int price,
                              String description, String material, String weight, String imageUrl) {
    }

    public record OrderView(String code, String customer, String status, int amount) {
    }
}
