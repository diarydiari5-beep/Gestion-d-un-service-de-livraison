package com.livraison.controller;

import com.livraison.dto.IncidentDto;
import com.livraison.entity.enums.StatutCommande;
import com.livraison.entity.enums.StatutIncident;
import com.livraison.entity.enums.TypeIncident;
import com.livraison.service.CommandeService;
import com.livraison.service.IncidentService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Ecran de traitement des incidents : liste filtree, signalement, mise en
 * traitement, resolution avec transition proposee, rejet.
 */
@Controller
@RequestMapping("/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final CommandeService commandeService;

    public IncidentController(IncidentService incidentService, CommandeService commandeService) {
        this.incidentService = incidentService;
        this.commandeService = commandeService;
    }

    @GetMapping
    public String lister(@RequestParam(required = false) StatutIncident statut,
                         @RequestParam(required = false) TypeIncident type,
                         Model model) {
        List<IncidentDto> incidents = incidentService.findByStatutDto(statut);
        if (type != null) {
            incidents = incidents.stream()
                    .filter(incident -> type.name().equals(incident.getType()))
                    .toList();
        }
        model.addAttribute("incidents", incidents);
        model.addAttribute("statuts", IncidentService.TOUS_STATUTS);
        model.addAttribute("types", TypeIncident.values());
        model.addAttribute("statutSelectionne", statut);
        model.addAttribute("typeSelectionne", type);
        model.addAttribute("compteursStatut", incidentService.compteursParStatut());
        model.addAttribute("compteursType", incidentService.compteursParType());
        model.addAttribute("nombreOuverts", incidentService.nombreOuverts());
        return "incident/list";
    }

    @GetMapping("/new")
    public String formulaire(@RequestParam(required = false) Long commandeId, Model model) {
        model.addAttribute("types", TypeIncident.values());
        model.addAttribute("commandes", commandeService.findAll());
        model.addAttribute("commandeId", commandeId);
        return "incident/form";
    }

    @PostMapping("/save")
    public String enregistrer(@RequestParam TypeIncident type,
                              @RequestParam String description,
                              @RequestParam(required = false) Long commandeId,
                              @RequestParam(required = false) Long livreurId,
                              RedirectAttributes redirectAttributes) {
        try {
            var incident = incidentService.signaler(type, description, commandeId, livreurId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Incident signale sur la commande "
                            + (incident.getCommande() == null ? "-" : incident.getCommande().getNumero())
                            + ".");
            return "redirect:/incidents/" + incident.getId();
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/incidents";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        IncidentDto incident = incidentService.findDto(id);
        model.addAttribute("incident", incident);
        model.addAttribute("transitionsProposables", incidentService.transitionsProposables(id));
        return "incident/detail";
    }

    @PostMapping("/{id}/traiter")
    public String traiter(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return executer(id, redirectAttributes, () -> {
            incidentService.traiter(id);
            return "Incidentpris en charge.";
        });
    }

    @PostMapping("/{id}/resoudre")
    public String resoudre(@PathVariable Long id,
                           @RequestParam String resolution,
                           @RequestParam(required = false) StatutCommande transition,
                           RedirectAttributes redirectAttributes) {
        return executer(id, redirectAttributes, () -> {
            incidentService.resoudre(id, resolution, transition);
            return "Incident resolu.";
        });
    }

    @PostMapping("/{id}/rejeter")
    public String rejeter(@PathVariable Long id,
                          @RequestParam String motif,
                          RedirectAttributes redirectAttributes) {
        return executer(id, redirectAttributes, () -> {
            incidentService.rejeter(id, motif);
            return "Incident rejete.";
        });
    }

    @PostMapping("/delete/{id}")
    public String supprimer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return executer(id, redirectAttributes, () -> {
            incidentService.supprimer(id);
            return "Incident supprime.";
        });
    }

    /**
     * Execute une action de traitement. Une regle de gestion refusee est
     * renvoyee en message d'erreur plutot qu'une page d'erreur technique.
     * Le retour se fait sur l'incident concerne, ou sur la liste apres une
     * suppression.
     */
    private String executer(Long id, RedirectAttributes redirectAttributes, Action action) {
        try {
            redirectAttributes.addFlashAttribute("successMessage", action.execute());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return id == null ? "redirect:/incidents" : "redirect:/incidents/" + id;
    }

    @FunctionalInterface
    private interface Action {
        String execute();
    }
}
