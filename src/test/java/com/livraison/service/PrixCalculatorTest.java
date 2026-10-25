package com.livraison.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.livraison.dto.PrixCalculeDto;
import com.livraison.entity.Tarif;
import com.livraison.entity.enums.PrioriteCommande;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * PrixCalculator est une fonction pure : ces tests n'utilisent ni Spring ni
 * la base, uniquement le bareme passe en parametre.
 */
class PrixCalculatorTest {

    /** Bareme a zéros pour isoler chaque facteur. */
    private static Tarif bareme(BigDecimal fixe, BigDecimal km, BigDecimal kg, BigDecimal m3) {
        return new Tarif("TOUTES", fixe, km, kg, m3,
                new BigDecimal("15.00"), new BigDecimal("30.00"));
    }

    private static final BigDecimal ZERO = new BigDecimal("0");
    private static final BigDecimal UN = new BigDecimal("1");

    @Test
    void prixFixeSeul() {
        PrixCalculeDto dto = PrixCalculator.calculer(
                bareme(new BigDecimal("8000.00"), ZERO, ZERO, ZERO),
                ZERO, ZERO, ZERO, PrioriteCommande.NORMALE);

        assertEquals(0, new BigDecimal("8000.00").compareTo(dto.getPrixFixe()));
        assertEquals(0, new BigDecimal("8000.00").compareTo(dto.getPrix()));
    }

    @Test
    void partDistance() {
        PrixCalculeDto dto = PrixCalculator.calculer(
                bareme(ZERO, new BigDecimal("1500.00"), ZERO, ZERO),
                new BigDecimal("4.80"), ZERO, ZERO, PrioriteCommande.NORMALE);

        assertEquals(0, new BigDecimal("7200.00").compareTo(dto.getPartDistanceKm()));
        assertEquals(0, new BigDecimal("7200.00").compareTo(dto.getPrix()));
    }

    @Test
    void partPoids() {
        PrixCalculeDto dto = PrixCalculator.calculer(
                bareme(ZERO, ZERO, new BigDecimal("2000.00"), ZERO),
                ZERO, new BigDecimal("3.50"), ZERO, PrioriteCommande.NORMALE);

        assertEquals(0, new BigDecimal("7000.00").compareTo(dto.getPartPoidsKg()));
    }

    @Test
    void partVolume() {
        PrixCalculeDto dto = PrixCalculator.calculer(
                bareme(ZERO, ZERO, ZERO, new BigDecimal("3000.00")),
                ZERO, ZERO, new BigDecimal("0.35"), PrioriteCommande.NORMALE);

        assertEquals(0, new BigDecimal("1050.00").compareTo(dto.getPartVolumeM3()));
    }

