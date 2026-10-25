package com.livraison.dto;

public class LivreurScoreDto {

    private Long livreurId;
    private String prenom;
    private String nom;
    private String zoneActivite;
    private double score;
    private double scoreProximite;
    private double scoreCharge;
    private double scoreCapacite;
    private double distanceKm;
    private long commandesActives;

    public LivreurScoreDto() {
    }

    public LivreurScoreDto(Long livreurId, String prenom, String nom, String zoneActivite,
                           double score, double scoreProximite, double scoreCharge,
                           double scoreCapacite, double distanceKm, long commandesActives) {
        this.livreurId = livreurId;
        this.prenom = prenom;
        this.nom = nom;
        this.zoneActivite = zoneActivite;
        this.score = score;
        this.scoreProximite = scoreProximite;
        this.scoreCharge = scoreCharge;
        this.scoreCapacite = scoreCapacite;
        this.distanceKm = distanceKm;
        this.commandesActives = commandesActives;
    }

    public Long getLivreurId() {
        return livreurId;
    }

    public void setLivreurId(Long livreurId) {
        this.livreurId = livreurId;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getZoneActivite() {
        return zoneActivite;
    }

    public void setZoneActivite(String zoneActivite) {
        this.zoneActivite = zoneActivite;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getScoreProximite() {
        return scoreProximite;
    }

    public void setScoreProximite(double scoreProximite) {
        this.scoreProximite = scoreProximite;
    }

    public double getScoreCharge() {
        return scoreCharge;
    }

    public void setScoreCharge(double scoreCharge) {
        this.scoreCharge = scoreCharge;
    }

    public double getScoreCapacite() {
        return scoreCapacite;
    }

    public void setScoreCapacite(double scoreCapacite) {
        this.scoreCapacite = scoreCapacite;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public long getCommandesActives() {
        return commandesActives;
    }

    public void setCommandesActives(long commandesActives) {
        this.commandesActives = commandesActives;
    }
}