package com.livraison.dto;

import java.time.LocalDateTime;

/**
 * Vue applicative d'un incident : tout est renvoye en texte pour que la meme
 * forme serve la page web et l'API REST.
 */
public class IncidentDto {

    private Long id;
    private String type;
    private String typeLibelle;
    private String statut;
    private String statutLibelle;
    private String description;
    private String resolution;
    private LocalDateTime date;
    private String dateAffichee;
    private LocalDateTime dateResolution;
    private String dateResolutionAffichee;
    private String transitionProposee;
    private Long commandeId;
    private String commandeNumero;
    private String commandeStatut;
    private Long livreurId;
    private String livreurNom;

    public IncidentDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTypeLibelle() {
        return typeLibelle;
    }

    public void setTypeLibelle(String typeLibelle) {
        this.typeLibelle = typeLibelle;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getStatutLibelle() {
        return statutLibelle;
    }

    public void setStatutLibelle(String statutLibelle) {
        this.statutLibelle = statutLibelle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getDateAffichee() {
        return dateAffichee;
    }

    public void setDateAffichee(String dateAffichee) {
        this.dateAffichee = dateAffichee;
    }

    public LocalDateTime getDateResolution() {
        return dateResolution;
    }

    public void setDateResolution(LocalDateTime dateResolution) {
        this.dateResolution = dateResolution;
    }

    public String getDateResolutionAffichee() {
        return dateResolutionAffichee;
    }

    public void setDateResolutionAffichee(String dateResolutionAffichee) {
        this.dateResolutionAffichee = dateResolutionAffichee;
    }

    public String getTransitionProposee() {
        return transitionProposee;
    }

    public void setTransitionProposee(String transitionProposee) {
        this.transitionProposee = transitionProposee;
    }

    public Long getCommandeId() {
        return commandeId;
    }

    public void setCommandeId(Long commandeId) {
        this.commandeId = commandeId;
    }

    public String getCommandeNumero() {
        return commandeNumero;
    }

    public void setCommandeNumero(String commandeNumero) {
        this.commandeNumero = commandeNumero;
    }

    public String getCommandeStatut() {
        return commandeStatut;
    }

    public void setCommandeStatut(String commandeStatut) {
        this.commandeStatut = commandeStatut;
    }

    public Long getLivreurId() {
        return livreurId;
    }

    public void setLivreurId(Long livreurId) {
        this.livreurId = livreurId;
    }

    public String getLivreurNom() {
        return livreurNom;
    }

    public void setLivreurNom(String livreurNom) {
        this.livreurNom = livreurNom;
    }
}
