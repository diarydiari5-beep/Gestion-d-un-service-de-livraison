package com.livraison.repository;

import com.livraison.entity.Colis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColisRepository extends JpaRepository<Colis, Long> {

    List<Colis> findByCommandeId(Long commandeId);
}
