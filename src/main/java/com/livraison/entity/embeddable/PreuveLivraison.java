package com.livraison.entity.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import java.util.Objects;

@Embeddable
public class PreuveLivraison {

    @Column(name = "preuve_date_heure")
    private LocalDateTime dateHeure;

    @Column(name = "preuve_nom_receptionnaire")
    private String nomReceptionnaire;

    @Column(name = "preuve_code_confirmation")
    private String codeConfirmation;

    @Column(name = "preuve_commentaire")
    private String commentaire;

    public PreuveLivraison() {
    }

    public PreuveLivraison(LocalDateTime dateHeure, String nomReceptionnaire, String codeConfirmation, String commentaire) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PreuveLivraison that = (PreuveLivraison) o;
        return Objects.equals(dateHeure, that.dateHeure)
                && Objects.equals(nomReceptionnaire, that.nomReceptionnaire)
                && Objects.equals(codeConfirmation, that.codeConfirmation)
                && Objects.equals(commentaire, that.commentaire);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dateHeure, nomReceptionnaire, codeConfirmation, commentaire);
    }

    @Override
    public String toString() {
        return "PreuveLivraison{"
                + "dateHeure=" + dateHeure
                + ", nomReceptionnaire='" + nomReceptionnaire + '\''
                + ", codeConfirmation='" + codeConfirmation + '\''
                + ", commentaire='" + commentaire + '\''
                + '}';
    }
}
