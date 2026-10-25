package com.livraison.dto;

import java.util.List;

public class TrackingDto {

    private Long commandeId;
    private String numero;
    private String statut;
    private String livreurNom;
    private Double livreurLatitude;
    private Double livreurLongitude;
    private String adresseEnlevement;
    private String adresseLivraison;
    private Double latitudeEnlevement;
    private Double longitudeEnlevement;
    private Double latitudeLivraison;
    private Double longitudeLivraison;
    private List<StatutHistoriqueDto> historique;

    public TrackingDto() {
    }

    public Long getCommandeId() {
        return commandeId;
    }

    public void setCommandeId(Long commandeId) {
        this.commandeId = commandeId;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getLivreurNom() {
        return livreurNom;
    }

    public void setLivreurNom(String livreurNom) {
        this.livreurNom = livreurNom;
    }

    public Double getLivreurLatitude() {
        return livreurLatitude;
    }

    public void setLivreurLatitude(Double livreurLatitude) {
        this.livreurLatitude = livreurLatitude;
    }

    public Double getLivreurLongitude() {
        return livreurLongitude;
    }

    public void setLivreurLongitude(Double livreurLongitude) {
        this.livreurLongitude = livreurLongitude;
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

    public List<StatutHistoriqueDto> getHistorique() {
        return historique;
    }

    public void setHistorique(List<StatutHistoriqueDto> historique) {
        this.historique = historique;
    }
}
