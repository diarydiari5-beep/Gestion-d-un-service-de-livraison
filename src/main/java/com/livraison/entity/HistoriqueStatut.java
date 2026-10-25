package com.livraison.entity;

import com.livraison.entity.enums.OrigineHistorique;
import com.livraison.entity.enums.StatutCommande;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Une ligne du journal des changements de statut d'une commande.
 *
 * Le journal est alimente dans CommandeService.changerStatut et dans
 * CommandeService.save : tout changement de statut passe par ces deux methodes,
 * donc aucune transition ne peut y echapper.
 */
@Entity
@Table(name = "historique_statuts",
        indexes = @Index(name = "idx_historique_commande", columnList = "commande_id"))
public class HistoriqueStatut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Commande commande;

    /** Statut avant le changement ; null pour la creation de la commande. */
    @Enumerated(EnumType.STRING)
    private StatutCommande statutPrecedent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCommande statutNouveau;

    @Column(nullable = false)
    private LocalDateTime dateChangement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigineHistorique origine;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    public HistoriqueStatut() {
    }

    public HistoriqueStatut(Commande commande, StatutCommande statutPrecedent,
                            StatutCommande statutNouveau, LocalDateTime dateChangement,
                            OrigineHistorique origine, String commentaire) {
        this.commande = commande;
        this.statutPrecedent = statutPrecedent;
        this.statutNouveau = statutNouveau;
        this.dateChangement = dateChangement;
        this.origine = origine;
        this.commentaire = commentaire;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Commande getCommande() {
        return commande;
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }

    public StatutCommande getStatutPrecedent() {
        return statutPrecedent;
    }

    public void setStatutPrecedent(StatutCommande statutPrecedent) {
        this.statutPrecedent = statutPrecedent;
    }

    public StatutCommande getStatutNouveau() {
        return statutNouveau;
    }

    public void setStatutNouveau(StatutCommande statutNouveau) {
        this.statutNouveau = statutNouveau;
    }

    public LocalDateTime getDateChangement() {
        return dateChangement;
    }

    public void setDateChangement(LocalDateTime dateChangement) {
        this.dateChangement = dateChangement;
    }

    public OrigineHistorique getOrigine() {
        return origine;
    }

    public void setOrigine(OrigineHistorique origine) {
        this.origine = origine;
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
        HistoriqueStatut autre = (HistoriqueStatut) o;
        return Objects.equals(id, autre.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "HistoriqueStatut{"
                + "id=" + id
                + ", statutPrecedent=" + statutPrecedent
                + ", statutNouveau=" + statutNouveau
                + ", dateChangement=" + dateChangement
                + ", origine=" + origine
                + '}';
    }
}
