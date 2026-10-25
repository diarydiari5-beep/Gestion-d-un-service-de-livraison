package com.livraison.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.livraison.dto.AlerteSuivi;
import com.livraison.dto.RouteDto;
import com.livraison.dto.SuiviLigneDto;
import com.livraison.entity.Client;
import com.livraison.entity.Commande;
import com.livraison.entity.Livreur;
import com.livraison.entity.enums.StatutCommande;
import com.livraison.entity.enums.StatutLivreur;
import com.livraison.repository.IncidentRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SuiviServiceTest {

    private CommandeService commandeService;
    private RouteService routeService;
    private IncidentRepository incidentRepository;
    private SuiviService suiviService;

    @BeforeEach
    void setUp() {
        commandeService = mock(CommandeService.class);
        routeService = mock(RouteService.class);
        incidentRepository = mock(IncidentRepository.class);
        suiviService = new SuiviService(commandeService, routeService, new GeoService(),
                incidentRepository);
    }

    private Commande commande(StatutCommande statut, Double latLivreur, Double lonLivreur) {
        Client client = new Client();
        client.setPrenom("Rakoto");
        client.setNom("Rasoanaivo");

        Livreur livreur = new Livreur();
        livreur.setId(7L);
        livreur.setPrenom("Mamy");
        livreur.setNom("Randriamampionona");
        livreur.setStatut(StatutLivreur.EN_LIVRAISON);
        livreur.setLatitude(latLivreur);
        livreur.setLongitude(lonLivreur);

        Commande commande = new Commande();
        commande.setId(1L);
        commande.setNumero("DEMO-CMD-001");
        commande.setClient(client);
        commande.setZoneLivraison("isoraka");
        commande.setStatut(statut);
        commande.setLivreur(livreur);
        // Enlevement Analakely (-18.910 / 47.525), livraison (-18.900 / 47.532)
        commande.setLatitudeEnlevement(-18.910);
        commande.setLongitudeEnlevement(47.525);
        commande.setLatitudeLivraison(-18.900);
        commande.setLongitudeLivraison(47.532);
        return commande;
    }

    private void itineraire(double distanceKm, double minutes, boolean approximatif) {
        List<double[]> points = List.of(new double[]{-18.910, 47.525}, new double[]{-18.900, 47.532});
        when(routeService.calculerRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new RouteDto(points, distanceKm, minutes, approximatif));
    }

    @Test
    void distanceRestanteVientDeLItineraireRoutier() {
        itineraire(2.4, 7.0, false);
        SuiviLigneDto ligne = suiviService.construire(commande(StatutCommande.EN_TRANSIT, -18.908, 47.520), Map.of());

        assertEquals(2.4, ligne.getDistanceKm(), 1e-9);
        assertEquals(7.0, ligne.getDureeMinutes(), 1e-9);
        assertTrue(ligne.isDistanceRoutiere());
        assertEquals(SuiviService.LIBELLE_ROUTIER, ligne.getLibelleDistance());
        assertTrue(ligne.getDistanceAffichee().startsWith("2,40"), ligne.getDistanceAffichee());
    }

    @Test
    void repliLigneDroiteEstSignaleCommeEstimation() {
        itineraire(2.0, 4.8, true);
        SuiviLigneDto ligne = suiviService.construire(commande(StatutCommande.EN_TRANSIT, -18.908, 47.520), Map.of());

        assertFalse(ligne.isDistanceRoutiere());
        assertEquals(SuiviService.LIBELLE_ESTIMATION, ligne.getLibelleDistance());
    }

    @Test
    void phaseCollectePourUneCommandeAssignee() {
        itineraire(1.2, 3.0, false);
        SuiviLigneDto ligne = suiviService.construire(commande(StatutCommande.ASSIGNEE, -18.915, 47.530), Map.of());

        assertEquals("Collecte", ligne.getPhase());
        assertEquals("Enlèvement", ligne.getCibleLibelle());
        assertNull(ligne.getAvancement(), "Pas d'avancement tant que le colis n'est pas charge");
    }

    @Test
    void phaseEnTransitViseLaLivraison() {
        itineraire(1.2, 3.0, false);
        SuiviLigneDto ligne = suiviService.construire(commande(StatutCommande.EN_TRANSIT, -18.915, 47.530), Map.of());

        assertEquals("En transit", ligne.getPhase());
        assertEquals("Livraison", ligne.getCibleLibelle());
    }

    @Test
    void avancementEstBorneEntreZeroEtCent() {
        itineraire(0.0, 0.0, false);
        SuiviLigneDto arrivee = suiviService.construire(commande(StatutCommande.EN_TRANSIT, -18.900, 47.532), Map.of());
        assertEquals(100, arrivee.getAvancement());

        itineraire(50.0, 90.0, false);
        SuiviLigneDto loin = suiviService.construire(commande(StatutCommande.EN_TRANSIT, -19.500, 47.100), Map.of());
        assertEquals(0, loin.getAvancement(), "Une distance superieure au trajet donne 0 %, jamais negatif");
    }

    @Test
    void avancementSuitLaPositionSurLeTraceEtPasUnRatioDeDistances() {
        // Trace routier en V : le livreur est le sommet du parcours, donc
        // l'avancement doit suivre sa position sur le trace.
        List<double[]> trace = List.of(
                new double[]{-18.9100, 47.5250},
                new double[]{-18.9150, 47.5300},
                new double[]{-18.9000, 47.5320});
        when(routeService.calculerRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new RouteDto(trace, 2.44, 6.0, false));

        SuiviLigneDto ligne = suiviService.construire(commande(StatutCommande.EN_TRANSIT, -18.9150, 47.5300),
                Map.of());

        int avancement = ligne.getAvancement();
        assertTrue(avancement >= 28 && avancement <= 34,
                "Le livreur est a ~31 % du trace, attendu 28-34, obtenu " + avancement);
    }

    @Test
    void avancementResteNulSiLeLivreurEstHorsDuTrace() {
        // Livreur tres eloigne de l'itineraire : la projection tombe sur le
        // point de depart, l'avancement ne doit pas depasser 100 %.
        List<double[]> trace = List.of(
                new double[]{-18.9100, 47.5250},
                new double[]{-18.9000, 47.5320});
        when(routeService.calculerRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new RouteDto(trace, 1.6, 4.0, false));

        SuiviLigneDto ligne = suiviService.construire(commande(StatutCommande.EN_TRANSIT, -19.5000, 47.1000),
                Map.of());

        assertEquals(0, ligne.getAvancement());
    }

    @Test
    void alerteRetardQuandLaDatePrevueEstDepassee() {
        itineraire(2.4, 7.0, false);
        Commande commande = commande(StatutCommande.EN_TRANSIT, -18.908, 47.520);
        commande.setDatePrevue(java.time.LocalDateTime.now().minusHours(2));

        SuiviLigneDto ligne = suiviService.construire(commande, Map.of());

        assertTrue(ligne.getAlertes().contains(AlerteSuivi.RETARD));
    }

    @Test
    void pasDAlerteRetardQuandLaDatePrevueEstFuture() {
        itineraire(2.4, 7.0, false);
        Commande commande = commande(StatutCommande.EN_TRANSIT, -18.908, 47.520);
        commande.setDatePrevue(java.time.LocalDateTime.now().plusHours(2));

        SuiviLigneDto ligne = suiviService.construire(commande, Map.of());

        assertFalse(ligne.getAlertes().contains(AlerteSuivi.RETARD));
    }

    @Test
    void alerteArriveeProcheSousTroisCentsMetres() {
        itineraire(0.1, 0.5, false);
        SuiviLigneDto ligne = suiviService.construire(commande(StatutCommande.EN_TRANSIT, -18.9001, 47.5321), Map.of());

        assertTrue(ligne.getAlertes().contains(AlerteSuivi.PRES_ARRIVEE));
    }

    @Test
    void positionInconnueRendLaDistanceIndisponible() {
        itineraire(2.4, 7.0, false);
        SuiviLigneDto ligne = suiviService.construire(commande(StatutCommande.EN_TRANSIT, null, null), Map.of());

        assertTrue(ligne.getAlertes().contains(AlerteSuivi.POSITION_INCONNUE));
        assertNull(ligne.getDistanceKm());
        assertEquals("—", ligne.getDistanceAffichee());
    }

    @Test
    void commandesTrieesParDatePrevuePuisParNumero() {
        itineraire(2.4, 7.0, false);
        Commande tardive = commande(StatutCommande.EN_TRANSIT, -18.908, 47.520);
        tardive.setNumero("DEMO-CMD-009");
        tardive.setDatePrevue(java.time.LocalDateTime.now().plusDays(2));
        Commande urgente = commande(StatutCommande.ASSIGNEE, -18.908, 47.520);
        urgente.setNumero("DEMO-CMD-002");
        urgente.setDatePrevue(java.time.LocalDateTime.now().plusHours(1));
        Commande sansDate = commande(StatutCommande.ASSIGNEE, -18.908, 47.520);
        sansDate.setNumero("DEMO-CMD-001");

        when(commandeService.findGeolocalisees()).thenReturn(List.of(tardive, sansDate, urgente));
        List<SuiviLigneDto> lignes = suiviService.rafraichir();

        assertEquals(List.of("DEMO-CMD-002", "DEMO-CMD-009", "DEMO-CMD-001"),
                lignes.stream().map(SuiviLigneDto::getNumero).toList());
    }

    @Test
    void nombreDeCommandesActivesParLivreurEstRenseigne() {
        itineraire(2.4, 7.0, false);
        SuiviLigneDto ligne = suiviService.construire(commande(StatutCommande.EN_TRANSIT, -18.908, 47.520),
                Map.of(7L, 3));

        assertEquals(3, ligne.getCommandesActives());
        assertEquals("En livraison", ligne.getLivreurStatut());
    }

    @Test
    void leCacheEviteUnRecalculAChaqueAppel() {
        itineraire(2.4, 7.0, false);
        when(commandeService.findGeolocalisees()).thenReturn(List.of(commande(StatutCommande.EN_TRANSIT, -18.908, 47.520)));

        suiviService.lignes();
        suiviService.lignes();
        suiviService.lignes();

        org.mockito.Mockito.verify(commandeService, org.mockito.Mockito.times(1)).findGeolocalisees();
    }
}
