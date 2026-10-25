package com.livraison.controller;

import com.livraison.entity.Livreur;
import com.livraison.entity.embeddable.Vehicule;
import com.livraison.service.LivreurService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/livreurs")
public class LivreurController {

    private final LivreurService livreurService;

    public LivreurController(LivreurService livreurService) {
        this.livreurService = livreurService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("livreurs", livreurService.findAll());
        return "livreur/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        prepareForm(model, new Livreur());
        return "livreur/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Livreur livreur, Model model,
                       RedirectAttributes redirectAttributes) {
        normalize(livreur);
        if (livreur.getNom() == null || livreur.getNom().isEmpty()
                || livreur.getPrenom() == null || livreur.getPrenom().isEmpty()) {
            model.addAttribute("errorMessage", "Le nom et le prenom sont obligatoires.");
            prepareForm(model, livreur);
            return "livreur/form";
        }
        if (livreur.getCapaciteMax() != null && livreur.getCapaciteMax().signum() < 0) {
            model.addAttribute("errorMessage", "La capacite maximale ne peut pas etre negative.");
            prepareForm(model, livreur);
            return "livreur/form";
        }
        livreurService.save(livreur);
        redirectAttributes.addFlashAttribute("successMessage", "Livreur enregistre avec succes.");
        return "redirect:/livreurs";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model,
                           RedirectAttributes redirectAttributes) {
        return livreurService.findById(id).map(livreur -> {
            prepareForm(model, livreur);
            return "livreur/form";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("errorMessage", "Livreur introuvable.");
            return "redirect:/livreurs";
        });
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (livreurService.deleteById(id)) {
            redirectAttributes.addFlashAttribute("successMessage", "Livreur supprime avec succes.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Suppression impossible : livreur introuvable ou deja associe a des commandes ou incidents.");
        }
        return "redirect:/livreurs";
    }

    private void prepareForm(Model model, Livreur livreur) {
        if (livreur.getVehicule() == null) {
            livreur.setVehicule(new Vehicule());
        }
        model.addAttribute("livreur", livreur);
        model.addAttribute("statuts", livreurService.getAllStatuts());
    }

    private void normalize(Livreur livreur) {
        livreur.setNom(trim(livreur.getNom()));
        livreur.setPrenom(trim(livreur.getPrenom()));
        livreur.setTelephone(trim(livreur.getTelephone()));
        livreur.setZoneActivite(trim(livreur.getZoneActivite()));
        if (livreur.getVehicule() != null) {
            livreur.getVehicule().setType(trim(livreur.getVehicule().getType()));
            livreur.getVehicule().setImmatriculation(trim(livreur.getVehicule().getImmatriculation()));
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}