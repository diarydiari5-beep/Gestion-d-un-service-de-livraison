package com.livraison.config;

import com.livraison.entity.Livreur;
import com.livraison.entity.Utilisateur;
import com.livraison.entity.enums.RoleUtilisateur;
import com.livraison.repository.LivreurRepository;
import com.livraison.repository.UtilisateurRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cree un jeu de comptes par defaut au premier demarrage (table vide).
 *
 * Ces comptes sont destines a la demonstration : les mots de passe doivent
 * etre changes en production via l'ecran de gestion des utilisateurs.
 */
@Configuration
public class ComptesInitialesConfig {

    @Bean
    CommandLineRunner initialiserComptes(UtilisateurRepository utilisateurRepository,
                                         LivreurRepository livreurRepository,
                                         PasswordEncoder passwordEncoder) {
        return args -> {
            if (utilisateurRepository.count() > 0) {
                return;
            }
            utilisateurRepository.save(new Utilisateur("admin",
                    passwordEncoder.encode("admin123"), "Super", "Administrateur",
                    RoleUtilisateur.ADMIN, true));
            utilisateurRepository.save(new Utilisateur("dispatcher",
                    passwordEncoder.encode("dispatcher123"), "Rakoto", "Diana",
                    RoleUtilisateur.DISPATCHER, true));

            Utilisateur chauffeur = new Utilisateur("livreur",
                    passwordEncoder.encode("livreur123"), "Ratsimi", "Haja",
                    RoleUtilisateur.LIVREUR, true);
            Livreur livreurDemo = livreurRepository.findAll().stream().findFirst().orElse(null);
            chauffeur.setLivreur(livreurDemo);
            utilisateurRepository.save(chauffeur);
        };
    }
}
