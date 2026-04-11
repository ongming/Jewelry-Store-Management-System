package com.example.Jewelry.controller;

import com.example.Jewelry.service.CategoryService;
import com.example.Jewelry.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminCatalogController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public AdminCatalogController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("/products")
    public String productList(Model model) {
        model.addAttribute("products", productService.findAll());
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String productCreateForm(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isEdit", false);
        return "admin/product-form";
    }

    @PostMapping("/products")
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
            redirectAttributes.addFlashAttribute("success", "Them san pham thanh cong.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
            return "redirect:/admin/products/new";
        } catch (IOException ioException) {
            redirectAttributes.addFlashAttribute("error", "Khong the tai anh len: " + ioException.getMessage());
            return "redirect:/admin/products/new";
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/products/{id}/edit")
    public String productEditForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        return productService.findById(id)
            .map(product -> {
                model.addAttribute("product", product);
                model.addAttribute("categories", categoryService.findAll());
                model.addAttribute("isEdit", true);
                return "admin/product-form";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("error", "Khong tim thay san pham.");
                return "redirect:/admin/products";
            });
    }

    @PostMapping("/products/{id}")
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
            redirectAttributes.addFlashAttribute("success", "Cap nhat san pham thanh cong.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
            return "redirect:/admin/products/" + id + "/edit";
        } catch (IOException ioException) {
            redirectAttributes.addFlashAttribute("error", "Khong the tai anh len: " + ioException.getMessage());
            return "redirect:/admin/products/" + id + "/edit";
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("success", "Xoa san pham thanh cong.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/categories")
    public String categoryList(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "admin/categories";
    }

    @GetMapping("/categories/new")
    public String categoryCreateForm(Model model) {
        model.addAttribute("isEdit", false);
        return "admin/category-form";
    }

    @PostMapping("/categories")
    public String createCategory(@RequestParam String categoryName, RedirectAttributes redirectAttributes) {
        try {
            categoryService.createCategory(categoryName);
            redirectAttributes.addFlashAttribute("success", "Them danh muc thanh cong.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
            return "redirect:/admin/categories/new";
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/{id}/edit")
    public String categoryEditForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        return categoryService.findById(id)
            .map(category -> {
                model.addAttribute("category", category);
                model.addAttribute("isEdit", true);
                return "admin/category-form";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("error", "Khong tim thay danh muc.");
                return "redirect:/admin/categories";
            });
    }

    @PostMapping("/categories/{id}")
    public String updateCategory(@PathVariable Integer id,
                                 @RequestParam String categoryName,
                                 RedirectAttributes redirectAttributes) {
        try {
            categoryService.updateCategory(id, categoryName);
            redirectAttributes.addFlashAttribute("success", "Cap nhat danh muc thanh cong.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
            return "redirect:/admin/categories/" + id + "/edit";
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Xoa danh muc thanh cong.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
        }
        return "redirect:/admin/categories";
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
        String extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        if (extension.length() > 10) {
            return ".jpg";
        }
        return extension;
    }
}