package com.livraison.service;

import com.livraison.dto.StatutHistoriqueDto;
import com.livraison.entity.Commande;
import com.livraison.entity.HistoriqueStatut;
import com.livraison.entity.enums.OrigineHistorique;
import com.livraison.entity.enums.StatutCommande;
import com.livraison.repository.HistoriqueStatutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class HistoriqueService {

    public static final String COMMENTAIRE_CREATION = "Creation de la commande";

    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRANCE);

    private final HistoriqueStatutRepository historiqueStatutRepository;

    public HistoriqueService(HistoriqueStatutRepository historiqueStatutRepository) {
        this.historiqueStatutRepository = historiqueStatutRepository;
    }
    public void enregistrer(Commande commande, StatutCommande precedent, StatutCommande nouveau,
                            OrigineHistorique origine, String commentaire) {
        if (commande == null || commande.getId() == null || nouveau == null) {
            return;
        }
        historiqueStatutRepository.save(new HistoriqueStatut(commande, precedent, nouveau,
                LocalDateTime.now(), origine, commentaire));
    }

    public List<HistoriqueStatut> historique(Long commandeId) {
        return historiqueStatutRepository.findByCommandeIdOrderByDateChangementAscIdAsc(commandeId);
    }

    @Transactional(readOnly = true)
    public List<StatutHistoriqueDto> historiqueDto(Long commandeId) {
        return historique(commandeId).stream()
                .map(entree -> new StatutHistoriqueDto(
                        nom(entree.getStatutPrecedent()),
                        nom(entree.getStatutNouveau()),
                        entree.getDateChangement(),
                        entree.getDateChangement() == null ? null
                                : FORMAT_DATE.format(entree.getDateChangement()),
                        entree.getOrigine() == null ? null : entree.getOrigine().name(),
                        entree.getCommentaire()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long nombreEntrees(Long commandeId) {
        return historiqueStatutRepository.countByCommandeId(commandeId);
    }

    /** Supprime le journal d'une commande ; appele lors de la suppression. */
    public void supprimer(Long commandeId) {
        historiqueStatutRepository.deleteByCommandeId(commandeId);
    }

    private String nom(StatutCommande statut) {
        return statut == null ? null : statut.name();
    }
}
