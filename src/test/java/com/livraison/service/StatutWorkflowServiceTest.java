package com.livraison.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.livraison.entity.enums.StatutCommande;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StatutWorkflowServiceTest {

    private final StatutWorkflowService workflow = new StatutWorkflowService();

    @Test
    void workflowCompletEstAutorise() {
        assertTrue(workflow.estAutorisee(StatutCommande.CREEE, StatutCommande.ASSIGNEE));
        assertTrue(workflow.estAutorisee(StatutCommande.ASSIGNEE, StatutCommande.PRISE_EN_CHARGE));
        assertTrue(workflow.estAutorisee(StatutCommande.PRISE_EN_CHARGE, StatutCommande.EN_TRANSIT));
        assertTrue(workflow.estAutorisee(StatutCommande.EN_TRANSIT, StatutCommande.LIVREE));
    }

    @Test
    void commandeLivreeNePeutPlusRevenirEnTransit() {
        assertFalse(workflow.estAutorisee(StatutCommande.LIVREE, StatutCommande.EN_TRANSIT));
        assertFalse(workflow.estAutorisee(StatutCommande.LIVREE, StatutCommande.PRISE_EN_CHARGE));
        assertFalse(workflow.estAutorisee(StatutCommande.LIVREE, StatutCommande.ASSIGNEE));
        assertFalse(workflow.estAutorisee(StatutCommande.LIVREE, StatutCommande.CREEE));
    }

    @Test
    void etatsTerminauxNAutorisentAucuneTransition() {
        assertTrue(workflow.transitionsPossibles(StatutCommande.LIVREE).isEmpty());
        assertTrue(workflow.transitionsPossibles(StatutCommande.ANNULEE).isEmpty());
        assertTrue(workflow.transitionsPossibles(StatutCommande.RETOURNEE).isEmpty());
        assertTrue(workflow.estTerminal(StatutCommande.LIVREE));
        assertTrue(workflow.estTerminal(StatutCommande.ANNULEE));
    }

    @Test
    void echecDeLivraisonPeutEtreRetourne() {
        assertTrue(workflow.estAutorisee(StatutCommande.ECHEC_LIVRAISON, StatutCommande.RETOURNEE));
        assertEquals(Set.of(StatutCommande.RETOURNEE),
                workflow.transitionsPossibles(StatutCommande.ECHEC_LIVRAISON));
    }

    @Test
    void onNeSautePasDeEtape() {
        assertFalse(workflow.estAutorisee(StatutCommande.CREEE, StatutCommande.EN_TRANSIT));
        assertFalse(workflow.estAutorisee(StatutCommande.CREEE, StatutCommande.LIVREE));
        assertFalse(workflow.estAutorisee(StatutCommande.ASSIGNEE, StatutCommande.LIVREE));
    }

    @Test
    void transitionsDepuisCreeeSontLimitees() {
        assertEquals(Set.of(StatutCommande.ASSIGNEE, StatutCommande.ANNULEE),
                workflow.transitionsPossibles(StatutCommande.CREEE));
    }

    @Test
    void transitionVersSoitMemeRefusee() {
        assertFalse(workflow.estAutorisee(StatutCommande.EN_TRANSIT, StatutCommande.EN_TRANSIT));
        assertFalse(workflow.estAutorisee(StatutCommande.ASSIGNEE, StatutCommande.ASSIGNEE));
    }

    @Test
    void valeursNullesRefusees() {
        assertFalse(workflow.estAutorisee(null, StatutCommande.ASSIGNEE));
        assertFalse(workflow.estAutorisee(StatutCommande.CREEE, null));
        assertTrue(workflow.transitionsPossibles(null).isEmpty());
    }
}
