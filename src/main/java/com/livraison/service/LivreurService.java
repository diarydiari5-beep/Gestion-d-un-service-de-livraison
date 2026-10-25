package com.livraison.service;

import com.livraison.entity.Livreur;
import com.livraison.entity.enums.StatutLivreur;
import com.livraison.repository.CommandeRepository;
import com.livraison.repository.IncidentRepository;
import com.livraison.repository.LivreurRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LivreurService {

    private final LivreurRepository livreurRepository;
    private final CommandeRepository commandeRepository;
    private final IncidentRepository incidentRepository;

    public LivreurService(LivreurRepository livreurRepository,
                          CommandeRepository commandeRepository,
                          IncidentRepository incidentRepository) {
        this.livreurRepository = livreurRepository;
        this.commandeRepository = commandeRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public List<Livreur> findAll() {
        return livreurRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Livreur> findById(Long id) {
        return livreurRepository.findById(id);
    }

    public Livreur save(Livreur livreur) {
        return livreurRepository.save(livreur);
    }

    public boolean deleteById(Long id) {
        if (!livreurRepository.existsById(id)
                || !commandeRepository.findByLivreurId(id).isEmpty()
                || !incidentRepository.findByLivreurId(id).isEmpty()) {
            return false;
        }
        livreurRepository.deleteById(id);
        return true;
    }

    public StatutLivreur[] getAllStatuts() {
        return StatutLivreur.values();
    }
}