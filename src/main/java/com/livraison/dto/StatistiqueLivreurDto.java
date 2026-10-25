package com.livraison.dto;

import java.math.BigDecimal;

/** Performance agregee d'un livreur sur la periode et les filtres courants. */
public class StatistiqueLivreurDto {

    private Long livreurId;
    private String nom;
    private long livraisons;
    private BigDecimal chiffreAffaires;
    private BigDecimal distanceTotaleKm;
    private BigDecimal tauxReussite;

    public StatistiqueLivreurDto() {
    }

    public StatistiqueLivreurDto(Long livreurId, String nom, long livraisons,
                                 BigDecimal chiffreAffaires, BigDecimal distanceTotaleKm,
                                 BigDecimal tauxReussite) {
        this.livreurId = livreurId;
        this.nom = nom;
        this.livraisons = livraisons;
        this.chiffreAffaires = chiffreAffaires;
        this.distanceTotaleKm = distanceTotaleKm;
        this.tauxReussite = tauxReussite;
    }

    public Long getLivreurId() {
        return livreurId;
    }

    public void setLivreurId(Long livreurId) {
        this.livreurId = livreurId;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public long getLivraisons() {
        return livraisons;
    }

    public void setLivraisons(long livraisons) {
        this.livraisons = livraisons;
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

    public BigDecimal getTauxReussite() {
        return tauxReussite;
    }

    public void setTauxReussite(BigDecimal tauxReussite) {
        this.tauxReussite = tauxReussite;
    }
}
