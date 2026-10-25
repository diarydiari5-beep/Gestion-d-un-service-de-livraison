package com.livraison.service;

import com.livraison.entity.Utilisateur;
import com.livraison.repository.UtilisateurRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UtilisateurService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurService(UtilisateurRepository utilisateurRepository,
                              PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur " + username + " introuvable"));
        return User.withUsername(utilisateur.getUsername())
                .password(utilisateur.getPassword())
                .authorities(new SimpleGrantedAuthority(utilisateur.getRole().getAuthority()))
                .disabled(!utilisateur.isActif())
                .build();
    }

    @Transactional(readOnly = true)
    public List<Utilisateur> findAll() {
        return utilisateurRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Utilisateur> findById(Long id) {
        return utilisateurRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Utilisateur> findByUsername(String username) {
        return utilisateurRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean usernameExiste(String username) {
        return username != null && utilisateurRepository.existsByUsername(username);
    }

    
    public Utilisateur save(Utilisateur utilisateur) {
        if (utilisateur.getId() != null) {
            Utilisateur existant = utilisateurRepository.findById(utilisateur.getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Utilisateur " + utilisateur.getId() + " introuvable"));
            if (utilisateur.getPassword() == null || utilisateur.getPassword().isBlank()) {
                utilisateur.setPassword(existant.getPassword());
            } else {
                utilisateur.setPassword(passwordEncoder.encode(utilisateur.getPassword()));
            }
            if (utilisateur.getLivreur() == null) {
                utilisateur.setLivreur(existant.getLivreur());
            }
        } else {
            utilisateur.setPassword(passwordEncoder.encode(utilisateur.getPassword()));
        }
        return utilisateurRepository.save(utilisateur);
    }

    public void deleteById(Long id) {
        utilisateurRepository.deleteById(id);
    }
}
