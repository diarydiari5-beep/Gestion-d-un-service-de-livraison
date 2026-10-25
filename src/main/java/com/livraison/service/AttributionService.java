package com.livraison.service;

import com.livraison.dto.LivreurScoreDto;
import com.livraison.entity.Colis;
import com.livraison.entity.Commande;
import com.livraison.entity.Livreur;
import com.livraison.entity.enums.PrioriteCommande;
import com.livraison.entity.enums.StatutCommande;
import com.livraison.entity.enums.StatutLivreur;
import com.livraison.repository.CommandeRepository;
import com.livraison.repository.LivreurRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttributionService {

    private static final double SEUIL_DISTANCE_KM = 15.0;
    private static final double POIDS_PROXIMITE = 0.5;
    private static final double POIDS_CHARGE = 0.3;
    private static final double POIDS_CAPACITE = 0.2;
    private static final double SCORE_MAX = 100.0;
    private static final List<StatutCommande> STATUTS_ACTIFS = List.of(
            StatutCommande.ASSIGNEE,
            StatutCommande.PRISE_EN_CHARGE,
            StatutCommande.EN_TRANSIT);

    private final LivreurRepository livreurRepository;
    private final CommandeRepository commandeRepository;
    private final GeoService geoService;

    public AttributionService(LivreurRepository livreurRepository,
                              CommandeRepository commandeRepository,
                              GeoService geoService) {
        this.livreurRepository = livreurRepository;
        this.commandeRepository = commandeRepository;
        this.geoService = geoService;
    }

    @Transactional(readOnly = true)
    public List<LivreurScoreDto> classerLivreurs(Commande commande) {
        BigDecimal poidsTotal = calculerPoidsTotal(commande);
        return livreurRepository.findAll().stream()
                .filter(livreur -> estEligible(livreur, commande, poidsTotal))
                .map(livreur -> calculerScore(livreur, commande, poidsTotal))
                .sorted(Comparator.comparingDouble(LivreurScoreDto::getScore).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<LivreurScoreDto> trouverMeilleurLivreur(Commande commande) {
        return classerLivreurs(commande).stream().findFirst();
    }

    private boolean estEligible(Livreur livreur, Commande commande, BigDecimal poidsTotal) {
        if (livreur.getStatut() != StatutLivreur.DISPONIBLE) {
            return false;
        }
        if (!zoneCompatible(livreur.getZoneActivite(), commande.getZoneLivraison())) {
            return false;
        }
        BigDecimal capacite = capaciteDisponible(livreur);
        if (capacite != null && poidsTotal != null && capacite.compareTo(poidsTotal) < 0) {
            return false;
        }
        return true;
    }

    private boolean zoneCompatible(String zoneLivreur, String zoneCommande) {
        if (zoneCommande == null || zoneCommande.isBlank()) {
            return true;
        }
        return zoneLivreur != null && zoneLivreur.equalsIgnoreCase(zoneCommande);
    }

    private LivreurScoreDto calculerScore(Livreur livreur, Commande commande, BigDecimal poidsTotal) {
        double distanceKm = distanceVersLivraison(livreur, commande);
        double scoreProximite = distanceKm >= 0
                ? Math.max(0.0, 1.0 - Math.min(distanceKm / SEUIL_DISTANCE_KM, 1.0))
                : 0.0;

        long commandesActives = commandeRepository
                .countByLivreurIdAndStatutIn(livreur.getId(), STATUTS_ACTIFS);
        double scoreCharge = 1.0 / (1.0 + commandesActives);

        BigDecimal capacite = capaciteDisponible(livreur);
        double scoreCapacite;
        if (capacite == null || poidsTotal == null || capacite.compareTo(BigDecimal.ZERO) <= 0
                || poidsTotal.compareTo(BigDecimal.ZERO) <= 0) {
            scoreCapacite = 1.0;
        } else {
            scoreCapacite = 1.0 - Math.min(poidsTotal.doubleValue() / capacite.doubleValue(), 1.0);
        }

        double score = SCORE_MAX
                * (POIDS_PROXIMITE * scoreProximite
                + POIDS_CHARGE * scoreCharge
                + POIDS_CAPACITE * scoreCapacite)
                * multiplierPriorite(commande.getPriorite());

        return new LivreurScoreDto(
                livreur.getId(),
                livreur.getPrenom(),
                livreur.getNom(),
                livreur.getZoneActivite(),
                Math.min(score, SCORE_MAX),
                Math.round(scoreProximite * SCORE_MAX) / SCORE_MAX,
                Math.round(scoreCharge * SCORE_MAX) / SCORE_MAX,
                Math.round(scoreCapacite * SCORE_MAX) / SCORE_MAX,
                distanceKm,
                commandesActives);
    }

    private double distanceVersLivraison(Livreur livreur, Commande commande) {
        if (livreur.getLatitude() == null || livreur.getLongitude() == null
                || commande.getLatitudeLivraison() == null || commande.getLongitudeLivraison() == null) {
            return -1.0;
        }
        return geoService.distanceKm(livreur.getLatitude(), livreur.getLongitude(),
                commande.getLatitudeLivraison(), commande.getLongitudeLivraison());
    }

    private BigDecimal calculerPoidsTotal(Commande commande) {
        List<Colis> colis = commande.getColis();
        if (colis != null && !colis.isEmpty()) {
            return colis.stream()
                    .map(Colis::getPoids)
                    .filter(poids -> poids != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return commande.getPoids();
    }

    private BigDecimal capaciteDisponible(Livreur livreur) {
        if (livreur.getCapaciteMax() != null) {
            return livreur.getCapaciteMax();
        }
        if (livreur.getVehicule() != null && livreur.getVehicule().getCapacite() != null) {
            return livreur.getVehicule().getCapacite();
        }
        return null;
    }

    private double multiplierPriorite(PrioriteCommande priorite) {
        if (priorite == null) {
            return 1.0;
        }
        return switch (priorite) {
            case EXPRESS -> 1.1;
            case URGENTE -> 1.05;
            default -> 1.0;
        };
    }
}