package com.livraison.dto;

public class LivreurPositionDto {

    private Long id;
    private String nom;
    private String prenom;
    private Double latitude;
    private Double longitude;
    private String statut;
    private String zoneActivite;

    public LivreurPositionDto() {
    }

    public LivreurPositionDto(Long id, String nom, String prenom, Double latitude,
                              Double longitude, String statut, String zoneActivite) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.latitude = latitude;
        this.longitude = longitude;
        this.statut = statut;
        this.zoneActivite = zoneActivite;
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

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getZoneActivite() {
        return zoneActivite;
    }

    public void setZoneActivite(String zoneActivite) {
        this.zoneActivite = zoneActivite;
    }
}