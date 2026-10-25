package com.livraison.controller;

import com.livraison.dto.LivreurScoreDto;
import com.livraison.dto.PrixCalculeDto;
import com.livraison.entity.Client;
import com.livraison.entity.Commande;
import com.livraison.entity.Livreur;
import com.livraison.entity.embeddable.PreuveLivraison;
import com.livraison.entity.enums.PrioriteCommande;
import com.livraison.entity.enums.StatutCommande;
import com.livraison.service.AttributionService;
import com.livraison.service.CommandeService;
import com.livraison.service.GeoService;
import com.livraison.service.HistoriqueService;
import com.livraison.service.IncidentService;
import com.livraison.service.LivreurService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/commandes")
public class CommandeController {

    private final CommandeService commandeService;
    private final GeoService geoService;
    private final HistoriqueService historiqueService;
    private final IncidentService incidentService;
    private final AttributionService attributionService;
    private final LivreurService livreurService;

    public CommandeController(CommandeService commandeService, GeoService geoService,
                              HistoriqueService historiqueService,
                              IncidentService incidentService,
                              AttributionService attributionService,
                              LivreurService livreurService) {
        this.commandeService = commandeService;
        this.geoService = geoService;
        this.historiqueService = historiqueService;
        this.incidentService = incidentService;
        this.attributionService = attributionService;
        this.livreurService = livreurService;
    }

    @GetMapping
    public String list(@RequestParam(name = "onglet", required = false, defaultValue = "toutes") String onglet,
                       Model model) {
        List<Commande> toutes = commandeService.findAll();
        List<Commande> enAttente = commandeService.findEnAttente();
        List<Commande> aAffecter = commandeService.findAffectables();

        String ongletActif = switch (onglet) {
            case "attente", "affecter" -> onglet;
            default -> "toutes";
        };
        List<Commande> commandes = switch (ongletActif) {
            case "attente" -> enAttente;
            case "affecter" -> aAffecter;
            default -> toutes;
        };

        model.addAttribute("onglet", ongletActif);
        model.addAttribute("commandes", commandes);
        model.addAttribute("totalToutes", toutes.size());
        model.addAttribute("totalAttente", enAttente.size());
        model.addAttribute("totalAffecter", aAffecter.size());

        if ("affecter".equals(ongletActif)) {
            Map<Long, List<LivreurScoreDto>> candidats = new LinkedHashMap<>();
            for (Commande commande : commandes) {
                candidats.put(commande.getId(), attributionService.classerLivreurs(commande));
            }
            model.addAttribute("candidatsParCommande", candidats);
        }
        return "commande/list";
    }

