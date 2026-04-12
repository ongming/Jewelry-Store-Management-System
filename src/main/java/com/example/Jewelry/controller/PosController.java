package com.example.Jewelry.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PosController {

    @GetMapping("/pos")
    public String posEntry() {
        return "redirect:/staff/orders";
    }
}
