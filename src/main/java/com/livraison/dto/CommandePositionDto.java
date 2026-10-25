package com.livraison.dto;

public class CommandePositionDto {

    private Long id;
    private String numero;
    private String statut;
    private String priorite;
    private Double latitudeEnlevement;
    private Double longitudeEnlevement;
    private Double latitudeLivraison;
    private Double longitudeLivraison;
    private Long livreurId;
    private String livreurNom;

    public CommandePositionDto() {
    }

    public CommandePositionDto(Long id, String numero, String statut, String priorite,
                              Double latitudeEnlevement, Double longitudeEnlevement,
                              Double latitudeLivraison, Double longitudeLivraison,
                              Long livreurId, String livreurNom) {
        this.id = id;
        this.numero = numero;
        this.statut = statut;
        this.priorite = priorite;
        this.latitudeEnlevement = latitudeEnlevement;
        this.longitudeEnlevement = longitudeEnlevement;
        this.latitudeLivraison = latitudeLivraison;
        this.longitudeLivraison = longitudeLivraison;
        this.livreurId = livreurId;
        this.livreurNom = livreurNom;
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

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getPriorite() {
        return priorite;
    }

    public void setPriorite(String priorite) {
        this.priorite = priorite;
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
