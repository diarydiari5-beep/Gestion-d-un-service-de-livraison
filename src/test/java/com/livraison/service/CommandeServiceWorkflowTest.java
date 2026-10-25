package com.livraison.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.livraison.dto.LivreurScoreDto;
import com.livraison.dto.PrixCalculeDto;
import com.livraison.entity.Commande;
import com.livraison.entity.Livreur;
import com.livraison.entity.embeddable.PreuveLivraison;
import com.livraison.entity.enums.OrigineHistorique;
import com.livraison.entity.enums.PrioriteCommande;
import com.livraison.entity.enums.StatutCommande;
import com.livraison.entity.enums.StatutLivreur;
import com.livraison.exception.PreuveLivraisonIncompleteException;
import com.livraison.exception.TransitionInvalideException;
import com.livraison.repository.ClientRepository;
import com.livraison.repository.CommandeRepository;
import com.livraison.repository.IncidentRepository;
import com.livraison.repository.LivreurRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class CommandeServiceWorkflowTest {

    private CommandeRepository commandeRepository;
    private IncidentRepository incidentRepository;
    private LivreurService livreurService;
    private HistoriqueService historiqueService;
    private TarifService tarifService;
    private AttributionService attributionService;
    private CommandeService commandeService;

    @BeforeEach
    void setUp() {
        commandeRepository = mock(CommandeRepository.class);
        incidentRepository = mock(IncidentRepository.class);
        livreurService = mock(LivreurService.class);
        historiqueService = mock(HistoriqueService.class);
        tarifService = mock(TarifService.class);
        attributionService = mock(AttributionService.class);
        // Bareme par defaut : une commande sans prix fourni doit toujours
        // recevoir un montant, meme si le test ne porte pas sur la tarification.
        PrixCalculeDto tarifParDefaut = new PrixCalculeDto();
        tarifParDefaut.setZone("TOUTES");
        tarifParDefaut.setPrix(BigDecimal.ZERO);
        when(tarifService.calculer(any(Commande.class))).thenReturn(tarifParDefaut);
        commandeService = new CommandeService(
                commandeRepository,
                mock(ClientRepository.class),
                mock(LivreurRepository.class),
                incidentRepository,
                new StatutWorkflowService(),
                livreurService,
                historiqueService,
                tarifService,
                attributionService);
    }

    private Commande commandeAvecStatut(StatutCommande statut) {
        Commande commande = new Commande();
        commande.setId(1L);
        commande.setStatut(statut);
        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commande));
        when(commandeRepository.save(any(Commande.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return commande;
    }

    @Test
    void transitionInterditeEstRefusee() {
        commandeAvecStatut(StatutCommande.LIVREE);
        assertThrows(TransitionInvalideException.class,
                () -> commandeService.changerStatut(1L, StatutCommande.EN_TRANSIT));
        verify(commandeRepository, never()).save(any(Commande.class));
    }

    @Test
    void transitionAutoriseeEstAppliquee() {
        Commande commande = commandeAvecStatut(StatutCommande.CREEE);
        commandeService.changerStatut(1L, StatutCommande.ANNULEE);
        assertEquals(StatutCommande.ANNULEE, commande.getStatut());
    }

    @Test
    void livraisonExigeUnePreuveComplete() {
        commandeAvecStatut(StatutCommande.EN_TRANSIT);
        assertThrows(PreuveLivraisonIncompleteException.class,
                () -> commandeService.changerStatut(1L, StatutCommande.LIVREE));
    }

    @Test
    void preuveSansCodeConfirmationEstRefusee() {
        commandeAvecStatut(StatutCommande.EN_TRANSIT);
        PreuveLivraison preuve = new PreuveLivraison(LocalDateTime.now(), "Rakoto", "  ", null);
        assertThrows(PreuveLivraisonIncompleteException.class,
                () -> commandeService.changerStatut(1L, StatutCommande.LIVREE, preuve));
    }

    @Test
    void livraisonAvecPreuveCompleteEstAcceptee() {
        Commande commande = commandeAvecStatut(StatutCommande.EN_TRANSIT);
        PreuveLivraison preuve = new PreuveLivraison(LocalDateTime.now(), "Rakoto", "CONF-123", "recu");
        commandeService.changerStatut(1L, StatutCommande.LIVREE, preuve);
        assertEquals(StatutCommande.LIVREE, commande.getStatut());
        assertEquals("CONF-123", commande.getPreuveLivraison().getCodeConfirmation());
    }

    @Test
    void priseEnChargeMetLeLivreurEnLivraison() {
        Commande commande = commandeAvecStatut(StatutCommande.ASSIGNEE);
        Livreur livreur = new Livreur();
        livreur.setId(7L);
        livreur.setStatut(StatutLivreur.DISPONIBLE);
        commande.setLivreur(livreur);
        commandeService.changerStatut(1L, StatutCommande.PRISE_EN_CHARGE);
        assertEquals(StatutLivreur.EN_LIVRAISON, livreur.getStatut());
        verify(livreurService).save(livreur);
    }

    @Test
    void livreurRevientDisponibleSiPlusAucuneCommandeActive() {
        Commande commande = commandeAvecStatut(StatutCommande.EN_TRANSIT);
        Livreur livreur = new Livreur();
        livreur.setId(7L);
        livreur.setStatut(StatutLivreur.EN_LIVRAISON);
        commande.setLivreur(livreur);
        when(commandeRepository.countByLivreurIdAndStatutIn(any(), anyList())).thenReturn(0L);
        PreuveLivraison preuve = new PreuveLivraison(LocalDateTime.now(), "Rakoto", "CONF-123", null);
        commandeService.changerStatut(1L, StatutCommande.LIVREE, preuve);
        assertEquals(StatutLivreur.DISPONIBLE, livreur.getStatut());
    }

    @Test
    void livreurResteEnLivraisonSiCommandeActive() {
        Commande commande = commandeAvecStatut(StatutCommande.EN_TRANSIT);
        Livreur livreur = new Livreur();
        livreur.setId(7L);
        livreur.setStatut(StatutLivreur.EN_LIVRAISON);
        commande.setLivreur(livreur);
        when(commandeRepository.countByLivreurIdAndStatutIn(any(), anyList())).thenReturn(1L);
        PreuveLivraison preuve = new PreuveLivraison(LocalDateTime.now(), "Rakoto", "CONF-123", null);
        commandeService.changerStatut(1L, StatutCommande.LIVREE, preuve);
        assertEquals(StatutLivreur.EN_LIVRAISON, livreur.getStatut());
        verify(livreurService, never()).save(any());
    }

    @Test
    void affectationAffecteLeLivreurEtPasseEnAssignee() {
        Commande commande = commandeAvecStatut(StatutCommande.CREEE);
        Livreur livreur = new Livreur();
        livreur.setId(9L);
        commandeService.assigner(1L, livreur);
        assertEquals(StatutCommande.ASSIGNEE, commande.getStatut());
        assertEquals(9L, commande.getLivreur().getId());
    }

    @Test
    void findEnAttenteRenvoieLaRequeteExistante() {
        Commande attente = new Commande();
        attente.setId(3L);
        attente.setStatut(StatutCommande.CREEE);
        when(commandeRepository.findCommandesEnAttenteAffectation()).thenReturn(List.of(attente));

        assertEquals(List.of(attente), commandeService.findEnAttente());
    }

    @Test
    void findAffectablesExclutLesCommandesDejaAffectees() {
        Commande sansLivreur = new Commande();
        sansLivreur.setId(1L);
        Commande avecLivreur = new Commande();
        avecLivreur.setId(2L);
        avecLivreur.setLivreur(new Livreur());
        when(commandeRepository.findCommandesEnAttenteAffectation())
                .thenReturn(List.of(sansLivreur, avecLivreur));

        assertEquals(List.of(sansLivreur), commandeService.findAffectables());
    }

    @Test
    void affecterMeilleurLivreurAffecteLeMeilleurEtPasseEnAssignee() {
        Commande commande = commandeAvecStatut(StatutCommande.CREEE);
        Livreur meilleur = new Livreur();
        meilleur.setId(7L);
        meilleur.setPrenom("Haja");
        meilleur.setNom("Ratsimihandrina");
        when(attributionService.trouverMeilleurLivreur(commande)).thenReturn(Optional.of(
                new LivreurScoreDto(7L, "Haja", "Ratsimihandrina", "mahamasina",
                        98.0, 0.9, 1.0, 0.8, 0.5, 0L)));
        when(livreurService.findById(7L)).thenReturn(Optional.of(meilleur));

        Optional<Livreur> resultat = commandeService.affecterMeilleurLivreur(1L);

        assertTrue(resultat.isPresent());
        assertEquals(7L, resultat.get().getId());
        assertEquals(StatutCommande.ASSIGNEE, commande.getStatut());
        assertEquals(meilleur, commande.getLivreur());
    }

    @Test
    void affecterMeilleurLivreurSansCandidatNeChangePasLaCommande() {
        Commande commande = commandeAvecStatut(StatutCommande.CREEE);
        when(attributionService.trouverMeilleurLivreur(commande)).thenReturn(Optional.empty());

        Optional<Livreur> resultat = commandeService.affecterMeilleurLivreur(1L);

        assertTrue(resultat.isEmpty());
        assertEquals(StatutCommande.CREEE, commande.getStatut());
    }

    @Test
    void passageEnAssigneeRefuseSansLivreur() {
        commandeAvecStatut(StatutCommande.CREEE);
        assertThrows(TransitionInvalideException.class,
                () -> commandeService.changerStatut(1L, StatutCommande.ASSIGNEE));
    }

    @Test
    void lePrixEstCalculeQuandIlEstVide() {
        Commande commande = new Commande();
        commande.setStatut(StatutCommande.CREEE);
        commande.setZoneLivraison("ivato");
        commande.setDistance(new BigDecimal("2.00"));
        commande.setPoids(new BigDecimal("3.00"));
        commande.setVolume(new BigDecimal("0.10"));
        commande.setPriorite(PrioriteCommande.URGENTE);
        PrixCalculeDto detail = new PrixCalculeDto();
        detail.setZone("ivato");
        detail.setPrix(new BigDecimal("27000.00"));
        when(tarifService.calculer(commande)).thenReturn(detail);
        when(commandeRepository.save(commande)).thenReturn(commande);

        commandeService.save(commande, OrigineHistorique.API);

        assertEquals(0, new BigDecimal("27000.00").compareTo(commande.getPrix()));
    }

    @Test
    void unPrixFourniEstConserve() {
        Commande commande = new Commande();
        commande.setStatut(StatutCommande.CREEE);
        commande.setPrix(new BigDecimal("42000.00"));
        when(commandeRepository.save(commande)).thenReturn(commande);

        commandeService.save(commande, OrigineHistorique.API);

        assertEquals(0, new BigDecimal("42000.00").compareTo(commande.getPrix()));
        verify(tarifService, never()).calculer(any());
    }

    @Test
    void recalculerEcraseLePrixAvecLeDetailDuBareme() {
        Commande commande = new Commande();
        commande.setId(1L);
        commande.setStatut(StatutCommande.CREEE);
        commande.setPrix(new BigDecimal("42000.00"));
        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commande));
        when(commandeRepository.save(commande)).thenReturn(commande);
        PrixCalculeDto detail = new PrixCalculeDto();
        detail.setZone("ivato");
        detail.setPrix(new BigDecimal("31000.00"));
        when(tarifService.calculer(commande)).thenReturn(detail);

        PrixCalculeDto obtenu = commandeService.recalculerPrix(1L);

        assertEquals(0, new BigDecimal("31000.00").compareTo(obtenu.getPrix()));
        assertEquals(0, new BigDecimal("31000.00").compareTo(commande.getPrix()));
    }

    @Test
    void creationEnregistreUneEntreeDHistoriqueApresLeSave() {
        Commande commande = new Commande();
        commande.setStatut(StatutCommande.CREEE);
        Commande enregistree = mock(Commande.class);
        when(enregistree.getId()).thenReturn(5L);
        when(enregistree.getStatut()).thenReturn(StatutCommande.CREEE);
        when(commandeRepository.save(commande)).thenReturn(enregistree);

        assertEquals(enregistree, commandeService.save(commande, OrigineHistorique.API));

        verify(historiqueService).enregistrer(enregistree, null, StatutCommande.CREEE,
                OrigineHistorique.API, HistoriqueService.COMMENTAIRE_CREATION);
    }

    @Test
    void creationParLeWebUtiliseLEOrigineWeb() {
        Commande commande = new Commande();
        commande.setStatut(StatutCommande.CREEE);
        Commande enregistree = mock(Commande.class);
        when(enregistree.getId()).thenReturn(5L);
        when(enregistree.getStatut()).thenReturn(StatutCommande.CREEE);
        when(commandeRepository.save(commande)).thenReturn(enregistree);

        commandeService.save(commande);

        verify(historiqueService).enregistrer(enregistree, null, StatutCommande.CREEE,
                OrigineHistorique.WEB, HistoriqueService.COMMENTAIRE_CREATION);
    }

    @Test
    void modificationNEnregistrePasDEntreeDeCreation() {
        Commande commande = commandeAvecStatut(StatutCommande.CREEE);
        commandeService.save(commande);
        verify(historiqueService, never()).enregistrer(any(), any(), any(), any(), any());
    }

    @Test
    void transitionAccepteeEnregistreLEvenement() {
        Commande commande = commandeAvecStatut(StatutCommande.CREEE);
        commandeService.changerStatut(1L, StatutCommande.ANNULEE);
        verify(historiqueService).enregistrer(commande, StatutCommande.CREEE,
                StatutCommande.ANNULEE, OrigineHistorique.WEB, null);
    }

    @Test
    void transitionApiConserveLEOrigine() {
        Commande commande = commandeAvecStatut(StatutCommande.CREEE);
        commandeService.changerStatut(1L, StatutCommande.ANNULEE, null, OrigineHistorique.API);
        verify(historiqueService).enregistrer(commande, StatutCommande.CREEE,
                StatutCommande.ANNULEE, OrigineHistorique.API, null);
    }

    @Test
    void transitionRefuseeNEnregistreAucunEvenement() {
        commandeAvecStatut(StatutCommande.LIVREE);
        assertThrows(TransitionInvalideException.class,
                () -> commandeService.changerStatut(1L, StatutCommande.EN_TRANSIT));
        verify(historiqueService, never()).enregistrer(any(), any(), any(), any(), any());
    }

    @Test
    void affectationEnregistreUneOrigineAffectation() {
        Commande commande = commandeAvecStatut(StatutCommande.CREEE);
        Livreur livreur = new Livreur();
        livreur.setId(9L);
        commandeService.assigner(1L, livreur);
        verify(historiqueService).enregistrer(commande, StatutCommande.CREEE,
                StatutCommande.ASSIGNEE, OrigineHistorique.AFFECTATION, null);
    }

    @Test
    void suppressionEmporteLHistorique() {
        commandeService.deleteById(1L);
        verify(historiqueService).supprimer(1L);
        verify(commandeRepository).deleteById(1L);
    }

    @Test
    void suppressionEmporteLesIncidentsDeLaCommande() {
        commandeService.deleteById(1L);

        InOrder ordre = inOrder(incidentRepository, historiqueService, commandeRepository);
        ordre.verify(incidentRepository).deleteByCommandeId(1L);
        ordre.verify(historiqueService).supprimer(1L);
        ordre.verify(commandeRepository).deleteById(1L);
    }
}
