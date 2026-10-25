package com.livraison.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GeoServiceTest {

    private final GeoService geoService = new GeoService();

    @Test
    void distanceSurEquateurUnDegre() {
        double distance = geoService.distanceKm(0.0, 0.0, 0.0, 1.0);
        assertEquals(111.19, distance, 0.2);
    }

    @Test
    void distanceMemePointEstNulle() {
        assertEquals(0.0, geoService.distanceKm(48.8566, 2.3522, 48.8566, 2.3522), 1e-9);
    }

    @Test
    void distanceParisBruxelles() {
        double distance = geoService.distanceKm(48.8566, 2.3522, 50.8503, 4.3517);
        assertTrue(distance >= 255.0 && distance <= 275.0,
                "Distance Paris-Bruxelles attendue ~265 km, obtenue " + distance);
    }

    @Test
    void distanceSymetrique() {
        double d1 = geoService.distanceKm(48.8566, 2.3522, 45.7640, 4.8357);
        double d2 = geoService.distanceKm(45.7640, 4.8357, 48.8566, 2.3522);
        assertEquals(d1, d2, 1e-9);
    }

    @Test
    void geocoderAdresseVideRenvoyeNull() {
        assertNull(geoService.geocoder(null));
        assertNull(geoService.geocoder("  "));
    }

    @Test
    void geocoderReseauInjoignableRenvoyeNull() {
        GeoService serviceInjoignable = new GeoService("http://localhost:1");
        assertNull(serviceInjoignable.geocoder("Analakely, Antananarivo, Madagascar"));
    }
}
