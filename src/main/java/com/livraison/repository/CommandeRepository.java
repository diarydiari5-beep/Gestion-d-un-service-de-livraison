package com.livraison.repository;

import com.livraison.entity.Commande;
import com.livraison.entity.enums.PrioriteCommande;
import com.livraison.entity.enums.StatutCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    Optional<Commande> findByNumero(String numero);

    List<Commande> findByStatut(StatutCommande statut);

    List<Commande> findByStatutIn(List<StatutCommande> statuts);

    List<Commande> findByClientId(Long clientId);

    List<Commande> findByLivreurId(Long livreurId);

    List<Commande> findByLivreurIdAndStatut(Long livreurId, StatutCommande statut);

    List<Commande> findByStatutAndPriorite(StatutCommande statut, PrioriteCommande priorite);

    List<Commande> findByDatePrevueBetween(LocalDateTime debut, LocalDateTime fin);

    List<Commande> findByDateCreationBetween(LocalDateTime debut, LocalDateTime fin);

    Long countByStatut(StatutCommande statut);

    /**
     * Compte le nombre de commandes actives (statuts autres que CREEE/ANNULEE/ECHEC_LIVRAISON)
     * pour un livreur donné. Utilisé comme critère d'attribution automatique : le livreur
     * avec le moins de commandes actives est prioritaire.
     */
    Long countByLivreurIdAndStatutIn(Long livreurId, List<StatutCommande> statuts);

    /**
     * Récupère les commandes en attente d'affectation (statut CREEE), triées par priorité
     * décroissante puis par date prévue croissante. Utilisé pour la file d'attente
     * d'attribution automatique.
     */
    @Query("SELECT c FROM Commande c WHERE c.statut = 'CREEE' ORDER BY c.priorite DESC, c.datePrevue ASC")
    List<Commande> findCommandesEnAttenteAffectation();

    /**
     * Calcule le chiffre d'affaires (somme des prix) pour les commandes livrées
     * sur une période donnée.
     */
    @Query("SELECT COALESCE(SUM(c.prix), 0) FROM Commande c WHERE c.statut = 'LIVREE' AND c.dateMiseAJour BETWEEN :debut AND :fin")
    BigDecimal calculerChiffreAffairesPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /**
     * Calcule la distance totale parcourue pour les commandes livrées sur une période donnée.
     */
    @Query("SELECT COALESCE(SUM(c.distance), 0) FROM Commande c WHERE c.statut = 'LIVREE' AND c.dateMiseAJour BETWEEN :debut AND :fin")
    BigDecimal calculerDistanceTotalePeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
}
