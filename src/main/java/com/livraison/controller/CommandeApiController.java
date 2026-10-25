package com.livraison.controller;

import com.livraison.dto.AssignerRequest;
import com.livraison.dto.CommandeCreateRequest;
import com.livraison.dto.CommandeDto;
import com.livraison.dto.CommandePositionDto;
import com.livraison.dto.IncidentDto;
import com.livraison.dto.IncidentRequest;
import com.livraison.dto.LivreurPositionDto;
import com.livraison.dto.PreuveLivraisonRequest;
import com.livraison.dto.PrixCalculeDto;
import com.livraison.dto.RouteDto;
import com.livraison.dto.StatistiquesDto;
import com.livraison.dto.TrackingDto;
import com.livraison.entity.Client;
import com.livraison.entity.Commande;
import com.livraison.entity.Incident;
import com.livraison.entity.Livreur;
import com.livraison.entity.embeddable.PreuveLivraison;
import com.livraison.entity.enums.OrigineHistorique;
import com.livraison.entity.enums.PrioriteCommande;
import com.livraison.entity.enums.StatutCommande;
import com.livraison.entity.enums.StatutLivreur;
import com.livraison.service.CommandeService;
import com.livraison.service.GeoService;
import com.livraison.service.HistoriqueService;
import com.livraison.service.IncidentService;
import com.livraison.service.LivreurService;
import com.livraison.service.RouteService;
import com.livraison.service.StatistiquesService;
import com.livraison.service.StatutWorkflowService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
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

@RestController
public class CommandeApiController {

    private final CommandeService commandeService;
    private final LivreurService livreurService;
    private final StatutWorkflowService statutWorkflowService;
    private final StatistiquesService statistiquesService;
    private final GeoService geoService;
    private final RouteService routeService;
    private final HistoriqueService historiqueService;
    private final IncidentService incidentService;

    public CommandeApiController(CommandeService commandeService,
                                 LivreurService livreurService,
                                 StatutWorkflowService statutWorkflowService,
                                 StatistiquesService statistiquesService,
                                 GeoService geoService,
                                 RouteService routeService,
                                 HistoriqueService historiqueService,
                                 IncidentService incidentService) {
        this.commandeService = commandeService;
        this.livreurService = livreurService;
        this.statutWorkflowService = statutWorkflowService;
        this.statistiquesService = statistiquesService;
          this.geoService = geoService;
          this.routeService = routeService;
          this.historiqueService = historiqueService;
          this.incidentService = incidentService;
      }

    @GetMapping("/api/orders")
    public List<CommandeDto> lister() {
        return commandeService.findAll().stream().map(this::toDto).toList();
    }

