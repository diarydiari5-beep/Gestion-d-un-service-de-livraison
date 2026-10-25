package com.livraison.dto;

import java.time.LocalDateTime;

/**
 * Une entree du journal des statuts, exposee par l'API.
 * Le statut precedent est null sur la premiere entree (creation).
 */
public class StatutHistoriqueDto {

    private String statutPrecedent;
    private String statut;
    private LocalDateTime date;
    private String dateAffichee;
    private String origine;
    private String commentaire;

    public StatutHistoriqueDto() {
    }

    public StatutHistoriqueDto(String statut, LocalDateTime date) {
        this.statut = statut;
        this.date = date;
    }

    public StatutHistoriqueDto(String statutPrecedent, String statut, LocalDateTime date,
                               String dateAffichee, String origine, String commentaire) {
        this.statutPrecedent = statutPrecedent;
        this.statut = statut;
        this.date = date;
        this.dateAffichee = dateAffichee;
        this.origine = origine;
        this.commentaire = commentaire;
    }

    public String getStatutPrecedent() {
        return statutPrecedent;
    }

    public void setStatutPrecedent(String statutPrecedent) {
        this.statutPrecedent = statutPrecedent;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
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

    public String getOrigine() {
        return origine;
    }

    public void setOrigine(String origine) {
        this.origine = origine;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
}
