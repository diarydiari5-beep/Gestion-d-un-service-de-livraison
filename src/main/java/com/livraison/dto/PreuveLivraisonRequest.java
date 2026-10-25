package com.livraison.dto;

import java.time.LocalDateTime;

public class PreuveLivraisonRequest {

    private LocalDateTime dateHeure;
    private String nomReceptionnaire;
    private String codeConfirmation;
    private String commentaire;

    public PreuveLivraisonRequest() {
    }

    public PreuveLivraisonRequest(LocalDateTime dateHeure, String nomReceptionnaire,
                                  String codeConfirmation, String commentaire) {
        this.dateHeure = dateHeure;
        this.nomReceptionnaire = nomReceptionnaire;
        this.codeConfirmation = codeConfirmation;
        this.commentaire = commentaire;
    }

    public LocalDateTime getDateHeure() {
        return dateHeure;
    }

    public void setDateHeure(LocalDateTime dateHeure) {
        this.dateHeure = dateHeure;
    }

    public String getNomReceptionnaire() {
        return nomReceptionnaire;
    }

    public void setNomReceptionnaire(String nomReceptionnaire) {
        this.nomReceptionnaire = nomReceptionnaire;
    }

    public String getCodeConfirmation() {
        return codeConfirmation;
    }

    public void setCodeConfirmation(String codeConfirmation) {
        this.codeConfirmation = codeConfirmation;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
}
