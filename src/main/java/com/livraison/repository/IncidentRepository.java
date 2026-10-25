package com.livraison.repository;

import com.livraison.entity.Incident;
import com.livraison.entity.enums.StatutIncident;
import com.livraison.entity.enums.TypeIncident;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByCommandeId(Long commandeId);

    /** Suppression en cascade applicative : la commande ne peut pas disparaitre
     * en laissant des incidents orphelins. */
    void deleteByCommandeId(Long commandeId);

    List<Incident> findByCommandeIdOrderByDateDesc(Long commandeId);

    /** Incidents d'une commande qui demandent encore une action (affichage detail). */
    List<Incident> findByCommandeIdAndStatutInOrderByDateDesc(Long commandeId,
                                                             Collection<StatutIncident> statuts);

    List<Incident> findByLivreurId(Long livreurId);

    List<Incident> findByLivreurIdOrderByDateDesc(Long livreurId);

    List<Incident> findByType(TypeIncident type);

    List<Incident> findByDateBetween(LocalDateTime debut, LocalDateTime fin);

    Long countByType(TypeIncident type);

    /** Tous les incidents, du plus recent au plus ancien. */
    List<Incident> findAllByOrderByDateDesc();

    List<Incident> findByStatutOrderByDateDesc(StatutIncident statut);

    Long countByStatut(StatutIncident statut);

    List<Incident> findByDateBetweenOrderByDateDesc(LocalDateTime debut, LocalDateTime fin);

    /**
     * Identifiants des commandes portant au moins un incident ouvert.
     * Une seule requete alimente tout le tableau de bord de suivi.
     */
    @Query("select distinct i.commande.id from Incident i "
            + "where i.commande.id is not null and i.statut in :statuts")
    Set<Long> findCommandeIdsAvecIncidentsOuverts(@Param("statuts") Collection<StatutIncident> statuts);
}