    @PostMapping("/affecter/{id}")
    public String affecter(@PathVariable Long id, @RequestParam Long livreurId,
                           RedirectAttributes redirectAttributes) {
        try {
            Livreur livreur = livreurService.findById(livreurId)
                    .orElseThrow(() -> new EntityNotFoundException("Livreur " + livreurId + " introuvable"));
            commandeService.assigner(id, livreur);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Commande affectee a " + livreur.getPrenom() + " " + livreur.getNom() + " !");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/commandes?onglet=affecter";
    }

    @PostMapping("/affecter-auto/{id}")
    public String affecterAuto(@PathVariable Long id,
                               @RequestParam(name = "retour", required = false) String retour,
                               RedirectAttributes redirectAttributes) {
        try {
            Optional<Livreur> livreur = commandeService.affecterMeilleurLivreur(id);
            if (livreur.isPresent()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Affectation automatique : " + livreur.get().getPrenom() + " "
                                + livreur.get().getNom() + " (meilleur score).");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Aucun livreur disponible pour cette commande.");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        if (retour != null && retour.startsWith("/commandes")) {
            return "redirect:" + retour;
        }
        return "redirect:/commandes?onglet=affecter";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        Commande commande = new Commande();
        model.addAttribute("commande", commande);
        model.addAttribute("clients", commandeService.findAllClients());
        model.addAttribute("priorites", commandeService.getAllPriorites());
        model.addAttribute("zones", commandeService.getAllZones());
        return "commande/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("commande") Commande formulaire,
                       RedirectAttributes redirectAttributes) {
        Commande commande;
        if (formulaire.getId() != null) {
            Commande existante = commandeService.findById(formulaire.getId()).orElse(null);
            if (existante == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Commande non trouvee !");
                return "redirect:/commandes";
            }
            commande = existante;
        } else {
            commande = new Commande();
        }
        commande.setNumero(formulaire.getNumero());
        commande.setClient(formulaire.getClient());
        commande.setAdresseEnlevement(formulaire.getAdresseEnlevement());
        commande.setAdresseLivraison(formulaire.getAdresseLivraison());
        commande.setZoneLivraison(formulaire.getZoneLivraison());
        commande.setPoids(formulaire.getPoids());
        commande.setVolume(formulaire.getVolume());
        commande.setPrix(formulaire.getPrix());
        commande.setPriorite(formulaire.getPriorite());
        commande.setDatePrevue(formulaire.getDatePrevue());
        geolocaliser(commande);
        commandeService.save(commande);
        redirectAttributes.addFlashAttribute("successMessage", "Commande enregistree avec succes !");
        return "redirect:/commandes";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return commandeService.findById(id)
                .map(commande -> {
                    model.addAttribute("commande", commande);
                    model.addAttribute("clients", commandeService.findAllClients());
                    model.addAttribute("priorites", commandeService.getAllPriorites());
                    model.addAttribute("zones", commandeService.getAllZones());
                    return "commande/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Commande non trouvee !");
                    return "redirect:/commandes";
                });
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return commandeService.findById(id)
                .map(commande -> {
                      model.addAttribute("commande", commande);
                      model.addAttribute("transitions", commandeService.getTransitionsPossibles(id));
                      model.addAttribute("historique", historiqueService.historiqueDto(id));
model.addAttribute("incidents", incidentService.findByCommandeDto(id));
                    model.addAttribute("prixCalcule", commandeService.detailPrix(id));
                    return "commande/detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Commande non trouvee !");
                    return "redirect:/commandes";
                });
    }

    @PostMapping("/recalculer-prix/{id}")
    public String recalculerPrix(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            PrixCalculeDto detail = commandeService.recalculerPrix(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Prix recalcule : " + detail.getPrix() + " Ar (barème " + detail.getZone() + ")");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/commandes/detail/" + id;
    }

    @PostMapping("/{id}/transition")
    public String transition(@PathVariable Long id,
                             @RequestParam StatutCommande statut,
                             @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateHeure,
                             @RequestParam(required = false) String nomReceptionnaire,
                             @RequestParam(required = false) String codeConfirmation,
                             @RequestParam(required = false) String commentaire,
                             RedirectAttributes redirectAttributes) {
        try {
            PreuveLivraison preuve = null;
            if (statut == StatutCommande.LIVREE) {
                preuve = new PreuveLivraison(dateHeure, nomReceptionnaire, codeConfirmation, commentaire);
            }
            commandeService.changerStatut(id, statut, preuve);
            redirectAttributes.addFlashAttribute("successMessage", "Statut mis a jour : " + statut);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/commandes/detail/" + id;
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        commandeService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Commande supprimee avec succes !");
        return "redirect:/commandes";
    }

    private void geolocaliser(Commande commande) {
        GeoService.GeoPoint enlevement = geoService.geocoder(commande.getAdresseEnlevement());
        GeoService.GeoPoint livraison = geoService.geocoder(commande.getAdresseLivraison());
        if (enlevement != null) {
            commande.setLatitudeEnlevement(enlevement.latitude());
            commande.setLongitudeEnlevement(enlevement.longitude());
        }
        if (livraison != null) {
            commande.setLatitudeLivraison(livraison.latitude());
            commande.setLongitudeLivraison(livraison.longitude());
        }
        if (enlevement != null && livraison != null) {
            double km = geoService.distanceKm(enlevement.latitude(), enlevement.longitude(),
                    livraison.latitude(), livraison.longitude());
            commande.setDistance(BigDecimal.valueOf(Math.round(km * 100.0) / 100.0));
        }
    }
}
