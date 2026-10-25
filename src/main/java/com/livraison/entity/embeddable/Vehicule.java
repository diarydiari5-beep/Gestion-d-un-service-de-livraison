package com.livraison.entity.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
public class Vehicule {

    @Column(name = "vehicule_type")
    private String type;

    @Column(name = "vehicule_capacite", precision = 10, scale = 2)
    private BigDecimal capacite;

    @Column(name = "vehicule_immatriculation")
    private String immatriculation;

    public Vehicule() {
    }

    public Vehicule(String type, BigDecimal capacite, String immatriculation) {
        this.type = type;
        this.capacite = capacite;
        this.immatriculation = immatriculation;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getCapacite() {
        return capacite;
    }

    public void setCapacite(BigDecimal capacite) {
        this.capacite = capacite;
    }

    public String getImmatriculation() {
        return immatriculation;
    }

    public void setImmatriculation(String immatriculation) {
        this.immatriculation = immatriculation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vehicule vehicule = (Vehicule) o;
        return Objects.equals(type, vehicule.type)
                && Objects.equals(capacite, vehicule.capacite)
                && Objects.equals(immatriculation, vehicule.immatriculation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, capacite, immatriculation);
    }

    @Override
    public String toString() {
        return "Vehicule{"
                + "type='" + type + '\''
                + ", capacite=" + capacite
                + ", immatriculation='" + immatriculation + '\''
                + '}';
    }
}
