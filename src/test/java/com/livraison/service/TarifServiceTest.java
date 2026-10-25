package com.livraison.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.livraison.dto.PrixCalculeDto;
import com.livraison.entity.Commande;
import com.livraison.entity.Tarif;
import com.livraison.entity.enums.PrioriteCommande;
import com.livraison.repository.TarifRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Resolution du bareme : zone, sinon TOUTES, sinon valeurs codees en dur. */
class TarifServiceTest {

    private TarifRepository tarifRepository;
    private TarifService tarifService;

    @BeforeEach
    void setUp() {
        tarifRepository = mock(TarifRepository.class);
        tarifService = new TarifService(tarifRepository);
    }

    private static Tarif tarif(String zone) {
        return new Tarif(zone, new BigDecimal("8000.00"), new BigDecimal("1500.00"),
                new BigDecimal("2000.00"), new BigDecimal("3000.00"),
                new BigDecimal("15.00"), new BigDecimal("30.00"));
    }

    /** Bareme propre a Isoraka : chaque taux est different du bareme TOUTES. */
    private static Tarif tarifIsoraka() {
        return new Tarif("isoraka", new BigDecimal("7000.00"), new BigDecimal("1200.00"),
                new BigDecimal("1800.00"), new BigDecimal("2500.00"),
                new BigDecimal("15.00"), new BigDecimal("30.00"));
    }

    @Test
    void leBaremeDeLaZoneEstUtilise() {
        Tarif ivato = tarif("ivato");
        when(tarifRepository.findByZoneIgnoreCase("ivato")).thenReturn(Optional.of(ivato));

        assertEquals("ivato", tarifService.tarifPourZone("ivato").getZone());
    }

    @Test
    void laRechercheInsensibleALaCasseTrouveLaZone() {
        when(tarifRepository.findByZoneIgnoreCase("Ivato")).thenReturn(Optional.of(tarif("ivato")));

        assertEquals("ivato", tarifService.tarifPourZone("Ivato").getZone());
    }

    @Test
    void repliSurToutesQuandLaZoneNAPasDeBareme() {
        Tarif defaut = tarif(Tarif.ZONE_PAR_DEFAUT);
        when(tarifRepository.findByZoneIgnoreCase("mahamasina"))
                .thenReturn(Optional.empty());
        when(tarifRepository.findByZoneIgnoreCase(Tarif.ZONE_PAR_DEFAUT))
                .thenReturn(Optional.of(defaut));

        assertEquals(Tarif.ZONE_PAR_DEFAUT, tarifService.tarifPourZone("mahamasina").getZone());
    }

    @Test
    void valeursCodeesEnDurQuandLaTableEstVide() {
        when(tarifRepository.findByZoneIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertEquals(Tarif.ZONE_PAR_DEFAUT, tarifService.tarifPourZone("mahamasina").getZone());
        assertEquals(Tarif.ZONE_PAR_DEFAUT, tarifService.tarifPourZone(null).getZone());
    }

    @Test
    void leCalculUtiliseLeBaremeDeLaZoneDeLaCommande() {
        when(tarifRepository.findByZoneIgnoreCase("isoraka"))
                .thenReturn(Optional.of(tarifIsoraka()));

        Commande commande = new Commande();
        commande.setZoneLivraison("isoraka");
        commande.setDistance(new BigDecimal("1.33"));
        commande.setPoids(new BigDecimal("5.50"));
        commande.setVolume(new BigDecimal("0.25"));
        commande.setPriorite(PrioriteCommande.NORMALE);

        PrixCalculeDto dto = tarifService.calculer(commande);

        assertEquals("isoraka", dto.getZone());
        // 7000 + 1.33*1200 + 5.50*1800 + 0.25*2500 = 19121
        assertEquals(0, new BigDecimal("19121.00").compareTo(dto.getPrix()));
    }

    @Test
    void enregistrementNormaliseLaZone() {
        when(tarifRepository.save(any(Tarif.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        tarifService.enregistrer(new Tarif("  Ivato  ", BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));

        verify(tarifRepository).save(any(Tarif.class));
    }

    @Test
    void zoneVideEstRefuseeALEnregistrement() {
        try {
            tarifService.enregistrer(new Tarif("  ", BigDecimal.ONE, BigDecimal.ONE,
                    BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));
            throw new AssertionError("Une zone vide doit etre refusee");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("zone"));
        }
    }

    @Test
    void aucunBaremeConfigureEstSignale() {
        when(tarifRepository.count()).thenReturn(0L);
        assertTrue(tarifService.aucunBaremeConfigure());

        when(tarifRepository.count()).thenReturn(3L);
        org.junit.jupiter.api.Assertions.assertFalse(tarifService.aucunBaremeConfigure());
    }

    @Test
    void tousLesBaremesSontEnregistresEnLot() {
        when(tarifRepository.save(any(Tarif.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<Tarif> enregistres = tarifService.enregistrerTous(
                List.of(tarif("ivato"), tarif("isoraka")));

        assertEquals(2, enregistres.size());
    }
}