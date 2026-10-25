package com.livraison.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CommandeCreateRequest {

    private String numero;
    private Long clientId;
    private String adresseEnlevement;
    private String adresseLivraison;
    private String zoneLivraison;
    private BigDecimal poids;
    private BigDecimal volume;
    private BigDecimal prix;
    private String priorite;
    private LocalDateTime datePrevue;

    public CommandeCreateRequest() {
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
}
