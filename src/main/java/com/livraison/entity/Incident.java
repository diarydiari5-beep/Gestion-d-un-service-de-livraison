package com.livraison.entity;

import com.livraison.entity.enums.StatutCommande;
import com.livraison.entity.enums.StatutIncident;
import com.livraison.entity.enums.TypeIncident;
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
 * Un incident signale sur une livraison (client absent, colis abime, panne)
 * ou sur un livreur.
 *
 * Le traitement suit StatutIncident. A la resolution, l'operateur peut
 * proposer une transition de la commande concernee (ECHEC_LIVRAISON,
 * RETOURNEE, ANNULEE...) : elle est alors appliquee par CommandeService avec
 * l'origine INCIDENT, donc elle apparait dans l'historique des statuts.
 *
 * Un incident porte au moins l'une des deux references commande ou livreur.
 */
@Entity
@Table(name = "incidents",
        indexes = {
                @Index(name = "idx_incident_commande", columnList = "commande_id"),
                @Index(name = "idx_incident_statut", columnList = "statut")
        })
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeIncident type;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    /** Etat du traitement ; un incident vient toujours en SIGNALEE. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutIncident statut = StatutIncident.SIGNALEE;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    /** Date de cloture : renseignee a la resolution ou au rejet. */
    private LocalDateTime dateResolution;

    /**
     * Transition proposee sur la commande lors de la resolution, si elle a
     * ete appliquee. Null si l'incident a ete resolu sans impact sur le
     * workflow de la commande.
     */
    @Enumerated(EnumType.STRING)
    private StatutCommande transitionProposee;

    @ManyToOne(fetch = FetchType.LAZY)
    private Commande commande;

    @ManyToOne(fetch = FetchType.LAZY)
    private Livreur livreur;

    public Incident() {
    }

    public Incident(TypeIncident type, LocalDateTime date, String description,
                    Commande commande, Livreur livreur) {
        this.type = type;
        this.date = date;
        this.description = description;
        this.commande = commande;
        this.livreur = livreur;
        this.statut = StatutIncident.SIGNALEE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TypeIncident getType() {
        return type;
    }

    public void setType(TypeIncident type) {
        this.type = type;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
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

    public StatutIncident getStatut() {
        return statut;
    }

    public void setStatut(StatutIncident statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateResolution() {
        return dateResolution;
    }

    public void setDateResolution(LocalDateTime dateResolution) {
        this.dateResolution = dateResolution;
    }

    public StatutCommande getTransitionProposee() {
        return transitionProposee;
    }

    public void setTransitionProposee(StatutCommande transitionProposee) {
        this.transitionProposee = transitionProposee;
    }

    public Commande getCommande() {
        return commande;
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }

    public Livreur getLivreur() {
        return livreur;
    }

    public void setLivreur(Livreur livreur) {
        this.livreur = livreur;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Incident incident = (Incident) o;
        return Objects.equals(id, incident.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Incident{"
                + "id=" + id
                + ", type=" + type
                + ", date=" + date
                + ", statut=" + statut
                + ", description='" + description + '\''
                + ", resolution='" + resolution + '\''
                + ", transitionProposee=" + transitionProposee
                + '}';
    }
}