    @GetMapping("/api/orders/{id}")
    public CommandeDto detail(@PathVariable Long id) {
        return toDtoAvecHistorique(commandeService.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Commande " + id + " introuvable")));
    }

    @PostMapping("/api/orders")
    public ResponseEntity<CommandeDto> creer(@RequestBody CommandeCreateRequest request) {
        Client client = commandeService.findClientById(request.getClientId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Client " + request.getClientId() + " introuvable"));
        Commande commande = new Commande();
        commande.setNumero(request.getNumero() != null && !request.getNumero().isBlank()
                ? request.getNumero()
                : "CMD-" + System.currentTimeMillis());
        commande.setClient(client);
        commande.setAdresseEnlevement(request.getAdresseEnlevement());
        commande.setAdresseLivraison(request.getAdresseLivraison());
        commande.setZoneLivraison(request.getZoneLivraison());
        commande.setPoids(request.getPoids());
        commande.setVolume(request.getVolume());
        commande.setPrix(request.getPrix());
        commande.setDatePrevue(request.getDatePrevue());
        commande.setPriorite(request.getPriorite() != null
                ? PrioriteCommande.valueOf(request.getPriorite())
                : PrioriteCommande.NORMALE);
        commande.setStatut(StatutCommande.CREEE);
        geolocaliser(commande);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toDto(commandeService.save(commande, OrigineHistorique.API)));
    }

    @PostMapping("/api/orders/{id}/assign")
    public CommandeDto assigner(@PathVariable Long id, @RequestBody AssignerRequest request) {
        Livreur livreur = livreurService.findById(request.getLivreurId()).orElseThrow(
                () -> new EntityNotFoundException("Livreur " + request.getLivreurId() + " introuvable"));
        return toDto(commandeService.assigner(id, livreur));
    }

    /** Selectionne et affecte automatiquement le livreur au meilleur score. */
    @PostMapping("/api/orders/{id}/assign-best")
    public ResponseEntity<CommandeDto> assignerMeilleur(@PathVariable Long id) {
        if (commandeService.affecterMeilleurLivreur(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        Commande commande = commandeService.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Commande " + id + " introuvable"));
        return ResponseEntity.ok(toDto(commande));
    }

    /** Cree un incident rattache a la commande indiquee par l'URL. */
    @PostMapping("/api/orders/{id}/incidents")
    public ResponseEntity<IncidentDto> signalerIncident(@PathVariable Long id,
                                                        @RequestBody IncidentRequest request) {
        Incident incident = incidentService.signaler(request.getType(), request.getDescription(),
                id, request.getLivreurId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incidentService.findDto(incident.getId()));
    }

    @PostMapping("/api/orders/{id}/pickup")
    public CommandeDto pickup(@PathVariable Long id) {
        return toDto(commandeService.changerStatut(id, StatutCommande.PRISE_EN_CHARGE, null, OrigineHistorique.API));
    }

    @PostMapping("/api/orders/{id}/start")
    public CommandeDto start(@PathVariable Long id) {
        return toDto(commandeService.changerStatut(id, StatutCommande.EN_TRANSIT, null, OrigineHistorique.API));
    }

    @PostMapping("/api/orders/{id}/deliver")
    public CommandeDto deliver(@PathVariable Long id,
                               @RequestBody(required = false) PreuveLivraisonRequest request) {
        PreuveLivraison preuve = null;
        if (request != null) {
            preuve = new PreuveLivraison(request.getDateHeure(), request.getNomReceptionnaire(),
                    request.getCodeConfirmation(), request.getCommentaire());
        }
        return toDto(commandeService.changerStatut(id, StatutCommande.LIVREE, preuve, OrigineHistorique.API));
    }

    @PostMapping("/api/orders/{id}/cancel")
    public CommandeDto cancel(@PathVariable Long id) {
        return toDto(commandeService.changerStatut(id, StatutCommande.ANNULEE, null, OrigineHistorique.API));
    }

    @GetMapping("/api/deliverers/available")
    public List<LivreurPositionDto> livreursDisponibles() {
        return livreurService.findAll().stream()
                .filter(l -> l.getStatut() == StatutLivreur.DISPONIBLE)
                .map(l -> new LivreurPositionDto(l.getId(), l.getNom(), l.getPrenom(),
                        l.getLatitude(), l.getLongitude(),
                        l.getStatut() != null ? l.getStatut().name() : null, l.getZoneActivite()))
                .toList();
    }

    @GetMapping("/api/orders/{id}/tracking")
    public TrackingDto tracking(@PathVariable Long id) {
        Commande commande = commandeService.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Commande " + id + " introuvable"));
        Livreur livreur = commande.getLivreur();
        TrackingDto dto = new TrackingDto();
        dto.setCommandeId(commande.getId());
        dto.setNumero(commande.getNumero());
        dto.setStatut(commande.getStatut() != null ? commande.getStatut().name() : null);
        dto.setLivreurNom(livreur != null ? livreur.getPrenom() + " " + livreur.getNom() : null);
        dto.setLivreurLatitude(livreur != null ? livreur.getLatitude() : null);
        dto.setLivreurLongitude(livreur != null ? livreur.getLongitude() : null);
        dto.setAdresseEnlevement(commande.getAdresseEnlevement());
        dto.setAdresseLivraison(commande.getAdresseLivraison());
        dto.setLatitudeEnlevement(commande.getLatitudeEnlevement());
        dto.setLongitudeEnlevement(commande.getLongitudeEnlevement());
        dto.setLatitudeLivraison(commande.getLatitudeLivraison());
        dto.setLongitudeLivraison(commande.getLongitudeLivraison());
        dto.setHistorique(historiqueService.historiqueDto(commande.getId()));
        return dto;
    }

    @GetMapping("/api/statistics/deliveries")
    public StatistiquesDto statistiquesLivraisons(
            @RequestParam(required = false) LocalDate debut,
            @RequestParam(required = false) LocalDate fin,
            @RequestParam(required = false) Long livreurId,
            @RequestParam(required = false) String zone) {
        return statistiquesService.calculer(debut, fin, livreurId, zone);
    }

    @GetMapping("/api/statistics/revenue")
    public StatistiquesDto statistiquesRevenu(
            @RequestParam(required = false) LocalDate debut,
            @RequestParam(required = false) LocalDate fin,
            @RequestParam(required = false) Long livreurId,
            @RequestParam(required = false) String zone) {
        return statistiquesService.calculer(debut, fin, livreurId, zone);
    }

    @GetMapping("/api/orders/positions")
    public List<CommandePositionDto> positionsCommandes() {
        return commandeService.findAllPositionsGeolocalisees();
    }

    @GetMapping("/api/orders/{id}/route")
    public ResponseEntity<RouteDto> itineraire(@PathVariable Long id) {
        Commande commande = commandeService.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Commande " + id + " introuvable"));
        if (commande.getLatitudeLivraison() == null || commande.getLongitudeLivraison() == null) {
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
        return ResponseEntity.ok(routeService.calculerRoute(latDepart, lonDepart,
                commande.getLatitudeLivraison(), commande.getLongitudeLivraison()));
    }

    @GetMapping("/api/orders/{id}/transitions")
    public List<String> transitions(@PathVariable Long id) {
        return commandeService.getTransitionsPossibles(id).stream().map(Enum::name).toList();
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
            commande.setDistance(java.math.BigDecimal.valueOf(Math.round(km * 100.0) / 100.0));
        }
    }

    /** Detail d'une commande : ajoute le journal des statuts (requete supplementaire). */
    private CommandeDto toDtoAvecHistorique(Commande commande) {
        CommandeDto dto = toDto(commande);
        dto.setHistorique(historiqueService.historiqueDto(commande.getId()));
        return dto;
    }

    /** Recalcule le prix depuis le bareme courant et renvoie le detail des postes. */
    @PostMapping("/api/orders/{id}/recalculate-price")
    public PrixCalculeDto recalculerPrix(@PathVariable Long id) {
        return commandeService.recalculerPrix(id);
    }

    private CommandeDto toDto(Commande commande) {
        Client client = commande.getClient();
        Livreur livreur = commande.getLivreur();
        CommandeDto dto = new CommandeDto();
        dto.setId(commande.getId());
        dto.setNumero(commande.getNumero());
        dto.setClientId(client != null ? client.getId() : null);
        dto.setClientNom(client != null ? client.getNom() : null);
        dto.setAdresseEnlevement(commande.getAdresseEnlevement());
        dto.setAdresseLivraison(commande.getAdresseLivraison());
        dto.setZoneLivraison(commande.getZoneLivraison());
        dto.setPoids(commande.getPoids());
        dto.setVolume(commande.getVolume());
        dto.setDistance(commande.getDistance());
        dto.setPrix(commande.getPrix());
        dto.setPriorite(commande.getPriorite() != null ? commande.getPriorite().name() : null);
        dto.setDatePrevue(commande.getDatePrevue());
        dto.setStatut(commande.getStatut() != null ? commande.getStatut().name() : null);
        dto.setLivreurId(livreur != null ? livreur.getId() : null);
        dto.setLivreurNom(livreur != null ? livreur.getPrenom() + " " + livreur.getNom() : null);
        dto.setNombreColis(commande.getColis() != null ? commande.getColis().size() : 0);
        dto.setStatutsSuivants(commande.getStatut() == null
                ? List.of()
                : statutWorkflowService.transitionsPossibles(commande.getStatut()).stream()
                .map(Enum::name).sorted().toList());
        return dto;
    }
}
