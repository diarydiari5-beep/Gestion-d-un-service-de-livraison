package com.livraison.controller;

import com.livraison.entity.Colis;
import com.livraison.service.ColisService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/colis")
public class ColisController {

    private final ColisService colisService;

    public ColisController(ColisService colisService) {
        this.colisService = colisService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("colis", colisService.findAll());
        return "colis/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        prepareForm(model, new Colis());
        return "colis/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Colis colis, Model model,
                       RedirectAttributes redirectAttributes) {
        colis.setDescription(colis.getDescription() == null ? null : colis.getDescription().trim());
        if (colis.getDescription() == null || colis.getDescription().isEmpty()
                || colis.getCommande() == null) {
            model.addAttribute("errorMessage", "La description et la commande sont obligatoires.");
            prepareForm(model, colis);
            return "colis/form";
        }
        if ((colis.getPoids() != null && colis.getPoids().signum() < 0)
                || (colis.getVolume() != null && colis.getVolume().signum() < 0)) {
            model.addAttribute("errorMessage", "Le poids et le volume ne peuvent pas etre negatifs.");
            prepareForm(model, colis);
            return "colis/form";
        }
        colisService.save(colis);
        redirectAttributes.addFlashAttribute("successMessage", "Colis enregistre avec succes.");
        return "redirect:/colis";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model,
                           RedirectAttributes redirectAttributes) {
        return colisService.findById(id).map(colis -> {
            prepareForm(model, colis);
            return "colis/form";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("errorMessage", "Colis introuvable.");
            return "redirect:/colis";
        });
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (colisService.deleteById(id)) {
            redirectAttributes.addFlashAttribute("successMessage", "Colis supprime avec succes.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Colis introuvable.");
        }
        return "redirect:/colis";
    }

    private void prepareForm(Model model, Colis colis) {
        model.addAttribute("colis", colis);
        model.addAttribute("commandes", colisService.findAllCommandes());
    }
}