package com.livraison.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class StatistiquesDto {

    private long nombreLivraisons;
    private long nombreEchecs;
    private long nombreAnnulees;
    private long nombreRetournes;
    private BigDecimal chiffreAffaires;
    private BigDecimal distanceTotaleKm;
    private BigDecimal revenuMoyen;
    private BigDecimal tauxReussite;
    private BigDecimal tauxEchec;
    private BigDecimal delaiMoyenHeures;
    private List<StatistiqueLivreurDto> parLivreur = new ArrayList<>();

    public StatistiquesDto() {
    }

    public BigDecimal getDelaiMoyenHeures() {
        return delaiMoyenHeures;
    }

    public void setDelaiMoyenHeures(BigDecimal delaiMoyenHeures) {
        this.delaiMoyenHeures = delaiMoyenHeures;
    }

    public List<StatistiqueLivreurDto> getParLivreur() {
        return parLivreur;
    }

    public void setParLivreur(List<StatistiqueLivreurDto> parLivreur) {
        this.parLivreur = parLivreur;
    }

    public long getNombreLivraisons() {
        return nombreLivraisons;
    }

    public void setNombreLivraisons(long nombreLivraisons) {
        this.nombreLivraisons = nombreLivraisons;
    }

    public long getNombreEchecs() {
        return nombreEchecs;
    }

    public void setNombreEchecs(long nombreEchecs) {
        this.nombreEchecs = nombreEchecs;
    }

    public long getNombreAnnulees() {
        return nombreAnnulees;
    }

    public void setNombreAnnulees(long nombreAnnulees) {
        this.nombreAnnulees = nombreAnnulees;
    }

    public long getNombreRetournes() {
        return nombreRetournes;
    }

    public void setNombreRetournes(long nombreRetournes) {
        this.nombreRetournes = nombreRetournes;
    }

    public BigDecimal getChiffreAffaires() {
        return chiffreAffaires;
    }

    public void setChiffreAffaires(BigDecimal chiffreAffaires) {
        this.chiffreAffaires = chiffreAffaires;
    }

    public BigDecimal getDistanceTotaleKm() {
        return distanceTotaleKm;
    }

    public void setDistanceTotaleKm(BigDecimal distanceTotaleKm) {
        this.distanceTotaleKm = distanceTotaleKm;
    }

    public BigDecimal getRevenuMoyen() {
        return revenuMoyen;
    }

    public void setRevenuMoyen(BigDecimal revenuMoyen) {
        this.revenuMoyen = revenuMoyen;
    }

    public BigDecimal getTauxReussite() {
        return tauxReussite;
    }

    public void setTauxReussite(BigDecimal tauxReussite) {
        this.tauxReussite = tauxReussite;
    }

    public BigDecimal getTauxEchec() {
        return tauxEchec;
    }

    public void setTauxEchec(BigDecimal tauxEchec) {
        this.tauxEchec = tauxEchec;
    }
}