    @Test
    void volumeNulNeCouteRien() {
        PrixCalculeDto dto = PrixCalculator.calculer(
                bareme(new BigDecimal("5000.00"), new BigDecimal("1000.00"),
                        new BigDecimal("2000.00"), new BigDecimal("3000.00")),
                new BigDecimal("2.00"), new BigDecimal("1.00"),
                BigDecimal.ZERO, PrioriteCommande.NORMALE);

        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getPartVolumeM3()));
        // 5000 + 2*1000 + 1*2000 = 9000
        assertEquals(0, new BigDecimal("9000.00").compareTo(dto.getPrix()));
    }

    @Test
    void volumeAbsentEstTolere() {
        PrixCalculeDto avecNull = PrixCalculator.calculer(
                bareme(new BigDecimal("5000.00"), ZERO, ZERO, new BigDecimal("3000.00")),
                ZERO, ZERO, null, PrioriteCommande.NORMALE);
        PrixCalculeDto avecZero = PrixCalculator.calculer(
                bareme(new BigDecimal("5000.00"), ZERO, ZERO, new BigDecimal("3000.00")),
                ZERO, ZERO, BigDecimal.ZERO, PrioriteCommande.NORMALE);

        assertEquals(0, avecZero.getPrix().compareTo(avecNull.getPrix()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(avecNull.getPrix()));
    }

    @Test
    void prioriteNormaleNAppliqueAucuneMajoration() {
        PrixCalculeDto dto = PrixCalculator.calculer(
                bareme(new BigDecimal("10000.00"), ZERO, ZERO, ZERO),
                ZERO, ZERO, ZERO, PrioriteCommande.NORMALE);

        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getTauxMajoration()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getMontantMajoration()));
        assertEquals(0, new BigDecimal("10000.00").compareTo(dto.getPrix()));
    }

    @Test
    void prioriteUrgenteAppliqueLePourcentageDuBareme() {
        PrixCalculeDto dto = PrixCalculator.calculer(
                bareme(new BigDecimal("10000.00"), ZERO, ZERO, ZERO),
                ZERO, ZERO, ZERO, PrioriteCommande.URGENTE);

        assertEquals(0, new BigDecimal("15.00").compareTo(dto.getTauxMajoration()));
        assertEquals(0, new BigDecimal("1500.00").compareTo(dto.getMontantMajoration()));
        assertEquals(0, new BigDecimal("11500.00").compareTo(dto.getPrix()));
    }

    @Test
    void prioriteExpressAppliqueLePourcentageDuBareme() {
        PrixCalculeDto dto = PrixCalculator.calculer(
                bareme(new BigDecimal("10000.00"), ZERO, ZERO, ZERO),
                ZERO, ZERO, ZERO, PrioriteCommande.EXPRESS);

        assertEquals(0, new BigDecimal("30.00").compareTo(dto.getTauxMajoration()));
        assertEquals(0, new BigDecimal("3000.00").compareTo(dto.getMontantMajoration()));
        assertEquals(0, new BigDecimal("13000.00").compareTo(dto.getPrix()));
    }

    @Test
    void baremeNullRetombeSurLesValeursParDefaut() {
        PrixCalculeDto dto = PrixCalculator.calculer(
                null, new BigDecimal("1.00"), new BigDecimal("1.00"),
                new BigDecimal("1.00"), PrioriteCommande.NORMALE);

        // 8000 + 1*1500 + 1*2000 + 1*3000 = 14500
        assertEquals(Tarif.ZONE_PAR_DEFAUT, dto.getZone());
        assertEquals(0, new BigDecimal("14500.00").compareTo(dto.getPrix()));
    }

    @Test
    void baremeVideNeBrouillePasLePrix() {
        // Tous les facteurs du bareme a zero : le prix vaut exactement 0.
        PrixCalculeDto dto = PrixCalculator.calculer(
                bareme(ZERO, ZERO, ZERO, ZERO),
                new BigDecimal("12.34"), new BigDecimal("56.78"),
                new BigDecimal("9.10"), PrioriteCommande.EXPRESS);

        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getSousTotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getPrix()));
    }

    @Test
    void lePrixEstArrondiAuCentimePres() {
        // 999.999 -> 1000.00
        PrixCalculeDto dto = PrixCalculator.calculer(
                bareme(ZERO, ZERO, ZERO, new BigDecimal("333.333")),
                ZERO, ZERO, new BigDecimal("3"), PrioriteCommande.NORMALE);

        assertEquals(2, dto.getPrix().scale());
        assertEquals(0, new BigDecimal("1000.00").compareTo(dto.getPrix()));
    }

    @Test
    void unExactAxeCentiemeEstArrondiALUnitESuperieure() {
        // 0.125 est un tie exact : HALF_UP doit monter a 0.13, pas 0.12
        PrixCalculeDto dto = PrixCalculator.calculer(
                bareme(ZERO, ZERO, ZERO, new BigDecimal("0.125")),
                ZERO, ZERO, UN, PrioriteCommande.NORMALE);

        assertEquals(0, new BigDecimal("0.13").compareTo(dto.getPrix()));
    }

    @Test
    void leSousTotalAdditionneLesQuatreFacteurs() {
        PrixCalculeDto dto = PrixCalculator.calculer(
                bareme(new BigDecimal("8000.00"), new BigDecimal("1500.00"),
                        new BigDecimal("2000.00"), new BigDecimal("3000.00")),
                new BigDecimal("4.80"), new BigDecimal("3.50"),
                new BigDecimal("0.12"), PrioriteCommande.EXPRESS);

        // 8000 + 7200 + 7000 + 360 = 22560 ; +30 % = 29328
        assertEquals(0, new BigDecimal("22560.00").compareTo(dto.getSousTotal()));
        assertEquals(0, new BigDecimal("6768.00").compareTo(dto.getMontantMajoration()));
        assertEquals(0, new BigDecimal("29328.00").compareTo(dto.getPrix()));
    }

    @Test
    void prioriteAbsenteEstToleree() {
        PrixCalculeDto dto = PrixCalculator.calculer(
                bareme(new BigDecimal("10000.00"), ZERO, ZERO, ZERO),
                ZERO, ZERO, ZERO, null);

        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getTauxMajoration()));
        assertEquals(0, new BigDecimal("10000.00").compareTo(dto.getPrix()));
    }
}