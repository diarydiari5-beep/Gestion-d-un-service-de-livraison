package com.livraison.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Une ligne du tableau de bord de suivi : ou en est une livraison en cours.
 * Les distances et durees proviennent de l'itineraire routier calcule par
 * RouteService ; distanceRoutiere indique si le calcul a reussi ou si l'on
 * affiche le repli en ligne droite.
 */
public class SuiviLigneDto {

    private Long id;
    private String numero;
    private String clientNom;
    private String zoneLivraison;
    private String priorite;
    private String statut;
    private String phase;
    private String cibleLibelle;
    private String livreurNom;
    private String livreurStatut;
    private int commandesActives;
    private int incidentsOuverts;
    private Double distanceKm;
    private String distanceAffichee;
    private Double dureeMinutes;
    private String etaAffichee;
    private Integer avancement;
    private boolean distanceRoutiere;
    private String libelleDistance;
    private LocalDateTime datePrevue;
    private String datePrevueAffichee;
    private List<AlerteSuivi> alertes = new ArrayList<>();

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

    public String getClientNom() {
        return clientNom;
    }

    public void setClientNom(String clientNom) {
        this.clientNom = clientNom;
    }

    public String getZoneLivraison() {
        return zoneLivraison;
    }

    public void setZoneLivraison(String zoneLivraison) {
        this.zoneLivraison = zoneLivraison;
    }

    public String getPriorite() {
        return priorite;
    }

    public void setPriorite(String priorite) {
        this.priorite = priorite;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getCibleLibelle() {
        return cibleLibelle;
    }

    public void setCibleLibelle(String cibleLibelle) {
        this.cibleLibelle = cibleLibelle;
    }

    public String getLivreurNom() {
        return livreurNom;
    }

    public void setLivreurNom(String livreurNom) {
        this.livreurNom = livreurNom;
    }

    public String getLivreurStatut() {
        return livreurStatut;
    }

    public void setLivreurStatut(String livreurStatut) {
        this.livreurStatut = livreurStatut;
    }

    public int getCommandesActives() {
        return commandesActives;
    }

    public void setCommandesActives(int commandesActives) {
        this.commandesActives = commandesActives;
    }

    public int getIncidentsOuverts() {
        return incidentsOuverts;
    }

    public void setIncidentsOuverts(int incidentsOuverts) {
        this.incidentsOuverts = incidentsOuverts;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getDistanceAffichee() {
        return distanceAffichee;
    }

    public void setDistanceAffichee(String distanceAffichee) {
        this.distanceAffichee = distanceAffichee;
    }

    public Double getDureeMinutes() {
        return dureeMinutes;
    }

    public void setDureeMinutes(Double dureeMinutes) {
        this.dureeMinutes = dureeMinutes;
    }

    public String getEtaAffichee() {
        return etaAffichee;
    }

    public void setEtaAffichee(String etaAffichee) {
        this.etaAffichee = etaAffichee;
    }

    public Integer getAvancement() {
        return avancement;
    }

    public void setAvancement(Integer avancement) {
        this.avancement = avancement;
    }

    public boolean isDistanceRoutiere() {
        return distanceRoutiere;
    }

    public void setDistanceRoutiere(boolean distanceRoutiere) {
        this.distanceRoutiere = distanceRoutiere;
    }

    public String getLibelleDistance() {
        return libelleDistance;
    }

    public void setLibelleDistance(String libelleDistance) {
        this.libelleDistance = libelleDistance;
    }

    public LocalDateTime getDatePrevue() {
        return datePrevue;
    }

    public void setDatePrevue(LocalDateTime datePrevue) {
        this.datePrevue = datePrevue;
    }

    public String getDatePrevueAffichee() {
        return datePrevueAffichee;
    }

    public void setDatePrevueAffichee(String datePrevueAffichee) {
        this.datePrevueAffichee = datePrevueAffichee;
    }

    public List<AlerteSuivi> getAlertes() {
        return alertes;
    }

    public void setAlertes(List<AlerteSuivi> alertes) {
        this.alertes = alertes;
    }
}
