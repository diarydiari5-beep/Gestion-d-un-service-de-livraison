package com.livraison.entity;

import com.livraison.entity.embeddable.PreuveLivraison;
import com.livraison.entity.enums.PrioriteCommande;
import com.livraison.entity.enums.StatutCommande;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "commandes")
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Client client;

    private String adresseEnlevement;

    private String adresseLivraison;

    private String zoneLivraison;

    private Double latitudeEnlevement;

    private Double longitudeEnlevement;

    private Double latitudeLivraison;

    private Double longitudeLivraison;

    @Column(precision = 10, scale = 2)
    private BigDecimal poids;

    @Column(precision = 10, scale = 2)
    private BigDecimal volume;

    @Column(precision = 10, scale = 2)
    private BigDecimal distance;

    @Column(precision = 10, scale = 2)
    private BigDecimal prix;

    @Enumerated(EnumType.STRING)
    private PrioriteCommande priorite;

    private LocalDateTime datePrevue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCommande statut;

    @ManyToOne(fetch = FetchType.LAZY)
    private Livreur livreur;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Colis> colis = new ArrayList<>();

    @Embedded
    private PreuveLivraison preuveLivraison;

    @OneToMany(mappedBy = "commande")
    private List<Incident> incidents = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateMiseAJour;

    public Commande() {
        this.statut = StatutCommande.CREEE;
    }

    public Commande(Long id, String numero, Client client, String adresseEnlevement,
                    String adresseLivraison, BigDecimal poids, BigDecimal volume,
                    BigDecimal distance, BigDecimal prix, PrioriteCommande priorite,
                    LocalDateTime datePrevue, StatutCommande statut, Livreur livreur,
                    PreuveLivraison preuveLivraison) {
        this.id = id;
        this.numero = numero;
        this.client = client;
        this.adresseEnlevement = adresseEnlevement;
        this.adresseLivraison = adresseLivraison;
        this.poids = poids;
        this.volume = volume;
        this.distance = distance;
        this.prix = prix;
        this.priorite = priorite;
        this.datePrevue = datePrevue;
        this.statut = statut;
        this.livreur = livreur;
        this.preuveLivraison = preuveLivraison;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getAdresseEnlevement() {
        return adresseEnlevement;
    }

    public void setAdresseEnlevement(String adresseEnlevement) {
        this.adresseEnlevement = adresseEnlevement;
    }

    public String getAdresseLivraison() {
        return adresseLivraison;
    }

    public void setAdresseLivraison(String adresseLivraison) {
        this.adresseLivraison = adresseLivraison;
    }

    public String getZoneLivraison() {
        return zoneLivraison;
    }

    public void setZoneLivraison(String zoneLivraison) {
        this.zoneLivraison = zoneLivraison;
    }

    public Double getLatitudeEnlevement() {
        return latitudeEnlevement;
    }

    public void setLatitudeEnlevement(Double latitudeEnlevement) {
        this.latitudeEnlevement = latitudeEnlevement;
    }

    public Double getLongitudeEnlevement() {
        return longitudeEnlevement;
    }

    public void setLongitudeEnlevement(Double longitudeEnlevement) {
        this.longitudeEnlevement = longitudeEnlevement;
    }

    public Double getLatitudeLivraison() {
        return latitudeLivraison;
    }

    public void setLatitudeLivraison(Double latitudeLivraison) {
        this.latitudeLivraison = latitudeLivraison;
    }

    public Double getLongitudeLivraison() {
        return longitudeLivraison;
    }

    public void setLongitudeLivraison(Double longitudeLivraison) {
        this.longitudeLivraison = longitudeLivraison;
    }

    public BigDecimal getPoids() {
        return poids;
    }

    public void setPoids(BigDecimal poids) {
        this.poids = poids;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public BigDecimal getDistance() {
        return distance;
    }

    public void setDistance(BigDecimal distance) {
        this.distance = distance;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }

    public PrioriteCommande getPriorite() {
        return priorite;
    }

    public void setPriorite(PrioriteCommande priorite) {
        this.priorite = priorite;
    }

    public LocalDateTime getDatePrevue() {
        return datePrevue;
    }

    public void setDatePrevue(LocalDateTime datePrevue) {
        this.datePrevue = datePrevue;
    }

    public StatutCommande getStatut() {
        return statut;
    }

    public void setStatut(StatutCommande statut) {
        this.statut = statut;
    }

    public Livreur getLivreur() {
        return livreur;
    }

    public void setLivreur(Livreur livreur) {
        this.livreur = livreur;
    }

    public List<Colis> getColis() {
        return colis;
    }

    public void setColis(List<Colis> colis) {
        this.colis = colis;
    }

    public PreuveLivraison getPreuveLivraison() {
        return preuveLivraison;
    }

    public void setPreuveLivraison(PreuveLivraison preuveLivraison) {
        this.preuveLivraison = preuveLivraison;
    }

    public List<Incident> getIncidents() {
        return incidents;
    }

    public void setIncidents(List<Incident> incidents) {
        this.incidents = incidents;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateMiseAJour() {
        return dateMiseAJour;
    }

    public void setDateMiseAJour(LocalDateTime dateMiseAJour) {
        this.dateMiseAJour = dateMiseAJour;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Commande commande = (Commande) o;
        return Objects.equals(id, commande.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Commande{"
                + "id=" + id
                + ", numero='" + numero + '\''
                + ", adresseEnlevement='" + adresseEnlevement + '\''
                + ", adresseLivraison='" + adresseLivraison + '\''
                + ", poids=" + poids
                + ", volume=" + volume
                + ", distance=" + distance
                + ", prix=" + prix
                + ", priorite=" + priorite
                + ", datePrevue=" + datePrevue
                + ", statut=" + statut
                + ", dateCreation=" + dateCreation
                + ", dateMiseAJour=" + dateMiseAJour
                + '}';
    }
}
