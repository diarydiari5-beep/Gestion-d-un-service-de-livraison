package com.livraison.controller;

import com.livraison.entity.Utilisateur;
import com.livraison.entity.enums.RoleUtilisateur;
import com.livraison.service.LivreurService;
import com.livraison.service.UtilisateurService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Gestion des comptes applicatifs, reservee au role ADMIN (voir SecurityConfig).
 */
@Controller
@RequestMapping("/utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final LivreurService livreurService;

    public UtilisateurController(UtilisateurService utilisateurService,
                                 LivreurService livreurService) {
        this.utilisateurService = utilisateurService;
        this.livreurService = livreurService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("utilisateurs", utilisateurService.findAll());
        return "utilisateur/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("utilisateur", new Utilisateur());
        preparerForm(model);
        return "utilisateur/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return utilisateurService.findById(id)
                .map(utilisateur -> {
                    model.addAttribute("utilisateur", utilisateur);
                    preparerForm(model);
                    return "utilisateur/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Utilisateur non trouve !");
                    return "redirect:/utilisateurs";
                });
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("utilisateur") Utilisateur utilisateur,
                       RedirectAttributes redirectAttributes) {
        boolean creation = utilisateur.getId() == null;
        if (creation && utilisateurService.usernameExiste(utilisateur.getUsername())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ce nom d'utilisateur est déjà utilisé.");
            return "redirect:/utilisateurs/new";
        }
        if (creation && (utilisateur.getPassword() == null || utilisateur.getPassword().isBlank())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Le mot de passe est obligatoire à la création.");
            return "redirect:/utilisateurs/new";
        }
        utilisateurService.save(utilisateur);
        redirectAttributes.addFlashAttribute("successMessage", "Utilisateur enregistré avec succès !");
        return "redirect:/utilisateurs";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        utilisateurService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Utilisateur supprimé !");
        return "redirect:/utilisateurs";
    }

    private void preparerForm(Model model) {
        model.addAttribute("roles", RoleUtilisateur.values());
        model.addAttribute("livreurs", livreurService.findAll());
    }
}
