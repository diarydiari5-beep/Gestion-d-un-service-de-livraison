package com.livraison.repository;

import com.livraison.entity.HistoriqueStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriqueStatutRepository extends JpaRepository<HistoriqueStatut, Long> {

    List<HistoriqueStatut> findByCommandeIdOrderByDateChangementAscIdAsc(Long commandeId);

    List<HistoriqueStatut> findByCommandeId(Long commandeId);

    long countByCommandeId(Long commandeId);

    void deleteByCommandeId(Long commandeId);
}
