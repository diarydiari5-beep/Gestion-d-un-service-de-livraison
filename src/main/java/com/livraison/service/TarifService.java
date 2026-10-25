package com.livraison.service;

import com.livraison.dto.PrixCalculeDto;
import com.livraison.entity.Commande;
import com.livraison.entity.Tarif;
import com.livraison.repository.TarifRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TarifService {

    private final TarifRepository tarifRepository;

    public TarifService(TarifRepository tarifRepository) {
        this.tarifRepository = tarifRepository;
    }

    @Transactional(readOnly = true)
    public List<Tarif> findAll() {
        return tarifRepository.findAllByOrderByZoneAsc();
    }

    /** Bareme retenu pour une zone : zone, sinon TOUTES, sinon valeurs par defaut. */
    @Transactional(readOnly = true)
    public Tarif tarifPourZone(String zone) {
        if (zone != null && !zone.isBlank()) {
            Tarif trouve = tarifRepository.findByZoneIgnoreCase(zone.trim()).orElse(null);
            if (trouve != null) {
                return trouve;
            }
        }
        return tarifParDefaut();
    }

    @Transactional(readOnly = true)
    public Tarif tarifParDefaut() {
        return tarifRepository.findByZoneIgnoreCase(Tarif.ZONE_PAR_DEFAUT)
                .orElseGet(Tarif::valeursParDefaut);
    }

    /** Detail du prix d'une commande d'apres le bareme de sa zone. */
    public PrixCalculeDto calculer(Commande commande) {
        if (commande == null) {
            throw new IllegalArgumentException("Commande inexistante");
        }
        return PrixCalculator.calculer(tarifPourZone(commande.getZoneLivraison()),
                commande.getDistance(), commande.getPoids(), commande.getVolume(),
                commande.getPriorite());
    }

    @Transactional
    public Tarif enregistrer(Tarif tarif) {
        if (tarif == null || tarif.getZone() == null || tarif.getZone().isBlank()) {
            throw new IllegalArgumentException("La zone du bareme est obligatoire");
        }
        tarif.setZone(tarif.getZone().trim());
        return tarifRepository.save(tarif);
    }

    /** Enregistre tous les baremes renvoyes par l'ecran d'edition en place. */
    @Transactional
    public List<Tarif> enregistrerTous(List<Tarif> tarifs) {
        return tarifs.stream().map(this::enregistrer).toList();
    }

    /** Indique si la table est vide : l'ecran d'administration le signale. */
    @Transactional(readOnly = true)
    public boolean aucunBaremeConfigure() {
        return tarifRepository.count() == 0;
    }
}