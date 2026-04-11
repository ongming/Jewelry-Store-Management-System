package com.example.Jewelry.controller;

import com.example.Jewelry.model.entity.Product;
import com.example.Jewelry.model.entity.ProductAttribute;
import com.example.Jewelry.service.CategoryService;
import com.example.Jewelry.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class UiController {

    private static final int PRODUCTS_PAGE_SIZE = 6;

    private final ProductService productService;
    private final CategoryService categoryService;

    public UiController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
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
        Optional<ProductView> product = productService.findById(id).map(this::toProductView);
        if (product.isEmpty()) {
            return "redirect:/guest/products";
        }
        model.addAttribute("product", product.get());
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

    public record ProductView(Long id, String category, String productName, int price,
                              String description, String material, String weight, String imageUrl) {
    }

    public record OrderView(String code, String customer, String status, int amount) {
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

    public record PageItem(Integer page, boolean active, boolean ellipsis) {
        public static PageItem page(int page, boolean active) {
            return new PageItem(page, active, false);
        }

        public static PageItem ofEllipsis() {
            return new PageItem(null, false, true);
        }
    }
}
