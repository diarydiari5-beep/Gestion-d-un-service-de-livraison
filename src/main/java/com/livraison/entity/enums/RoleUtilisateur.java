package com.livraison.entity.enums;

/**
 * Roles fonctionnels de l'application.
 *
 * ADMIN      : acces complet, y compris les baremes et la gestion des comptes.
 * DISPATCHER : exploitation courante (commandes, clients, colis, livreurs,
 *              affectation, incidents, statistiques).
 * LIVREUR    : acces terrain (ses livraisons, suivi, incidents).
 */
public enum RoleUtilisateur {

    ADMIN("Administrateur"),
    DISPATCHER("Gestionnaire"),
    LIVREUR("Livreur");

    private final String libelle;

    RoleUtilisateur(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Nom de l'autorite Spring Security correspondante (prefixe ROLE_). */
    public String getAuthority() {
        return "ROLE_" + name();
    }
}
