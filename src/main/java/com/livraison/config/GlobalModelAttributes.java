package com.livraison.config;

import com.livraison.service.UtilisateurService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expose l'utilisateur connecte a toutes les vues Thymeleaf (barre de
 * navigation) : nom affichable et role, utilises pour n'afficher que les
 * entrees de menu autorisees.
 */
@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAttributes {

    private static final String PREFIXE_ROLE = "ROLE_";

    private final UtilisateurService utilisateurService;

    public GlobalModelAttributes(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @ModelAttribute
    public void attributsUtilisateur(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }
        utilisateurService.findByUsername(authentication.getName()).ifPresentOrElse(
                utilisateur -> {
                    model.addAttribute("utilisateurNom", utilisateur.getNomComplet());
                    model.addAttribute("roleUtilisateur", utilisateur.getRole().name());
                },
                () -> {
                    model.addAttribute("utilisateurNom", authentication.getName());
                    authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .filter(authorite -> authorite.startsWith(PREFIXE_ROLE))
                            .findFirst()
                            .ifPresent(authorite -> model.addAttribute(
                                    "roleUtilisateur", authorite.substring(PREFIXE_ROLE.length())));
                });
    }
}
