package com.livraison.repository;

import com.livraison.entity.Tarif;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TarifRepository extends JpaRepository<Tarif, Long> {

    /** Recherche insensible a la casse : les zones saisies au clavier varient. */
    Optional<Tarif> findByZoneIgnoreCase(String zone);

    List<Tarif> findAllByOrderByZoneAsc();

    boolean existsByZoneIgnoreCase(String zone);
}