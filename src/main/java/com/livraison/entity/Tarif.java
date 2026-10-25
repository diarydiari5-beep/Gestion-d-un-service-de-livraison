package com.livraison.entity;

import com.livraison.entity.enums.PrioriteCommande;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Bareme de facturation d'une zone de livraison.
 *
 * Le prix d'une commande est une valeur derivee : PrixCalculator combine
 * frais fixes, distance, poids et volume, puis applique la majoration de
 * priorite. Une ligne par zone ; la zone {@link #ZONE_PAR_DEFAUT} sert de
 * repli pour toute zone sans bareme propre.
 *
 * Formule : sousTotal = prixFixe + distanceKm x prixParKm + poidsKg x
 * prixParKg + volumeM3 x prixParM3, puis prix = arrondi(sousTotal x
 * (1 + majorationPriorite / 100), 2).
 */
@Entity
@Table(name = "tarifs")
public class Tarif {

    /** Zone virtuelle utilisee quand la zone de la commande n'a pas de bareme. */
    public static final String ZONE_PAR_DEFAUT = "TOUTES";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String zone;

    /** Part fixe du bareme, quel que soit le contenu de la commande. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixFixe = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixParKm = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixParKg = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixParM3 = BigDecimal.ZERO;

    /** Majoration en pourcentage pour une priorite URGENTE. */
    @Column(name = "majoration_urgente_pct", nullable = false, precision = 6, scale = 2)
    private BigDecimal majorationUrgentePct = BigDecimal.ZERO;

    /** Majoration en pourcentage pour une priorite EXPRESS. */
    @Column(name = "majoration_express_pct", nullable = false, precision = 6, scale = 2)
    private BigDecimal majorationExpressPct = BigDecimal.ZERO;

    public Tarif() {
    }

    public Tarif(String zone, BigDecimal prixFixe, BigDecimal prixParKm,
                 BigDecimal prixParKg, BigDecimal prixParM3,
                 BigDecimal majorationUrgentePct, BigDecimal majorationExpressPct) {
        this.zone = zone;
        this.prixFixe = prixFixe;
        this.prixParKm = prixParKm;
        this.prixParKg = prixParKg;
        this.prixParM3 = prixParM3;
        this.majorationUrgentePct = majorationUrgentePct;
        this.majorationExpressPct = majorationExpressPct;
    }

    /**
     * Bareme de secours, uniquement utilise si la table tarifs est vide : la
     * creation d'une commande ne doit jamais echouer faute de configuration.
     */
    public static Tarif valeursParDefaut() {
        return new Tarif(ZONE_PAR_DEFAUT,
                new BigDecimal("8000.00"), new BigDecimal("1500.00"),
                new BigDecimal("2000.00"), new BigDecimal("3000.00"),
                new BigDecimal("15.00"), new BigDecimal("30.00"));
    }

    /** Pourcentage de majoration associe a une priorite. */
    public BigDecimal majorationPour(PrioriteCommande priorite) {
        if (priorite == PrioriteCommande.EXPRESS) {
            return nonNull(majorationExpressPct);
        }
        if (priorite == PrioriteCommande.URGENTE) {
            return nonNull(majorationUrgentePct);
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal nonNull(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public BigDecimal getPrixParKm() {
        return prixParKm;
    }

    public void setPrixParKm(BigDecimal prixParKm) {
        this.prixParKm = prixParKm;
    }

    public BigDecimal getPrixParKg() {
        return prixParKg;
    }

    public void setPrixParKg(BigDecimal prixParKg) {
        this.prixParKg = prixParKg;
    }

    public BigDecimal getPrixParM3() {
        return prixParM3;
    }

    public void setPrixParM3(BigDecimal prixParM3) {
        this.prixParM3 = prixParM3;
    }

    public BigDecimal getMajorationUrgentePct() {
        return majorationUrgentePct;
    }

    public void setMajorationUrgentePct(BigDecimal majorationUrgentePct) {
        this.majorationUrgentePct = majorationUrgentePct;
    }

    public BigDecimal getMajorationExpressPct() {
        return majorationExpressPct;
    }

    public void setMajorationExpressPct(BigDecimal majorationExpressPct) {
        this.majorationExpressPct = majorationExpressPct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tarif tarif = (Tarif) o;
        return Objects.equals(id, tarif.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Tarif{"
                + "id=" + id
                + ", zone='" + zone + '\''
                + ", prixFixe=" + prixFixe
                + ", prixParKm=" + prixParKm
                + ", prixParKg=" + prixParKg
                + ", prixParM3=" + prixParM3
                + ", majorationUrgentePct=" + majorationUrgentePct
                + ", majorationExpressPct=" + majorationExpressPct
                + '}';
    }
}