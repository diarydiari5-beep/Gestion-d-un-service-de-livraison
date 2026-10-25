package com.livraison.dto;

public enum AlerteSuivi {

    RETARD("En retard", "bg-danger"),
    NON_ASSIGNEE("Non assignée", "bg-danger"),
    SANS_LIVREUR("Aucun livreur", "bg-danger"),
    POSITION_INCONNUE("Position inconnue", "bg-secondary"),
    CIBLE_INCONNUE("Cible non géolocalisée", "bg-secondary"),
    INCIDENT("Incident signalé", "bg-warning"),
    PRES_ARRIVEE("Arrivée proche", "bg-success");

    private final String libelle;
    private final String couleur;

    AlerteSuivi(String libelle, String couleur) {
        this.libelle = libelle;
        this.couleur = couleur;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getCouleur() {
        return couleur;
    }
}
