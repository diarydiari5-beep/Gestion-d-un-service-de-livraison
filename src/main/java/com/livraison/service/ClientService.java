package com.livraison.service;

import com.livraison.entity.Client;
import com.livraison.repository.ClientRepository;
import com.livraison.repository.CommandeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final CommandeRepository commandeRepository;

    public ClientService(ClientRepository clientRepository, CommandeRepository commandeRepository) {
        this.clientRepository = clientRepository;
        this.commandeRepository = commandeRepository;
    }

    @Transactional(readOnly = true)
    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Client> findById(Long id) {
        return clientRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Client> findByEmail(String email) {
        return clientRepository.findByEmail(email);
    }

    public Client save(Client client) {
        return clientRepository.save(client);
    }

    public boolean deleteById(Long id) {
        if (!clientRepository.existsById(id)) {
            return false;
        }
        if (!commandeRepository.findByClientId(id).isEmpty()) {
            return false; // Le client a des commandes, suppression impossible
        }
        clientRepository.deleteById(id);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return clientRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmailAndIdNot(String email, Long id) {
        return clientRepository.findByEmail(email)
                .map(client -> !client.getId().equals(id))
                .orElse(false);
    }
}
