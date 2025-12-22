package com.bsmart.scoretracker.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to handle root path redirection
 */
@Controller
public class RootController {

    /**
     * Redirect root path to admin dashboard
     */
    @GetMapping("/")
    public String redirectToDashboard() {
        return "redirect:/admin/dashboard";
    }
}
