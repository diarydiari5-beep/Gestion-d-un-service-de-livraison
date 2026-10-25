package com.livraison.dto;

import java.math.BigDecimal;

/**
 * Detail d'un calcul de prix : chaque poste est conserve pour que le montant
 * reste reconstituable et affichable sur la fiche commande.
 */
public class PrixCalculeDto {

    private String zone;
    private BigDecimal prixFixe = BigDecimal.ZERO;
    private BigDecimal partDistanceKm = BigDecimal.ZERO;
    private BigDecimal partPoidsKg = BigDecimal.ZERO;
    private BigDecimal partVolumeM3 = BigDecimal.ZERO;
    private BigDecimal sousTotal = BigDecimal.ZERO;
    private BigDecimal tauxMajoration = BigDecimal.ZERO;
    private BigDecimal montantMajoration = BigDecimal.ZERO;
    private BigDecimal prix;

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public BigDecimal getPrixFixe() {
        return prixFixe;
    }

    public void setPrixFixe(BigDecimal prixFixe) {
        this.prixFixe = prixFixe;
    }

    public BigDecimal getPartDistanceKm() {
        return partDistanceKm;
    }

    public void setPartDistanceKm(BigDecimal partDistanceKm) {
        this.partDistanceKm = partDistanceKm;
    }

    public BigDecimal getPartPoidsKg() {
        return partPoidsKg;
    }

    public void setPartPoidsKg(BigDecimal partPoidsKg) {
        this.partPoidsKg = partPoidsKg;
    }

    public BigDecimal getPartVolumeM3() {
        return partVolumeM3;
    }

    public void setPartVolumeM3(BigDecimal partVolumeM3) {
        this.partVolumeM3 = partVolumeM3;
    }

    public BigDecimal getSousTotal() {
        return sousTotal;
    }

    public void setSousTotal(BigDecimal sousTotal) {
        this.sousTotal = sousTotal;
    }

    public BigDecimal getTauxMajoration() {
        return tauxMajoration;
    }

    public void setTauxMajoration(BigDecimal tauxMajoration) {
        this.tauxMajoration = tauxMajoration;
    }

    public BigDecimal getMontantMajoration() {
        return montantMajoration;
    }

    public void setMontantMajoration(BigDecimal montantMajoration) {
        this.montantMajoration = montantMajoration;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }
}