package com.example.Jewelry.controller;

import com.example.Jewelry.service.SupplierService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public String  listSuppliers(Model model) {
        model.addAttribute("suppliers", supplierService.findAll());
        model.addAttribute("activePage", "suppliers");
        return "admin/suppliers";
    }

    @GetMapping("/new")
    public String createSupplierForm(Model model) {
        model.addAttribute("isEdit", false);
        model.addAttribute("activePage", "suppliers");
        return "admin/supplier-form";
    }

    @PostMapping
    public String createSupplier(@RequestParam String supplierName,
                                 @RequestParam(required = false) String phone,
                                 @RequestParam(required = false) String address,
                                 RedirectAttributes redirectAttributes) {
        try {
            supplierService.createSupplier(supplierName, phone, address);
            redirectAttributes.addFlashAttribute("success", "Thêm nhà cung cấp thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi thêm nhà cung cấp: " + e.getMessage());
            return "redirect:/admin/suppliers/new";
        }
        return "redirect:/admin/suppliers";
    }

    @GetMapping("/{id}/edit")
    public String editSupplierForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        return supplierService.findById(id)
            .map(supplier -> {
                model.addAttribute("supplier", supplier);
                model.addAttribute("isEdit", true);
                model.addAttribute("activePage", "suppliers");
                return "admin/supplier-form";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy nhà cung cấp.");
                return "redirect:/admin/suppliers";
            });
    }

    @PostMapping("/{id}")
    public String updateSupplier(@PathVariable Integer id,
                                 @RequestParam String supplierName,
                                 @RequestParam(required = false) String phone,
                                 @RequestParam(required = false) String address,
                                 RedirectAttributes redirectAttributes) {
        try {
            supplierService.updateSupplier(id, supplierName, phone, address);
            redirectAttributes.addFlashAttribute("success", "Cập nhật nhà cung cấp thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi cập nhật nhà cung cấp: " + e.getMessage());
            return "redirect:/admin/suppliers/" + id + "/edit";
        }
        return "redirect:/admin/suppliers";
    }

    @PostMapping("/{id}/delete")
    public String deleteSupplier(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            supplierService.deleteSupplier(id);
            redirectAttributes.addFlashAttribute("success", "Xóa nhà cung cấp thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xóa nhà cung cấp: " + e.getMessage());
        }
        return "redirect:/admin/suppliers";
    }
}
