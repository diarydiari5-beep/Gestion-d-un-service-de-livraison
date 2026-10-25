package com.livraison.controller;

import com.livraison.dto.CommandePositionDto;
import com.livraison.dto.LivreurScoreDto;
import com.livraison.dto.RouteDto;
import com.livraison.entity.Commande;
import com.livraison.entity.Livreur;
import com.livraison.entity.enums.StatutCommande;
import com.livraison.service.AttributionService;
import com.livraison.service.CommandeService;
import com.livraison.service.LivreurService;
import com.livraison.service.RouteService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AttributionController {

    private final CommandeService commandeService;
    private final LivreurService livreurService;
    private final AttributionService attributionService;
    private final RouteService routeService;

    public AttributionController(CommandeService commandeService,
                                 LivreurService livreurService,
                                 AttributionService attributionService,
                                 RouteService routeService) {
        this.commandeService = commandeService;
        this.livreurService = livreurService;
        this.attributionService = attributionService;
        this.routeService = routeService;
    }

    @GetMapping("/api/commandes/{id}/candidats")
    public ResponseEntity<List<LivreurScoreDto>> candidats(@PathVariable Long id) {
        return commandeService.findById(id)
                .map(commande -> ResponseEntity.ok(attributionService.classerLivreurs(commande)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/api/commandes/{id}/assigner/{livreurId}")
    public ResponseEntity<Map<String, Object>> assigner(@PathVariable Long id,
                                                        @PathVariable Long livreurId) {
        Commande commande = commandeService.findById(id).orElse(null);
        Livreur livreur = livreurService.findById(livreurId).orElse(null);
        if (commande == null || livreur == null) {
            return ResponseEntity.notFound().build();
        }
        commandeService.assigner(id, livreur);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "commandeId", id,
                "livreurId", livreurId,
                "statut", StatutCommande.ASSIGNEE.name()));
    }

    @GetMapping("/api/commandes/positions")
    public List<CommandePositionDto> positionsCommandes() {
        return commandeService.findAllPositionsGeolocalisees();
    }

    @GetMapping("/api/commandes/{id}/itineraire")
    public ResponseEntity<RouteDto> itineraire(@PathVariable Long id,
                                               @RequestParam(defaultValue = "livraison") String vers) {
        Commande commande = commandeService.findById(id).orElse(null);
        if (commande == null) {
            return ResponseEntity.notFound().build();
        }
        boolean versEnlevement = "enlevement".equalsIgnoreCase(vers);
        Double latCible = versEnlevement ? commande.getLatitudeEnlevement() : commande.getLatitudeLivraison();
        Double lonCible = versEnlevement ? commande.getLongitudeEnlevement() : commande.getLongitudeLivraison();
        if (latCible == null || lonCible == null) {
            return ResponseEntity.notFound().build();
        }
        Double latDepart = commande.getLatitudeEnlevement();
        Double lonDepart = commande.getLongitudeEnlevement();
        Livreur livreur = commande.getLivreur();
        if (livreur != null && livreur.getLatitude() != null && livreur.getLongitude() != null) {
            latDepart = livreur.getLatitude();
            lonDepart = livreur.getLongitude();
        }
        if (latDepart == null || lonDepart == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(routeService.calculerRoute(latDepart, lonDepart, latCible, lonCible));
    }
}