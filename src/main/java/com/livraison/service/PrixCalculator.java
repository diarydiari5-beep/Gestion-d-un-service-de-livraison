package com.livraison.service;

import com.livraison.dto.PrixCalculeDto;
import com.livraison.entity.Tarif;
import com.livraison.entity.enums.PrioriteCommande;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcul du prix d'une commande a partir d'un bareme.
 *
 * Fonction pure : aucun acces a la base, aucune annotation Spring, donc
 * testable directement sans contexte.
 *
 * sousTotal = prixFixe + distanceKm x prixParKm + poidsKg x prixParKg
 *             + volumeM3 x prixParM3
 * prix      = arrondi(sousTotal x (1 + majorationPriorite / 100), 2)
 */
public final class PrixCalculator {

    private static final BigDecimal CENT = new BigDecimal("0.01");
    private static final BigDecimal CENT_ENTIER = new BigDecimal("100");

    private PrixCalculator() {
    }

    /**
     * @param tarif     bareme applique ; null est tolere et traite comme le
     *                  bareme par defaut
     * @param distanceKm distance en km, nullTolere
     * @param poidsKg   poids en kg, nullTolere
     * @param volumeM3  volume en m3, nullTolere (un volume absent ne coute rien)
     * @param priorite  priorite de la commande, nullTolere
     */
    public static PrixCalculeDto calculer(Tarif tarif, BigDecimal distanceKm,
                                          BigDecimal poidsKg, BigDecimal volumeM3,
                                          PrioriteCommande priorite) {
        Tarif bareme = tarif != null ? tarif : Tarif.valeursParDefaut();

        PrixCalculeDto dto = new PrixCalculeDto();
        dto.setZone(bareme.getZone());
        dto.setPrixFixe(zeroSiNonNull(bareme.getPrixFixe()));
        dto.setPartDistanceKm(produit(zeroSiNonNull(distanceKm), zeroSiNonNull(bareme.getPrixParKm())));
        dto.setPartPoidsKg(produit(zeroSiNonNull(poidsKg), zeroSiNonNull(bareme.getPrixParKg())));
        dto.setPartVolumeM3(produit(zeroSiNonNull(volumeM3), zeroSiNonNull(bareme.getPrixParM3())));

        BigDecimal sousTotal = dto.getPrixFixe()
                .add(dto.getPartDistanceKm())
                .add(dto.getPartPoidsKg())
                .add(dto.getPartVolumeM3());
        dto.setSousTotal(sousTotal);

        BigDecimal taux = zeroSiNonNull(bareme.majorationPour(priorite));
        dto.setTauxMajoration(taux);
        dto.setMontantMajoration(arrondi(sousTotal.multiply(taux).divide(CENT_ENTIER, 10, RoundingMode.HALF_UP)));
        dto.setPrix(arrondi(sousTotal.add(dto.getMontantMajoration())));
        return dto;
    }

    private static BigDecimal produit(BigDecimal quantite, BigDecimal prixUnitaire) {
        return arrondi(quantite.multiply(prixUnitaire));
    }

    private static BigDecimal arrondi(BigDecimal valeur) {
        return valeur.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zeroSiNonNull(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}