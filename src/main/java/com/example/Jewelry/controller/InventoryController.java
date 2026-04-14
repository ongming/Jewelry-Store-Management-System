package com.example.Jewelry.controller;

import com.example.Jewelry.service.InventoryService;
import com.example.Jewelry.service.LowStockAlertService;
import com.example.Jewelry.service.ProductService;
import com.example.Jewelry.service.SupplierService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class InventoryController {

    private final InventoryService inventoryService;
    private final ProductService productService;
    private final SupplierService supplierService;
    private final LowStockAlertService lowStockAlertService;

    public InventoryController(InventoryService inventoryService,
                               ProductService productService,
                               SupplierService supplierService,
                               LowStockAlertService lowStockAlertService) {
        this.inventoryService = inventoryService;
        this.productService = productService;
        this.supplierService = supplierService;
        this.lowStockAlertService = lowStockAlertService;
    }

    @GetMapping("/admin/inventory")
    public String adminInventory(Model model) {
        try {
            model.addAttribute("products", productService.findAll());
        } catch (Exception e) {
            model.addAttribute("products", List.of());
            model.addAttribute("error", "Lỗi khi tải danh sách sản phẩm: " + e.getMessage());
        }
        // [OBSERVER] Truyền cảnh báo tồn kho thấp từ listener ra UI
        model.addAttribute("lowStockAlerts", lowStockAlertService.getAlerts());
        model.addAttribute("lowStockThreshold", 5);
        return "admin/inventory";
    }

    @GetMapping("/staff/inventory")
    public String staffInventory(Model model) {
        try {
            model.addAttribute("products", productService.findAll());
            model.addAttribute("suppliers", supplierService.findAll());
        } catch (Exception e) {
            model.addAttribute("products", List.of());
            model.addAttribute("suppliers", List.of());
            model.addAttribute("error", "Lỗi khi tải dữ liệu: " + e.getMessage());
        }
        // [OBSERVER] Truyền cảnh báo tồn kho thấp từ listener ra UI
        model.addAttribute("lowStockAlerts", lowStockAlertService.getAlerts());
        model.addAttribute("lowStockThreshold", 5);
        return "staff/inventory";
    }

    @GetMapping("/admin/inventory/import/new")
    public String adminImportForm(Model model) {
        try {
            model.addAttribute("products", productService.findAll());
            model.addAttribute("suppliers", supplierService.findAll());
        } catch (Exception e) {
            model.addAttribute("products", List.of());
            model.addAttribute("suppliers", List.of());
            model.addAttribute("error", "Lỗi khi tải dữ liệu: " + e.getMessage());
        }
        return "admin/import-form";
    }

    @GetMapping({"/staff/inventory/import-form", "/staff/inventory/import/new"})
    public String staffImportForm(Model model) {
        try {
            model.addAttribute("products", productService.findAll());
            model.addAttribute("suppliers", supplierService.findAll());
        } catch (Exception e) {
            model.addAttribute("products", List.of());
            model.addAttribute("suppliers", List.of());
            model.addAttribute("error", "Lỗi khi tải dữ liệu: " + e.getMessage());
        }
        return "staff/import-form";
    }

    @PostMapping("/admin/inventory/import-batch")
    public String adminImportBatch(@RequestParam Integer supplierId,
                                   @RequestParam List<Integer> productIds,
                                   @RequestParam List<Integer> quantities,
                                   @RequestParam List<BigDecimal> importPrices,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        try {
            Integer accountId = (Integer) session.getAttribute("accountId");
            if (accountId == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập lại.");
                return "redirect:/admin/inventory";
            }

            inventoryService.importStock(supplierId, accountId, productIds, quantities, importPrices);
            redirectAttributes.addFlashAttribute("success", "Nhập kho thành công.");
            return "redirect:/admin/inventory";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi nhập kho: " + e.getMessage());
            return "redirect:/admin/inventory/import/new";
        }
    }

    @PostMapping("/staff/inventory/import-batch")
    public String staffImportBatch(@RequestParam Integer supplierId,
                                   @RequestParam List<Integer> productIds,
                                   @RequestParam List<Integer> quantities,
                                   @RequestParam List<BigDecimal> importPrices,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        try {
            Integer accountId = (Integer) session.getAttribute("accountId");
            if (accountId == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập lại.");
                return "redirect:/staff/inventory";
            }

            inventoryService.importStock(supplierId, accountId, productIds, quantities, importPrices);
            redirectAttributes.addFlashAttribute("success", "Nhập kho thành công.");
            return "redirect:/staff/inventory";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi nhập kho: " + e.getMessage());
            return "redirect:/staff/inventory/import-form";
        }
    }

    @PostMapping("/staff/inventory/import")
    public String staffImportSingle(@RequestParam Integer productId,
                                   @RequestParam Integer quantity,
                                   @RequestParam BigDecimal importPrice,
                                   @RequestParam Integer supplierId,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        try {
            Integer accountId = (Integer) session.getAttribute("accountId");
            if (accountId == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập lại.");
                return "redirect:/staff/inventory";
            }

            inventoryService.addStock(productId, quantity, supplierId, importPrice, accountId);
            redirectAttributes.addFlashAttribute("success", "Nhập kho thành công.");
            return "redirect:/staff/inventory";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi nhập kho: " + e.getMessage());
            return "redirect:/staff/inventory";
        }
    }
}
