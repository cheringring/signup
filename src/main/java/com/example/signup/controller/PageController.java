package com.example.signup.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/company-info")
    public String companyInfo() {
        return "company_info";
    }

    @GetMapping("/interview")
    public String interview() {
        return "interview";
    }

    @GetMapping("/community")
    public String community() {
        return "community";
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }
}
