package com.livraison.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CarteSuiviController {

    @GetMapping("/suivi")
    public String suivi() {
        return "carte/suivi";
    }
}