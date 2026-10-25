package com.livraison.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CommandeDto {

    private Long id;
    private String numero;
    private Long clientId;
    private String clientNom;
    private String adresseEnlevement;
    private String adresseLivraison;
    private String zoneLivraison;
    private BigDecimal poids;
    private BigDecimal volume;
    private BigDecimal distance;
    private BigDecimal prix;
    private String priorite;
    private LocalDateTime datePrevue;
    private String statut;
    private Long livreurId;
    private String livreurNom;
    private Integer nombreColis;
    private List<String> statutsSuivants;
    private List<StatutHistoriqueDto> historique;

    public CommandeDto() {
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

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getClientNom() {
        return clientNom;
    }

    public void setClientNom(String clientNom) {
        this.clientNom = clientNom;
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

    public String getPriorite() {
        return priorite;
    }

    public void setPriorite(String priorite) {
        this.priorite = priorite;
    }

    public LocalDateTime getDatePrevue() {
        return datePrevue;
    }

    public void setDatePrevue(LocalDateTime datePrevue) {
        this.datePrevue = datePrevue;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
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

    public Integer getNombreColis() {
        return nombreColis;
    }

    public void setNombreColis(Integer nombreColis) {
        this.nombreColis = nombreColis;
    }

    public List<String> getStatutsSuivants() {
        return statutsSuivants;
    }

    public void setStatutsSuivants(List<String> statutsSuivants) {
        this.statutsSuivants = statutsSuivants;
    }

    public List<StatutHistoriqueDto> getHistorique() {
        return historique;
    }

    public void setHistorique(List<StatutHistoriqueDto> historique) {
        this.historique = historique;
    }
}
