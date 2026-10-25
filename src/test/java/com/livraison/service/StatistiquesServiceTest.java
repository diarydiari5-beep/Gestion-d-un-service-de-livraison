package com.livraison.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.livraison.dto.StatistiqueLivreurDto;
import com.livraison.dto.StatistiquesDto;
import com.livraison.entity.Commande;
import com.livraison.entity.Livreur;
import com.livraison.entity.embeddable.PreuveLivraison;
import com.livraison.entity.enums.StatutCommande;
import com.livraison.repository.CommandeRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Agregats statistiques : delai moyen, repartition par livreur, filtres. */
class StatistiquesServiceTest {

    private CommandeRepository commandeRepository;
    private StatistiquesService statistiquesService;

    @BeforeEach
    void setUp() {
        commandeRepository = mock(CommandeRepository.class);
        statistiquesService = new StatistiquesService(commandeRepository);
    }

    private static Livreur livreur(long id, String prenom, String nom) {
        Livreur livreur = new Livreur();
        livreur.setId(id);
        livreur.setPrenom(prenom);
        livreur.setNom(nom);
        return livreur;
    }

    private static Commande commande(StatutCommande statut, String zone, String prix,
                                     String distance, Livreur livreur,
                                     LocalDateTime creation, LocalDateTime livraison) {
        Commande commande = new Commande();
        commande.setStatut(statut);
        commande.setZoneLivraison(zone);
        commande.setPrix(prix == null ? null : new BigDecimal(prix));
        commande.setDistance(distance == null ? null : new BigDecimal(distance));
        commande.setLivreur(livreur);
        commande.setDateCreation(creation);
        if (livraison != null) {
            commande.setDateMiseAJour(livraison);
        }
        return commande;
    }

    @Test
    void delaiMoyenUtiliseLaPreuvePuisLaMiseAJour() {
        Livreur rakoto = livreur(1L, "Jean", "Rakoto");
        Commande c1 = commande(StatutCommande.LIVREE, "ivato", "100", "10", rakoto,
                LocalDateTime.of(2026, 1, 1, 8, 0), LocalDateTime.of(2026, 1, 1, 14, 0));
        c1.setPreuveLivraison(new PreuveLivraison(LocalDateTime.of(2026, 1, 1, 12, 0),
                "Client", "1234", "ok"));
        Commande c2 = commande(StatutCommande.LIVREE, "ivato", "200", "20", rakoto,
                LocalDateTime.of(2026, 1, 1, 9, 0), LocalDateTime.of(2026, 1, 1, 15, 0));

        when(commandeRepository.findAll()).thenReturn(List.of(c1, c2));

        StatistiquesDto stats = statistiquesService.calculer(null, null, null, null);

        // c1 : 08:00 -> 12:00 = 4 h (preuve) ; c2 : 09:00 -> 15:00 = 6 h (mise a jour)
        assertEquals(0, new BigDecimal("5.00").compareTo(stats.getDelaiMoyenHeures()));
    }

    @Test
    void repartitionParLivreurTrieParLivraisons() {
        Livreur rakoto = livreur(1L, "Jean", "Rakoto");
        Livreur bema = livreur(2L, "Bema", "Randria");
        LocalDateTime creation = LocalDateTime.of(2026, 1, 1, 8, 0);

        List<Commande> commandes = List.of(
                commande(StatutCommande.LIVREE, "ivato", "100", "10", rakoto, creation, creation.plusHours(1)),
                commande(StatutCommande.LIVREE, "ivato", "200", "20", rakoto, creation, creation.plusHours(1)),
                commande(StatutCommande.ECHEC_LIVRAISON, "ivato", "50", "5", rakoto, creation, creation.plusHours(1)),
                commande(StatutCommande.LIVREE, "isoraka", "400", "40", bema, creation, creation.plusHours(1)));
        when(commandeRepository.findAll()).thenReturn(commandes);

        StatistiquesDto stats = statistiquesService.calculer(null, null, null, null);

        assertEquals(2, stats.getParLivreur().size());
        StatistiqueLivreurDto premier = stats.getParLivreur().get(0);
        assertEquals(1L, premier.getLivreurId());
        assertEquals("Jean Rakoto", premier.getNom());
        assertEquals(2, premier.getLivraisons());
        assertEquals(0, new BigDecimal("300").compareTo(premier.getChiffreAffaires()));
        assertEquals(0, new BigDecimal("30").compareTo(premier.getDistanceTotaleKm()));
        assertEquals(0, new BigDecimal("66.67").compareTo(premier.getTauxReussite()));

        StatistiqueLivreurDto second = stats.getParLivreur().get(1);
        assertEquals(2L, second.getLivreurId());
        assertEquals(1, second.getLivraisons());
        assertEquals(0, new BigDecimal("100.00").compareTo(second.getTauxReussite()));
    }

    @Test
    void filtreZoneNeGardeQueLaZoneDemandee() {
        Livreur rakoto = livreur(1L, "Jean", "Rakoto");
        LocalDateTime creation = LocalDateTime.of(2026, 1, 1, 8, 0);
        List<Commande> commandes = List.of(
                commande(StatutCommande.LIVREE, "ivato", "100", "10", rakoto, creation, creation.plusHours(1)),
                commande(StatutCommande.LIVREE, "isoraka", "200", "20", rakoto, creation, creation.plusHours(1)));
        when(commandeRepository.findAll()).thenReturn(commandes);

        StatistiquesDto stats = statistiquesService.calculer(null, null, null, "IVATO");

        assertEquals(1, stats.getNombreLivraisons());
        assertEquals(0, new BigDecimal("100").compareTo(stats.getChiffreAffaires()));
    }

    @Test
    void periodeVideRetourneDesZeros() {
        when(commandeRepository.findAll()).thenReturn(List.of());

        StatistiquesDto stats = statistiquesService.calculer(null, null, null, null);

        assertEquals(0, stats.getNombreLivraisons());
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getChiffreAffaires()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getDistanceTotaleKm()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getDelaiMoyenHeures()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getRevenuMoyen()));
        assertTrue(stats.getParLivreur().isEmpty());
    }

    @Test
    void filtreLivreurDelegueAuRepository() {
        Livreur rakoto = livreur(1L, "Jean", "Rakoto");
        LocalDateTime creation = LocalDateTime.of(2026, 1, 1, 8, 0);
        when(commandeRepository.findByLivreurId(1L)).thenReturn(List.of(
                commande(StatutCommande.LIVREE, "ivato", "100", "10", rakoto, creation, creation.plusHours(1))));

        StatistiquesDto stats = statistiquesService.calculer(null, null, 1L, null);

        assertEquals(1, stats.getNombreLivraisons());
        assertEquals(1, stats.getParLivreur().size());
    }
}
