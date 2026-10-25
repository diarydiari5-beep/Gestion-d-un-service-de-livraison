package com.livraison.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.livraison.dto.StatutHistoriqueDto;
import com.livraison.entity.Commande;
import com.livraison.entity.HistoriqueStatut;
import com.livraison.entity.enums.OrigineHistorique;
import com.livraison.entity.enums.StatutCommande;
import com.livraison.repository.HistoriqueStatutRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class HistoriqueServiceTest {

    private HistoriqueStatutRepository repository;
    private HistoriqueService service;

    @BeforeEach
    void setUp() {
        repository = mock(HistoriqueStatutRepository.class);
        service = new HistoriqueService(repository);
    }

    private Commande commande() {
        Commande commande = new Commande();
        commande.setId(3L);
        return commande;
    }

    @Test
    void enregistrerPersisteLEvenement() {
        service.enregistrer(commande(), StatutCommande.CREEE, StatutCommande.ASSIGNEE,
                OrigineHistorique.AFFECTATION, "Affectation");

        ArgumentCaptor<HistoriqueStatut> captor = ArgumentCaptor.forClass(HistoriqueStatut.class);
        verify(repository).save(captor.capture());
        HistoriqueStatut entree = captor.getValue();
        assertEquals(3L, entree.getCommande().getId());
        assertEquals(StatutCommande.CREEE, entree.getStatutPrecedent());
        assertEquals(StatutCommande.ASSIGNEE, entree.getStatutNouveau());
        assertEquals(OrigineHistorique.AFFECTATION, entree.getOrigine());
        assertEquals("Affectation", entree.getCommentaire());
    }

    @Test
    void enregistrerIgnoreUneCommandeNonPersistee() {
        Commande sansId = new Commande();
        service.enregistrer(sansId, StatutCommande.CREEE, StatutCommande.ASSIGNEE,
                OrigineHistorique.WEB, null);
        service.enregistrer(null, StatutCommande.CREEE, StatutCommande.ASSIGNEE,
                OrigineHistorique.WEB, null);
        service.enregistrer(commande(), StatutCommande.CREEE, null, OrigineHistorique.WEB, null);
        verify(repository, never()).save(any(HistoriqueStatut.class));
    }

    @Test
    void historiqueDtoEstOrdonneEtFormate() {
        LocalDateTime date = LocalDateTime.of(2026, 3, 7, 14, 5);
        when(repository.findByCommandeIdOrderByDateChangementAscIdAsc(3L)).thenReturn(List.of(
                new HistoriqueStatut(commande(), null, StatutCommande.CREEE, date,
                        OrigineHistorique.WEB, HistoriqueService.COMMENTAIRE_CREATION),
                new HistoriqueStatut(commande(), StatutCommande.CREEE, StatutCommande.LIVREE,
                        date.plusHours(2), OrigineHistorique.API, null)));

        List<StatutHistoriqueDto> entrees = service.historiqueDto(3L);

        assertEquals(2, entrees.size());
        StatutHistoriqueDto creation = entrees.get(0);
        assertNull(creation.getStatutPrecedent());
        assertEquals("CREEE", creation.getStatut());
        assertEquals("WEB", creation.getOrigine());
        assertEquals("07/03/2026 14:05", creation.getDateAffichee());
        StatutHistoriqueDto livraison = entrees.get(1);
        assertEquals("CREEE", livraison.getStatutPrecedent());
        assertEquals("LIVREE", livraison.getStatut());
        assertEquals("API", livraison.getOrigine());
        assertEquals("07/03/2026 16:05", livraison.getDateAffichee());
    }

    @Test
    void historiqueDtoTolereUneDateNulle() {
        HistoriqueStatut entree = new HistoriqueStatut(commande(), StatutCommande.CREEE,
                StatutCommande.ANNULEE, null, OrigineHistorique.WEB, null);
        when(repository.findByCommandeIdOrderByDateChangementAscIdAsc(3L)).thenReturn(List.of(entree));

        List<StatutHistoriqueDto> entrees = service.historiqueDto(3L);

        assertEquals(1, entrees.size());
        assertNull(entrees.get(0).getDate());
        assertNull(entrees.get(0).getDateAffichee());
    }

    @Test
    void supprimerDelegueAuDepot() {
        service.supprimer(3L);
        verify(repository).deleteByCommandeId(3L);
    }

    @Test
    void nombreEntreesCompteParCommande() {
        when(repository.countByCommandeId(3L)).thenReturn(4L);
        assertEquals(4L, service.nombreEntrees(3L));
    }
}
