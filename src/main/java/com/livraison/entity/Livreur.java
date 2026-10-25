package com.livraison.entity;

import com.livraison.entity.embeddable.Vehicule;
import com.livraison.entity.enums.StatutLivreur;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "livreurs")
public class Livreur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    private String telephone;

    @Embedded
    private Vehicule vehicule;

    private String zoneActivite;

    private Double latitude;

    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutLivreur statut = StatutLivreur.DISPONIBLE;

    @Column(precision = 10, scale = 2)
    private BigDecimal capaciteMax;

    @OneToMany(mappedBy = "livreur")
    private List<Commande> commandesAssignees = new ArrayList<>();

    @OneToMany(mappedBy = "livreur")
    private List<Incident> incidents = new ArrayList<>();

    public Livreur() {
    }

    public Livreur(Long id, String nom, String prenom, String telephone, Vehicule vehicule,
                   String zoneActivite, StatutLivreur statut, BigDecimal capaciteMax) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.vehicule = vehicule;
        this.zoneActivite = zoneActivite;
        this.statut = statut;
        this.capaciteMax = capaciteMax;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Vehicule getVehicule() {
        return vehicule;
    }

    public void setVehicule(Vehicule vehicule) {
        this.vehicule = vehicule;
    }

    public String getZoneActivite() {
        return zoneActivite;
    }

    public void setZoneActivite(String zoneActivite) {
        this.zoneActivite = zoneActivite;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public StatutLivreur getStatut() {
        return statut;
    }

    public void setStatut(StatutLivreur statut) {
        this.statut = statut;
    }

    public BigDecimal getCapaciteMax() {
        return capaciteMax;
    }

    public void setCapaciteMax(BigDecimal capaciteMax) {
        this.capaciteMax = capaciteMax;
    }

    public List<Commande> getCommandesAssignees() {
        return commandesAssignees;
    }

    public void setCommandesAssignees(List<Commande> commandesAssignees) {
        this.commandesAssignees = commandesAssignees;
    }

    public List<Incident> getIncidents() {
        return incidents;
    }

    public void setIncidents(List<Incident> incidents) {
        this.incidents = incidents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Livreur livreur = (Livreur) o;
        return Objects.equals(id, livreur.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Livreur{"
                + "id=" + id
                + ", nom='" + nom + '\''
                + ", prenom='" + prenom + '\''
                + ", telephone='" + telephone + '\''
                + ", vehicule=" + vehicule
                + ", zoneActivite='" + zoneActivite + '\''
                + ", statut=" + statut
                + ", capaciteMax=" + capaciteMax
                + '}';
    }
}
