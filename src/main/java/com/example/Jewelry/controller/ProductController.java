package com.example.Jewelry.controller;

import com.example.Jewelry.model.entity.Product;
import com.example.Jewelry.model.entity.ProductAttribute;
import com.example.Jewelry.model.entity.ProductImage;
import com.example.Jewelry.service.CategoryService;
import com.example.Jewelry.service.InventoryService;
import com.example.Jewelry.service.ProductService;
import com.example.Jewelry.service.SupplierService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class ProductController {

    private static final int PRODUCTS_PAGE_SIZE = 6;

    private final ProductService productService;
    private final CategoryService categoryService;
    private final InventoryService inventoryService;
    private final SupplierService supplierService;

    public ProductController(ProductService productService,
                             CategoryService categoryService,
                             InventoryService inventoryService,
                             SupplierService supplierService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.inventoryService = inventoryService;
        this.supplierService = supplierService;
    }

    @GetMapping({"/", "/home", "/guest/home"})
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

    @GetMapping({"/products", "/guest/products"})
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
            .toList());
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

    @GetMapping({"/products/{id}", "/guest/product-detail/{id}"})
    public String productDetail(@PathVariable Integer id, Model model) {
        Optional<Product> product = productService.findById(id);
        if (product.isEmpty()) {
            return "redirect:/products";
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

    @GetMapping({"/cart", "/guest/cart"})
    public String cart(Model model) {
        List<ProductView> cartItems = productService.findAll().stream()
            .map(this::toProductView)
            .limit(2)
            .toList();
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalPrice", cartItems.stream().mapToInt(ProductView::price).sum());
        return "guest/cart";
    }

    @GetMapping({"/checkout", "/guest/checkout"})
    public String checkout(Model model) {
        int orderTotal = productService.findAll().stream()
            .map(this::toProductView)
            .limit(2)
            .mapToInt(ProductView::price)
            .sum();
        model.addAttribute("orderTotal", orderTotal);
        return "guest/checkout";
    }

    @GetMapping({"/products/staff", "/staff/products"})
    public String staffProducts(Model model) {
        model.addAttribute("products", productService.findAll());
        return "staff/products";
    }

    @GetMapping({"/products/manage", "/admin/products"})
    public String productManagement(Model model) {
        model.addAttribute("products", productService.findAll());
        return "admin/products";
    }

    @GetMapping({"/products/manage/new", "/admin/products/new"})
    public String productCreateForm(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isEdit", false);
        return "admin/product-form";
    }

    @PostMapping({"/products/manage", "/admin/products"})
    public String createProduct(@RequestParam String productCode,
                                @RequestParam String productName,
                                @RequestParam BigDecimal basePrice,
                                @RequestParam(required = false) String imageUrl,
                                @RequestParam(required = false) MultipartFile imageFile,
                                @RequestParam Integer categoryId,
                                RedirectAttributes redirectAttributes) {
        try {
            String finalImageUrl = resolveImageUrl(imageUrl, imageFile);
            productService.createProduct(productCode, productName, basePrice, finalImageUrl, categoryId);
            redirectAttributes.addFlashAttribute("success", "Thêm sản phẩm thành công.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
            return "redirect:/admin/products/new";
        } catch (IOException ioException) {
            redirectAttributes.addFlashAttribute("error", "Không thể tải ảnh lên: " + ioException.getMessage());
            return "redirect:/admin/products/new";
        }
        return "redirect:/admin/products";
    }

    @GetMapping({"/products/manage/{id}/edit", "/admin/products/{id}/edit"})
    public String productEditForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        return productService.findById(id)
            .map(product -> {
                model.addAttribute("product", product);
                model.addAttribute("categories", categoryService.findAll());
                model.addAttribute("isEdit", true);
                return "admin/product-form";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm.");
                return "redirect:/admin/products";
            });
    }

    @PostMapping({"/products/manage/{id}", "/admin/products/{id}"})
    public String updateProduct(@PathVariable Integer id,
                                @RequestParam String productCode,
                                @RequestParam String productName,
                                @RequestParam BigDecimal basePrice,
                                @RequestParam(required = false) String imageUrl,
                                @RequestParam(required = false) MultipartFile imageFile,
                                @RequestParam Integer categoryId,
                                RedirectAttributes redirectAttributes) {
        try {
            String finalImageUrl = resolveImageUrl(imageUrl, imageFile);
            if (finalImageUrl == null || finalImageUrl.isBlank()) {
                finalImageUrl = productService.findById(id)
                    .map(product -> product.getImageUrl())
                    .orElse(null);
            }
            productService.updateProduct(id, productCode, productName, basePrice, finalImageUrl, categoryId);
            redirectAttributes.addFlashAttribute("success", "Cập nhật sản phẩm thành công.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
            return "redirect:/admin/products/" + id + "/edit";
        } catch (IOException ioException) {
            redirectAttributes.addFlashAttribute("error", "Không thể tải ảnh lên: " + ioException.getMessage());
            return "redirect:/admin/products/" + id + "/edit";
        }
        return "redirect:/admin/products";
    }

    @PostMapping({"/products/manage/{id}/delete", "/admin/products/{id}/delete"})
    public String deleteProduct(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("success", "Xóa sản phẩm thành công.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping({"/products/categories", "/admin/categories"})
    public String categoryList(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "admin/categories";
    }

    @GetMapping({"/products/categories/new", "/admin/categories/new"})
    public String categoryCreateForm(Model model) {
        model.addAttribute("isEdit", false);
        return "admin/category-form";
    }

    @PostMapping({"/products/categories", "/admin/categories"})
    public String createCategory(@RequestParam String categoryName, RedirectAttributes redirectAttributes) {
        try {
            categoryService.createCategory(categoryName);
            redirectAttributes.addFlashAttribute("success", "Thêm danh mục thành công.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
            return "redirect:/admin/categories/new";
        }
        return "redirect:/admin/categories";
    }

    @GetMapping({"/products/categories/{id}/edit", "/admin/categories/{id}/edit"})
    public String categoryEditForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        return categoryService.findById(id)
            .map(category -> {
                model.addAttribute("category", category);
                model.addAttribute("isEdit", true);
                return "admin/category-form";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục.");
                return "redirect:/admin/categories";
            });
    }

    @PostMapping({"/products/categories/{id}", "/admin/categories/{id}"})
    public String updateCategory(@PathVariable Integer id,
                                 @RequestParam String categoryName,
                                 RedirectAttributes redirectAttributes) {
        try {
            categoryService.updateCategory(id, categoryName);
            redirectAttributes.addFlashAttribute("success", "Cập nhật danh mục thành công.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
            return "redirect:/admin/categories/" + id + "/edit";
        }
        return "redirect:/admin/categories";
    }

    @PostMapping({"/products/categories/{id}/delete", "/admin/categories/{id}/delete"})
    public String deleteCategory(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Xóa danh mục thành công.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
        }
        return "redirect:/admin/categories";
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
        int displayPrice = product.getBasePrice() == null ? 0 : product.getBasePrice().intValue();

        return new ProductView(
            (long) product.getProductId(),
            categoryName,
            product.getProductName(),
            displayPrice,
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

    private String resolveImageUrl(String imageUrl, MultipartFile imageFile) throws IOException {
        if (imageFile != null && !imageFile.isEmpty()) {
            String originalName = imageFile.getOriginalFilename();
            String extension = extractExtension(originalName);
            String fileName = UUID.randomUUID() + extension;

            Path uploadDir = Paths.get("uploads", "products");
            Files.createDirectories(uploadDir);
            Files.copy(imageFile.getInputStream(), uploadDir.resolve(fileName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/products/" + fileName;
        }

        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        return imageUrl.trim();
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.contains(".")) {
            return ".jpg";
        }
        String extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
        if (extension.length() > 10) {
            return ".jpg";
        }
        return extension;
    }

    public record ProductView(Long id, String category, String productName, int price,
                              String description, String material, String weight, String imageUrl) {
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
