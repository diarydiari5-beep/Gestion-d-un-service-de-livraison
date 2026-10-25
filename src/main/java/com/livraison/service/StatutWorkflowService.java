package com.livraison.service;

import com.livraison.entity.enums.StatutCommande;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class StatutWorkflowService {

    private final Map<StatutCommande, Set<StatutCommande>> transitions = new EnumMap<>(StatutCommande.class);

    public StatutWorkflowService() {
        transitions.put(StatutCommande.CREEE, EnumSet.of(StatutCommande.ASSIGNEE, StatutCommande.ANNULEE));
        transitions.put(StatutCommande.ASSIGNEE, EnumSet.of(
                StatutCommande.PRISE_EN_CHARGE,
                StatutCommande.ANNULEE,
                StatutCommande.ECHEC_LIVRAISON));
        transitions.put(StatutCommande.PRISE_EN_CHARGE, EnumSet.of(
                StatutCommande.EN_TRANSIT,
                StatutCommande.ECHEC_LIVRAISON,
                StatutCommande.RETOURNEE));
        transitions.put(StatutCommande.EN_TRANSIT, EnumSet.of(
                StatutCommande.LIVREE,
                StatutCommande.ECHEC_LIVRAISON,
                StatutCommande.RETOURNEE));
        transitions.put(StatutCommande.LIVREE, EnumSet.noneOf(StatutCommande.class));
        transitions.put(StatutCommande.ANNULEE, EnumSet.noneOf(StatutCommande.class));
        transitions.put(StatutCommande.RETOURNEE, EnumSet.noneOf(StatutCommande.class));
        transitions.put(StatutCommande.ECHEC_LIVRAISON, EnumSet.of(StatutCommande.RETOURNEE));
    }

    public boolean estAutorisee(StatutCommande depuis, StatutCommande vers) {
        if (depuis == null || vers == null) {
            return false;
        }
        return transitions.getOrDefault(depuis, Set.of()).contains(vers);
    }

    public Set<StatutCommande> transitionsPossibles(StatutCommande depuis) {
        if (depuis == null) {
            return Set.of();
        }
        return Set.copyOf(transitions.getOrDefault(depuis, Set.of()));
    }

    public boolean estTerminal(StatutCommande statut) {
        return transitionsPossibles(statut).isEmpty();
    }
}
