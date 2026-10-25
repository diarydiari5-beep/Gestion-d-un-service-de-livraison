package com.livraison.service;

import com.livraison.dto.AlerteSuivi;
import com.livraison.dto.RouteDto;
import com.livraison.dto.SuiviLigneDto;
import com.livraison.entity.Client;
import com.livraison.entity.Commande;
import com.livraison.entity.Livreur;
import com.livraison.entity.enums.StatutCommande;
import com.livraison.entity.enums.StatutLivreur;
import com.livraison.repository.IncidentRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tableau de bord de suivi des livraisons en cours.
 *
 * Les distances et les durees restantes sont les valeurs de l'itineraire
 * routier calcule par RouteService (donc le long de la voirie). Si aucun
 * fournisseur de routage n'est joignable, RouteService renvoie un repli en
 * ligne droite : la ligne est alors marquee "estimation" et ne pretend pas
 * etre une distance routiere.
 *
 * Le resultat est mis en cache quelques secondes : la page se rafraichit toutes
 * les 5 secondes, on ne veut pas un appel de routage par commande et par page.
 */
@Service
public class SuiviService {

    static final int DUREE_CACHE_SECONDES = 30;
    static final double SEUIL_ARRIVEE_PROCHE_KM = 0.3;
    static final double SEUIL_TRAITEMENT_KM = 0.05;
    static final String LIBELLE_ROUTIER = "Trajet routier";
    static final String LIBELLE_ESTIMATION = "Estimation (ligne droite)";

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.FRANCE);
    private static final String INCONNU = "—";

    private final CommandeService commandeService;
    private final RouteService routeService;
    private final GeoService geoService;
    private final IncidentRepository incidentRepository;

    private volatile List<SuiviLigneDto> lignes = List.of();
    private volatile Instant dernierCalcul;

    public SuiviService(CommandeService commandeService, RouteService routeService,
                        GeoService geoService, IncidentRepository incidentRepository) {
        this.commandeService = commandeService;
        this.routeService = routeService;
        this.geoService = geoService;
        this.incidentRepository = incidentRepository;
    }

    /** Lignes du tableau de bord, recalculees si le cache est perime. */
    @Transactional(readOnly = true)
    public List<SuiviLigneDto> lignes() {
        if (cachePerime()) {
            return rafraichir();
        }
        return lignes;
    }

    @Transactional(readOnly = true)
    public List<SuiviLigneDto> rafraichir() {
        List<Commande> commandes = commandeService.findGeolocalisees();
        Map<Long, Integer> charges = chargesParLivreur(commandes);
        Set<Long> incidentsOuverts = incidentsOuverts();
        List<SuiviLigneDto> resultat = new ArrayList<>();
        for (Commande commande : commandes) {
            resultat.add(construire(commande, charges, incidentsOuverts));
        }
        resultat.sort(Comparator.comparing(SuiviLigneDto::getDatePrevue,
                Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SuiviLigneDto::getNumero, Comparator.nullsLast(Comparator.naturalOrder())));
        lignes = List.copyOf(resultat);
        dernierCalcul = Instant.now();
        return lignes;
    }

    public Instant getDernierCalcul() {
        return dernierCalcul;
    }

    /**
     * Identifiants des commandes portant au moins un incident ouvert. Une seule
     * requete alimente toutes les lignes du tableau.
     */
    private Set<Long> incidentsOuverts() {
        return incidentRepository.findCommandeIdsAvecIncidentsOuverts(
                IncidentService.statutsOuverts());
    }

    /** Construit la ligne d'une commande ; utilise l'itineraire routier pour la distance. */
    public SuiviLigneDto construire(Commande commande, Map<Long, Integer> charges) {
        return construire(commande, charges, Set.of());
    }

    /**
     * Construit la ligne d'une commande. Les identifiants d'incidents ouverts
     * sont fournis par l'appelant pour eviter une requete par ligne du tableau.
     */
    public SuiviLigneDto construire(Commande commande, Map<Long, Integer> charges,
                                     Set<Long> incidentsOuverts) {
        StatutCommande statut = commande.getStatut();
        boolean collecte = statut == StatutCommande.ASSIGNEE;
        Livreur livreur = commande.getLivreur();
        Client client = commande.getClient();

        Double latCible = collecte ? commande.getLatitudeEnlevement() : commande.getLatitudeLivraison();
        Double lonCible = collecte ? commande.getLongitudeEnlevement() : commande.getLongitudeLivraison();

        SuiviLigneDto ligne = new SuiviLigneDto();
        ligne.setId(commande.getId());
        ligne.setNumero(commande.getNumero());
        ligne.setClientNom(client == null ? INCONNU : (client.getPrenom() + " " + client.getNom()).trim());
        ligne.setZoneLivraison(commande.getZoneLivraison());
        ligne.setPriorite(commande.getPriorite() == null ? null : commande.getPriorite().name());
        ligne.setStatut(statut == null ? null : statut.name());
        ligne.setPhase(libellePhase(statut));
        ligne.setCibleLibelle(collecte ? "Enlèvement" : "Livraison");
        ligne.setLivreurNom(livreur == null ? null : (livreur.getPrenom() + " " + livreur.getNom()).trim());
        ligne.setLivreurStatut(livreur == null || livreur.getStatut() == null
                ? null : libelleStatutLivreur(livreur.getStatut()));
        ligne.setDatePrevue(commande.getDatePrevue());
        ligne.setDatePrevueAffichee(commande.getDatePrevue() == null
                ? INCONNU : FORMAT_DATE.format(commande.getDatePrevue()));
        ligne.setCommandesActives(livreur == null ? 0 : charges.getOrDefault(livreur.getId(), 0));

        List<AlerteSuivi> alertes = new ArrayList<>();
        if (commande.getDatePrevue() != null && commande.getDatePrevue().isBefore(LocalDateTime.now())) {
            alertes.add(AlerteSuivi.RETARD);
        }
        if (livreur == null) {
            alertes.add(AlerteSuivi.SANS_LIVREUR);
        }
        if (incidentsOuverts.contains(commande.getId())) {
            ligne.setIncidentsOuverts(1);
            alertes.add(AlerteSuivi.INCIDENT);
        }

        RouteDto route = null;
        if (livreur == null) {
            alertes.add(AlerteSuivi.NON_ASSIGNEE);
        } else if (livreur.getLatitude() == null || livreur.getLongitude() == null) {
            alertes.add(AlerteSuivi.POSITION_INCONNUE);
        } else if (latCible == null || lonCible == null) {
            alertes.add(AlerteSuivi.CIBLE_INCONNUE);
        } else {
            route = routeService.calculerRoute(livreur.getLatitude(), livreur.getLongitude(), latCible, lonCible);
        }

        if (route != null) {
            ligne.setDistanceKm(route.getDistanceKm());
            ligne.setDistanceAffichee(String.format(Locale.FRANCE, "%.2f km", route.getDistanceKm()));
            ligne.setDureeMinutes(route.getDurationMinutes());
            ligne.setEtaAffichee(String.format(Locale.FRANCE, "%.0f min", route.getDurationMinutes()));
            ligne.setDistanceRoutiere(!route.isApproximative());
            ligne.setLibelleDistance(route.isApproximative() ? LIBELLE_ESTIMATION : LIBELLE_ROUTIER);
            if (route.getDistanceKm() < SEUIL_ARRIVEE_PROCHE_KM) {
                alertes.add(AlerteSuivi.PRES_ARRIVEE);
            }
            ligne.setAvancement(avancement(commande, collecte));
        } else {
            ligne.setDistanceAffichee(INCONNU);
            ligne.setEtaAffichee(INCONNU);
            ligne.setLibelleDistance(INCONNU);
        }

        ligne.setAlertes(alertes);
        return ligne;
    }

    /**
     * Avancement en pourcentage du trajet enlèvement -> livraison. Non calculé
     * pendant la collecte : le livreur n'a pas encore chargé le colis, et la
     * distance qui le sépare du point d'enlèvement n'appartient pas au trajet.
     *
     * L'avancement mesure la position du livreur le long du tracé routier
     * (projection sur la polyligne), et non un ratio de deux distances : le
     * livreur peut arriver par un autre chemin ou partir plus loin que le
     * point d'enlèvement, auquel cas un ratio donnerait 0 % à l'infini.
     */
    private Integer avancement(Commande commande, boolean collecte) {
        if (collecte) {
            return null;
        }
        RouteDto trajet = trajetEnlevementLivraison(commande);
        if (trajet == null) {
            return null;
        }
        double total = longueurTrace(trajet);
        if (total < SEUIL_TRAITEMENT_KM) {
            return null;
        }
        Livreur livreur = commande.getLivreur();
        double parcouru = 0.0;
        if (livreur != null && livreur.getLatitude() != null && livreur.getLongitude() != null) {
            parcouru = distanceLeLongDeLaRoute(trajet, livreur.getLatitude(), livreur.getLongitude());
        }
        int pourcentage = (int) Math.round((parcouru / total) * 100.0);
        return Math.max(0, Math.min(100, pourcentage));
    }

    /** Itinéraire complet enlèvement -> livraison, servant de référence à l'avancement. */
    private RouteDto trajetEnlevementLivraison(Commande commande) {
        if (commande.getLatitudeEnlevement() == null || commande.getLongitudeEnlevement() == null
                || commande.getLatitudeLivraison() == null || commande.getLongitudeLivraison() == null) {
            return null;
        }
        return routeService.calculerRoute(commande.getLatitudeEnlevement(), commande.getLongitudeEnlevement(),
                commande.getLatitudeLivraison(), commande.getLongitudeLivraison());
    }

    private double longueurTrace(RouteDto route) {
        List<double[]> points = route.getPoints();
        double total = 0.0;
        for (int i = 1; i < points.size(); i++) {
            total += longueurSegment(points.get(i - 1), points.get(i));
        }
        return total;
    }

    /** Distance déjà parcourue le long du tracé, mesurée jusqu'au point le plus proche. */
    private double distanceLeLongDeLaRoute(RouteDto route, double lat, double lon) {
        List<double[]> points = route.getPoints();
        if (points.size() < 2) {
            return 0.0;
        }
        double parcouru = 0.0;
        double meilleurEcart = Double.MAX_VALUE;
        double meilleurParcouru = 0.0;
        for (int i = 1; i < points.size(); i++) {
            double[] debut = points.get(i - 1);
            double[] fin = points.get(i);
            double portion = longueurSegment(debut, fin);
            double t = projectionSurSegment(debut, fin, lat, lon);
            double ecartLat = debut[0] + t * (fin[0] - debut[0]) - lat;
            double ecartLon = debut[1] + t * (fin[1] - debut[1]) - lon;
            double ecart = ecartLat * ecartLat + ecartLon * ecartLon;
            if (ecart < meilleurEcart) {
                meilleurEcart = ecart;
                meilleurParcouru = parcouru + t * portion;
            }
            parcouru += portion;
        }
        return Math.min(meilleurParcouru, parcouru);
    }

    private double projectionSurSegment(double[] debut, double[] fin, double lat, double lon) {
        double dx = fin[0] - debut[0];
        double dy = fin[1] - debut[1];
        double longueurCarree = dx * dx + dy * dy;
        if (longueurCarree == 0.0) {
            return 0.0;
        }
        double t = ((lat - debut[0]) * dx + (lon - debut[1]) * dy) / longueurCarree;
        return Math.max(0.0, Math.min(1.0, t));
    }

    private double longueurSegment(double[] a, double[] b) {
        return geoService.distanceKm(a[0], a[1], b[0], b[1]);
    }

    private Map<Long, Integer> chargesParLivreur(List<Commande> commandes) {
        Map<Long, Integer> charges = new HashMap<>();
        for (Commande commande : commandes) {
            Livreur livreur = commande.getLivreur();
            if (livreur != null) {
                charges.merge(livreur.getId(), 1, Integer::sum);
            }
        }
        return charges;
    }

    private String libellePhase(StatutCommande statut) {
        if (statut == null) {
            return INCONNU;
        }
        return switch (statut) {
            case ASSIGNEE -> "Collecte";
            case PRISE_EN_CHARGE -> "En charge";
            case EN_TRANSIT -> "En transit";
            default -> statut.name();
        };
    }

    private String libelleStatutLivreur(StatutLivreur statut) {
        return switch (statut) {
            case DISPONIBLE -> "Disponible";
            case EN_LIVRAISON -> "En livraison";
            case INDISPONIBLE -> "Indisponible";
            case PAUSE -> "En pause";
        };
    }

    private boolean cachePerime() {
        return dernierCalcul == null
                || dernierCalcul.plusSeconds(DUREE_CACHE_SECONDES).isBefore(Instant.now());
    }
}
