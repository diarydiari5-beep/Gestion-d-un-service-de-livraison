package com.livraison.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.livraison.dto.LivreurScoreDto;
import com.livraison.entity.Commande;
import com.livraison.entity.Livreur;
import com.livraison.entity.embeddable.Vehicule;
import com.livraison.entity.enums.PrioriteCommande;
import com.livraison.entity.enums.StatutLivreur;
import com.livraison.repository.CommandeRepository;
import com.livraison.repository.LivreurRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttributionServiceTest {

    private static final double LAT_LIVRAISON = 48.8566;
    private static final double LNG_LIVRAISON = 2.3522;

    @Mock
    private LivreurRepository livreurRepository;

    @Mock
    private CommandeRepository commandeRepository;

    private AttributionService attributionService;

    @BeforeEach
    void setUp() {
        attributionService = new AttributionService(
                livreurRepository, commandeRepository, new GeoService());
    }

    @Test
    void meilleurLivreurEstLePlusProche() {
        Livreur proche = livreur(1L, "Centre", StatutLivreur.DISPONIBLE,
                BigDecimal.valueOf(100), 48.8746, 2.3522);
        Livreur lointain = livreur(2L, "Centre", StatutLivreur.DISPONIBLE,
                BigDecimal.valueOf(100), 48.9466, 2.3522);
        when(livreurRepository.findAll()).thenReturn(List.of(proche, lointain));

        List<LivreurScoreDto> classement = attributionService.classerLivreurs(commande("Centre", "10.0"));

        assertThat(classement).extracting(LivreurScoreDto::getLivreurId)
                .containsExactly(1L, 2L);
        assertThat(classement.get(0).getScore()).isGreaterThan(classement.get(1).getScore());
    }

    @Test
    void meilleurLivreurEstLaTeteDuClassement() {
        Livreur proche = livreur(1L, "Centre", StatutLivreur.DISPONIBLE,
                BigDecimal.valueOf(100), 48.8746, 2.3522);
        Livreur lointain = livreur(2L, "Centre", StatutLivreur.DISPONIBLE,
                BigDecimal.valueOf(100), 48.9466, 2.3522);
        when(livreurRepository.findAll()).thenReturn(List.of(proche, lointain));

        Commande commande = commande("Centre", "10.0");
        long meilleur = attributionService.trouverMeilleurLivreur(commande).orElseThrow().getLivreurId();
        long tete = attributionService.classerLivreurs(commande).get(0).getLivreurId();

        assertThat(meilleur).isEqualTo(tete);
    }

    @Test
    void indisponibleExclu() {
        Livreur disponible = livreur(1L, "Centre", StatutLivreur.DISPONIBLE,
                BigDecimal.valueOf(100), 48.8746, 2.3522);
        Livreur occupe = livreur(2L, "Centre", StatutLivreur.EN_LIVRAISON,
                BigDecimal.valueOf(100), LAT_LIVRAISON, LNG_LIVRAISON);
        when(livreurRepository.findAll()).thenReturn(List.of(disponible, occupe));

        List<LivreurScoreDto> classement = attributionService.classerLivreurs(commande("Centre", "10.0"));

        assertThat(classement).extracting(LivreurScoreDto::getLivreurId)
                .containsExactly(1L);
    }

    @Test
    void zoneIncompatibleExclue() {
        Livreur mauvaiseZone = livreur(1L, "Centre", StatutLivreur.DISPONIBLE,
                BigDecimal.valueOf(100), 48.8746, 2.3522);
        Livreur bonneZone = livreur(2L, "Nord", StatutLivreur.DISPONIBLE,
                BigDecimal.valueOf(100), 48.9466, 2.3522);
        when(livreurRepository.findAll()).thenReturn(List.of(mauvaiseZone, bonneZone));

        List<LivreurScoreDto> classement = attributionService.classerLivreurs(commande("Nord", "10.0"));

        assertThat(classement).extracting(LivreurScoreDto::getLivreurId)
                .containsExactly(2L);
    }

    @Test
    void capaciteInssuffisanteExclue() {
        Livreur petitVehicule = livreur(1L, "Centre", StatutLivreur.DISPONIBLE,
                BigDecimal.valueOf(50), 48.8746, 2.3522);
        Livreur bonVehicule = livreur(2L, "Centre", StatutLivreur.DISPONIBLE,
                BigDecimal.valueOf(200), 48.9466, 2.3522);
        when(livreurRepository.findAll()).thenReturn(List.of(petitVehicule, bonVehicule));

        List<LivreurScoreDto> classement = attributionService.classerLivreurs(commande("Centre", "100.0"));

        assertThat(classement).extracting(LivreurScoreDto::getLivreurId)
                .containsExactly(2L);
    }

    @Test
    void sansCibleAvecZoneVideTousZonesAcceptees() {
        Livreur l1 = livreur(1L, "Centre", StatutLivreur.DISPONIBLE,
                BigDecimal.valueOf(100), 48.8746, 2.3522);
        when(livreurRepository.findAll()).thenReturn(List.of(l1));

        List<LivreurScoreDto> classement = attributionService.classerLivreurs(commande(null, "10.0"));

        assertThat(classement).hasSize(1);
    }

    @Test
    void commandeExpressMarqueeAvantNormale() {
        Livreur l1 = livreur(1L, "Centre", StatutLivreur.DISPONIBLE,
                BigDecimal.valueOf(100), 48.8746, 2.3522);
        when(livreurRepository.findAll()).thenReturn(List.of(l1));

        double scoreNormale = attributionService.classerLivreurs(commande("Centre", "10.0"))
                .get(0).getScore();
        Commande express = commande("Centre", "10.0");
        express.setPriorite(PrioriteCommande.EXPRESS);
        double scoreExpress = attributionService.classerLivreurs(express).get(0).getScore();

        assertThat(scoreExpress).isGreaterThan(scoreNormale);
    }

    private Livreur livreur(Long id, String zone, StatutLivreur statut, BigDecimal capaciteMax,
                            double latitude, double longitude) {
        Livreur livreur = new Livreur(id, "Nom", "Prenom", null,
                new Vehicule("moto", BigDecimal.valueOf(150), "AB-123-CD"),
                zone, statut, capaciteMax);
        livreur.setLatitude(latitude);
        livreur.setLongitude(longitude);
        return livreur;
    }

    private Commande commande(String zone, String poids) {
        Commande commande = new Commande();
        commande.setZoneLivraison(zone);
        commande.setPoids(new BigDecimal(poids));
        commande.setLatitudeLivraison(LAT_LIVRAISON);
        commande.setLongitudeLivraison(LNG_LIVRAISON);
        return commande;
    }
}