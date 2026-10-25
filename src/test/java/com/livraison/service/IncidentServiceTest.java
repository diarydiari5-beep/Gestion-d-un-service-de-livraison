package com.livraison.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.livraison.dto.IncidentDto;
import com.livraison.entity.Commande;
import com.livraison.entity.Incident;
import com.livraison.entity.Livreur;
import com.livraison.entity.enums.OrigineHistorique;
import com.livraison.entity.enums.StatutCommande;
import com.livraison.entity.enums.StatutIncident;
import com.livraison.entity.enums.TypeIncident;
import com.livraison.exception.TransitionInvalideException;
import com.livraison.repository.CommandeRepository;
import com.livraison.repository.IncidentRepository;
import com.livraison.repository.LivreurRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IncidentServiceTest {

    private IncidentRepository incidentRepository;
    private CommandeRepository commandeRepository;
    private LivreurRepository livreurRepository;
    private CommandeService commandeService;
    private IncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentRepository = mock(IncidentRepository.class);
        commandeRepository = mock(CommandeRepository.class);
        livreurRepository = mock(LivreurRepository.class);
        commandeService = mock(CommandeService.class);
        incidentService = new IncidentService(incidentRepository, commandeRepository,
                livreurRepository, commandeService, new StatutWorkflowService());
    }

    private Commande commande(Long id, StatutCommande statut) {
        Commande commande = new Commande();
        commande.setId(id);
        commande.setStatut(statut);
        commande.setNumero("CMD-" + id);
        when(commandeRepository.findById(id)).thenReturn(Optional.of(commande));
        return commande;
    }

    private Livreur livreur(Long id) {
        Livreur livreur = new Livreur();
        livreur.setId(id);
        livreur.setPrenom("Naina");
        livreur.setNom("Rakotoarisoa");
        when(livreurRepository.findById(id)).thenReturn(Optional.of(livreur));
        return livreur;
    }

    private Incident incidentExistant(Long id, StatutIncident statut, Commande commande) {
        Incident incident = new Incident(TypeIncident.CLIENT_ABSENT,
                java.time.LocalDateTime.now(), "description", commande,
                commande == null ? null : commande.getLivreur());
        incident.setId(id);
        incident.setStatut(statut);
        when(incidentRepository.findById(id)).thenReturn(Optional.of(incident));
        return incident;
    }

    // ------------------------------------------------------------------ signalement

    @Test
    void signalerCreeUnIncidentOuvertSurLaCommande() {
        Commande commande = commande(1L, StatutCommande.EN_TRANSIT);
        Livreur affecte = livreur(7L);
        commande.setLivreur(affecte);

        incidentService.signaler(TypeIncident.ADRESSE_INCORRECTE, "  adresse introuvable  ",
                1L, null);

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        Incident cree = captor.getValue();
        assertEquals(TypeIncident.ADRESSE_INCORRECTE, cree.getType());
        assertEquals("adresse introuvable", cree.getDescription());
        assertEquals(StatutIncident.SIGNALEE, cree.getStatut());
        assertEquals(commande, cree.getCommande());
        assertEquals(affecte, cree.getLivreur());
    }

    @Test
    void signalerRenseigneLaDateDuSignalement() {
        commande(1L, StatutCommande.CREEE);
        incidentService.signaler(TypeIncident.REFUS_CLIENT, "refus", 1L, null);

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        assertFalse(captor.getValue().getDate().isAfter(java.time.LocalDateTime.now()));
    }

    @Test
    void signalerExigeUnType() {
        assertThrows(IllegalArgumentException.class,
                () -> incidentService.signaler(null, "description", 1L, null));
    }

    @Test
    void signalerExigeUneDescription() {
        assertThrows(IllegalArgumentException.class,
                () -> incidentService.signaler(TypeIncident.REFUS_CLIENT, "   ", 1L, null));
    }

    @Test
    void signalerExigeUneCommandeOuUnLivreur() {
        assertThrows(IllegalArgumentException.class,
                () -> incidentService.signaler(TypeIncident.REFUS_CLIENT, "refus", null, null));
    }

    @Test
    void signalerAccepteUnLivreurSansCommande() {
        Livreur livreur = livreur(7L);
        incidentService.signaler(TypeIncident.VEHICULE_EN_PANNE, "panne moteur", null, 7L);

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        assertNull(captor.getValue().getCommande());
        assertEquals(livreur, captor.getValue().getLivreur());
    }

    @Test
    void signalerRefuseUnLivreurEtrangerALaCommande() {
        Commande commande = commande(1L, StatutCommande.EN_TRANSIT);
        commande.setLivreur(livreur(7L));
        livreur(8L);

        assertThrows(IllegalArgumentException.class,
                () -> incidentService.signaler(TypeIncident.REFUS_CLIENT, "refus", 1L, 8L));
    }

    @Test
    void signalerRefuseUneCommandeInconnue() {
        when(commandeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> incidentService.signaler(TypeIncident.REFUS_CLIENT, "refus", 99L, null));
    }

    // ------------------------------------------------------------------- traitement

    @Test
    void traiterPasseLIncidentEnTraitement() {
        Incident incident = incidentExistant(1L, StatutIncident.SIGNALEE,
                commande(1L, StatutCommande.EN_TRANSIT));
        incidentService.traiter(1L);
        assertEquals(StatutIncident.EN_TRAITEMENT, incident.getStatut());
        verify(incidentRepository).save(incident);
    }

    @Test
    void traiterRefuseUnIncidentDejaCloture() {
        incidentExistant(1L, StatutIncident.RESOLU, commande(1L, StatutCommande.LIVREE));
        assertThrows(TransitionInvalideException.class, () -> incidentService.traiter(1L));
    }

    // ------------------------------------------------------------------- resolution

    @Test
    void resoudreSansTransitionLaisseLeWorkflowIntact() {
        Incident incident = incidentExistant(1L, StatutIncident.EN_TRAITEMENT,
                commande(1L, StatutCommande.EN_TRANSIT));

        incidentService.resoudre(1L, "colis remis", null);

        assertEquals(StatutIncident.RESOLU, incident.getStatut());
        assertEquals("colis remis", incident.getResolution());
        assertNull(incident.getTransitionProposee());
        assertFalse(incident.getDateResolution() == null);
        verify(commandeService, never()).changerStatut(any(), any(), any(), any());
    }

    @Test
    void resoudreAppliqueLaTransitionProposeeAvecLOfficeIncident() {
        Incident incident = incidentExistant(1L, StatutIncident.EN_TRAITEMENT,
                commande(1L, StatutCommande.EN_TRANSIT));

        incidentService.resoudre(1L, "client absent, retour impossible", StatutCommande.RETOURNEE);

        verify(commandeService).changerStatut(1L, StatutCommande.RETOURNEE, null,
                OrigineHistorique.INCIDENT);
        assertEquals(StatutIncident.RESOLU, incident.getStatut());
        assertEquals(StatutCommande.RETOURNEE, incident.getTransitionProposee());
    }

    @Test
    void resoudreRefuseDeValiderLaLivraison() {
        incidentExistant(1L, StatutIncident.EN_TRAITEMENT, commande(1L, StatutCommande.EN_TRANSIT));

        assertThrows(TransitionInvalideException.class,
                () -> incidentService.resoudre(1L, "pretend-on livrer", StatutCommande.LIVREE));

        verify(commandeService, never()).changerStatut(any(), any(), any(), any());
    }

    @Test
    void resoudreNeCloturePasSiLaTransitionEstRefusee() {
        // La commande est deja terminee : le workflow refuse toute transition.
        Incident incident = incidentExistant(1L, StatutIncident.EN_TRAITEMENT,
                commande(1L, StatutCommande.RETOURNEE));
        doThrow(new TransitionInvalideException("Transition interdite : RETOURNEE -> ECHEC_LIVRAISON"))
                .when(commandeService)
                .changerStatut(eq(1L), eq(StatutCommande.ECHEC_LIVRAISON), any(), any());

        assertThrows(TransitionInvalideException.class,
                () -> incidentService.resoudre(1L, "tentative tardive",
                        StatutCommande.ECHEC_LIVRAISON));

        assertEquals(StatutIncident.EN_TRAITEMENT, incident.getStatut());
        assertNull(incident.getResolution());
        assertNull(incident.getDateResolution());
    }

    @Test
    void resoudreRefuseUneTransitionSurIncidentSansCommande() {
        Incident incident = incidentExistant(1L, StatutIncident.EN_TRAITEMENT, null);
        assertThrows(TransitionInvalideException.class,
                () -> incidentService.resoudre(1L, "panne", StatutCommande.RETOURNEE));
        assertEquals(StatutIncident.EN_TRAITEMENT, incident.getStatut());
    }

    @Test
    void resoudreRefuseUnIncidentDejaCloture() {
        incidentExistant(1L, StatutIncident.REJETE, commande(1L, StatutCommande.CREEE));
        assertThrows(TransitionInvalideException.class,
                () -> incidentService.resoudre(1L, "trop tard", null));
    }

    @Test
    void resoudreExigeUneResolution() {
        incidentExistant(1L, StatutIncident.EN_TRAITEMENT, commande(1L, StatutCommande.CREEE));
        assertThrows(IllegalArgumentException.class,
                () -> incidentService.resoudre(1L, "  ", null));
    }

    @Test
    void resoudreRefuseUnIncidentInconnu() {
        when(incidentRepository.findById(42L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> incidentService.resoudre(42L, "resolution", null));
    }

    // ----------------------------------------------------------------------- rejet

    @Test
    void rejeterClotureSansTransition() {
        Incident incident = incidentExistant(1L, StatutIncident.EN_TRAITEMENT,
                commande(1L, StatutCommande.EN_TRANSIT));

        incidentService.rejeter(1L, "information erronee");

        assertEquals(StatutIncident.REJETE, incident.getStatut());
        assertEquals("information erronee", incident.getResolution());
        assertNull(incident.getTransitionProposee());
        verify(commandeService, never()).changerStatut(any(), any(), any(), any());
    }

    @Test
    void rejeterExigeUnMotif() {
        incidentExistant(1L, StatutIncident.SIGNALEE, commande(1L, StatutCommande.CREEE));
        assertThrows(IllegalArgumentException.class, () -> incidentService.rejeter(1L, " "));
    }

    @Test
    void supprimerSupprimeLIncident() {
        incidentExistant(1L, StatutIncident.RESOLU, null);
        incidentService.supprimer(1L);
        verify(incidentRepository).deleteById(1L);
    }

    // -------------------------------------------------------------------- lectures

    @Test
    void transitionsProposablesExcluentLaLivraison() {
        incidentExistant(1L, StatutIncident.EN_TRAITEMENT,
                commande(1L, StatutCommande.EN_TRANSIT));

        List<StatutCommande> transitions = incidentService.transitionsProposables(1L);

        assertTrue(transitions.contains(StatutCommande.ECHEC_LIVRAISON));
        assertTrue(transitions.contains(StatutCommande.RETOURNEE));
        assertFalse(transitions.contains(StatutCommande.LIVREE));
    }

    @Test
    void transitionsProposablesVidesSansCommande() {
        incidentExistant(1L, StatutIncident.EN_TRAITEMENT, null);
        assertTrue(incidentService.transitionsProposables(1L).isEmpty());
    }

    @Test
    void nombreOuvertsCompteLesStatutsNonClotures() {
        when(incidentRepository.countByStatut(StatutIncident.SIGNALEE)).thenReturn(2L);
        when(incidentRepository.countByStatut(StatutIncident.EN_TRAITEMENT)).thenReturn(1L);
        assertEquals(3L, incidentService.nombreOuverts());
    }

    @Test
    void compteursParStatutCouvrentTousLesStatuts() {
        when(incidentRepository.countByStatut(any())).thenReturn(0L);
        Map<String, Long> compteurs = incidentService.compteursParStatut();
        assertEquals(IncidentService.TOUS_STATUTS.size(), compteurs.size());
        assertTrue(compteurs.containsKey("SIGNALEE"));
        assertTrue(compteurs.containsKey("REJETE"));
    }

    @Test
    void compteursParTypeCouvrentTousLesTypes() {
        when(incidentRepository.countByType(any())).thenReturn(0L);
        Map<String, Long> compteurs = incidentService.compteursParType();
        assertEquals(TypeIncident.values().length, compteurs.size());
    }

    @Test
    void incidentsOuvertsDUneCommandeSontLimitesAuxStatutsOuverts() {
        when(incidentRepository.findByCommandeIdAndStatutInOrderByDateDesc(eq(1L), any()))
                .thenReturn(List.of());
        incidentService.findOuvertsParCommande(1L);
        verify(incidentRepository).findByCommandeIdAndStatutInOrderByDateDesc(eq(1L),
                eq(List.of(StatutIncident.SIGNALEE, StatutIncident.EN_TRAITEMENT)));
    }

    @Test
    void dtoExposeLibellesEtDatesFormatees() {
        Commande commande = commande(1L, StatutCommande.ECHEC_LIVRAISON);
        commande.setLivreur(livreur(7L));
        Incident incident = incidentExistant(1L, StatutIncident.RESOLU, commande);
        incident.setTransitionProposee(StatutCommande.ECHEC_LIVRAISON);
        incident.setResolution("colis abime, retour au depot");

        IncidentDto dto = incidentService.toDto(incident);

        assertEquals(1L, dto.getId());
        assertEquals("CLIENT_ABSENT", dto.getType());
        assertEquals("Client absent", dto.getTypeLibelle());
        assertEquals("RESOLU", dto.getStatut());
        assertEquals("Resolu", dto.getStatutLibelle());
        assertEquals("CMD-1", dto.getCommandeNumero());
        assertEquals("ECHEC_LIVRAISON", dto.getCommandeStatut());
        assertEquals("ECHEC_LIVRAISON", dto.getTransitionProposee());
        assertEquals("Naina Rakotoarisoa", dto.getLivreurNom());
        assertTrue(dto.getDateAffichee().matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}"));
    }

    @Test
    void libellesCouvrentToutesLesValeursDesEnums() {
        for (TypeIncident type : TypeIncident.values()) {
            assertFalse(IncidentService.libelleType(type).isBlank());
        }
        for (StatutIncident statut : StatutIncident.values()) {
            assertFalse(IncidentService.libelleStatut(statut).isBlank());
        }
    }

    @Test
    void statutsOuvertsSontCeuxNonClotures() {
        assertEquals(List.of(StatutIncident.SIGNALEE, StatutIncident.EN_TRAITEMENT),
                List.copyOf(IncidentService.statutsOuverts()));
        assertTrue(StatutIncident.SIGNALEE.estOuvert());
        assertTrue(StatutIncident.EN_TRAITEMENT.estOuvert());
        assertTrue(StatutIncident.RESOLU.estCloture());
        assertTrue(StatutIncident.REJETE.estCloture());
    }
}