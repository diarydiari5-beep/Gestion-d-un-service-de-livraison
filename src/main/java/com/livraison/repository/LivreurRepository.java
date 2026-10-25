package com.livraison.repository;

import com.livraison.entity.Livreur;
import com.livraison.entity.enums.StatutLivreur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LivreurRepository extends JpaRepository<Livreur, Long> {

    List<Livreur> findByStatut(StatutLivreur statut);

    List<Livreur> findByZoneActivite(String zoneActivite);

    /**
     * Recherche les livreurs disponibles dans une zone donnée. Utilisé pour
     * l'attribution automatique : filtre par disponibilité + compatibilité de zone.
     */
    List<Livreur> findByStatutAndZoneActivite(StatutLivreur statut, String zoneActivite);

    /**
     * Recherche les livreurs disponibles dont la capacité maximale est supérieure
     * ou égale à un poids minimum. Utilisé pour l'attribution automatique :
     * vérifie que le véhicule du livreur peut transporter le colis.
     */
    List<Livreur> findByStatutAndCapaciteMaxGreaterThanEqual(StatutLivreur statut, BigDecimal capaciteMin);
}
