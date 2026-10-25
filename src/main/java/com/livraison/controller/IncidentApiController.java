package com.livraison.controller;

import com.livraison.dto.IncidentDto;
import com.livraison.dto.IncidentRequest;
import com.livraison.dto.IncidentResolutionRequest;
import com.livraison.entity.enums.StatutIncident;
import com.livraison.entity.enums.TypeIncident;
import com.livraison.service.IncidentService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST des incidents de livraison.
 *
 * Les codes HTTP suivent ceux de ApiExceptionHandler : 404 si l'incident ou
 * la commande n'existe pas, 409 si la regle de gestion refuse le traitement,
 * 400 si la requete est incomplete.
 */
@RestController
public class IncidentApiController {

    private final IncidentService incidentService;

    public IncidentApiController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping("/api/incidents")
    public List<IncidentDto> lister(@RequestParam(required = false) StatutIncident statut,
                                    @RequestParam(required = false) TypeIncident type) {
        if (type != null) {
            return incidentService.findByStatutDto(statut).stream()
                    .filter(incident -> type.name().equals(incident.getType()))
                    .toList();
        }
        return incidentService.findByStatutDto(statut);
    }

    @GetMapping("/api/incidents/{id}")
    public IncidentDto detail(@PathVariable Long id) {
        return incidentService.findDto(id);
    }

    /** Incidents d'une commande, du plus recent au plus ancien. */
    @GetMapping("/api/incidents/commande/{commandeId}")
    public List<IncidentDto> parCommande(@PathVariable Long commandeId) {
        return incidentService.findByCommandeDto(commandeId);
    }

    @GetMapping("/api/incidents/livreur/{livreurId}")
    public List<IncidentDto> parLivreur(@PathVariable Long livreurId) {
        return incidentService.findByLivreurDto(livreurId);
    }

    @GetMapping("/api/incidents/stats")
    public Map<String, Object> statistiques() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("nombreOuverts", incidentService.nombreOuverts());
        stats.put("parStatut", incidentService.compteursParStatut());
        stats.put("parType", incidentService.compteursParType());
        return stats;
    }

    /** Transitions que l'operateur peut proposer sur la commande de l'incident. */
    @GetMapping("/api/incidents/{id}/transitions")
    public List<String> transitions(@PathVariable Long id) {
        return incidentService.transitionsProposables(id).stream().map(Enum::name).toList();
    }

    @PostMapping("/api/incidents")
    public ResponseEntity<IncidentDto> signaler(@RequestBody IncidentRequest request) {
        var incident = incidentService.signaler(request.getType(), request.getDescription(),
                request.getCommandeId(), request.getLivreurId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incidentService.findDto(incident.getId()));
    }

    @PostMapping("/api/incidents/{id}/traiter")
    public IncidentDto traiter(@PathVariable Long id) {
        incidentService.traiter(id);
        return incidentService.findDto(id);
    }

    /** Cloture l'incident ; la transition proposee est appliquee si elle est fournie. */
    @PostMapping("/api/incidents/{id}/resoudre")
    public IncidentDto resoudre(@PathVariable Long id, @RequestBody IncidentResolutionRequest request) {
        incidentService.resoudre(id, request.getResolution(), request.getTransition());
        return incidentService.findDto(id);
    }

    @PostMapping("/api/incidents/{id}/rejeter")
    public IncidentDto rejeter(@PathVariable Long id, @RequestBody IncidentResolutionRequest request) {
        incidentService.rejeter(id, request.getResolution());
        return incidentService.findDto(id);
    }

    @PostMapping("/api/incidents/{id}/delete")
    public Map<String, Object> supprimer(@PathVariable Long id) {
        incidentService.supprimer(id);
        return Map.of("success", true, "message", "Incident " + id + " supprime");
    }

    /** Exposition des statuts d'incidents pour un client qui veut les lister. */
    @GetMapping("/api/incidents/statuts")
    public Map<String, String> statuts() {
        Map<String, String> libelles = new LinkedHashMap<>();
        for (StatutIncident statut : IncidentService.TOUS_STATUTS) {
            libelles.put(statut.name(), IncidentService.libelleStatut(statut));
        }
        return libelles;
    }

    /** Exposition des types d'incidents et de leur libelle. */
    @GetMapping("/api/incidents/types")
    public Map<String, String> types() {
        Map<String, String> libelles = new LinkedHashMap<>();
        for (TypeIncident type : TypeIncident.values()) {
            libelles.put(type.name(), IncidentService.libelleType(type));
        }
        return libelles;
    }
}
