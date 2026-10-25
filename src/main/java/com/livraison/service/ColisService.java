package com.livraison.service;

import com.livraison.entity.Colis;
import com.livraison.entity.Commande;
import com.livraison.repository.ColisRepository;
import com.livraison.repository.CommandeRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ColisService {

    private final ColisRepository colisRepository;
    private final CommandeRepository commandeRepository;

    public ColisService(ColisRepository colisRepository, CommandeRepository commandeRepository) {
        this.colisRepository = colisRepository;
        this.commandeRepository = commandeRepository;
    }

    @Transactional(readOnly = true)
    public List<Colis> findAll() {
        return colisRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Colis> findById(Long id) {
        return colisRepository.findById(id);
    }

    public Colis save(Colis colis) {
        return colisRepository.save(colis);
    }

    public boolean deleteById(Long id) {
        if (!colisRepository.existsById(id)) {
            return false;
        }
        colisRepository.deleteById(id);
        return true;
    }

    @Transactional(readOnly = true)
    public List<Commande> findAllCommandes() {
        return commandeRepository.findAll();
    }
}