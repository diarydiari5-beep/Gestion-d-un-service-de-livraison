package com.livraison.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.livraison.dto.RouteDto;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RouteServiceTest {

    private static final String JSON_ORS = """
            {"routes":[{"summary":{"distance":2450.7,"duration":312.4},
            "geometry":{"type":"LineString","coordinates":[[47.525,-18.9100],[47.5280,-18.9050],[47.5320,-18.9000]]}}]}
            """;

    private static final String JSON_OSRM = """
            {"code":"Ok","routes":[{"distance":2410.0,"duration":300.0,
            "geometry":{"type":"LineString","coordinates":[[47.525,-18.9100],[47.5320,-18.9000]]}}]}
            """;

    /** Format des API HeiGIT : FeatureCollection GeoJSON. */
    private static final String JSON_ORS_HEIGIT = """
            {"type":"FeatureCollection","bbox":[47.5210,-18.9101,47.5324,-18.8984],
            "features":[{"bbox":[47.5210,-18.9101,47.5324,-18.8984],"type":"Feature",
            "properties":{"segments":[{"distance":2855.1,"duration":332.7,"steps":[]}],
            "way_points":[0,83],"summary":{"distance":2855.1,"duration":332.7}},
            "geometry":{"type":"LineString","coordinates":[[47.5249,-18.9101],[47.5290,-18.9040],[47.5324,-18.8984]]}}],
            "metadata":{}}
            """;

    private static final String JSON_ORS_SANS_GEOMETRIE = """
            {"type":"FeatureCollection","features":[]}
            """;

    private static final String URL_INVALIDE = "http://localhost:1";

    private HttpServer serveur;
    private final AtomicInteger appels = new AtomicInteger();
    private final AtomicReference<String> dernierChemin = new AtomicReference<>("");
    private final AtomicReference<String> derniereAutorisation = new AtomicReference<>("");
    private final AtomicReference<String> corpsOrs = new AtomicReference<>(JSON_ORS);

    @BeforeEach
    void demarrerServeur() throws IOException {
        corpsOrs.set(JSON_ORS);
        serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serveur.createContext("/v2/directions", e -> repondre(e, corpsOrs.get()));
        serveur.createContext("/route/v1/driving", e -> repondre(e, JSON_OSRM));
        serveur.start();
    }

    @AfterEach
    void arreterServeur() {
        if (serveur != null) {
            serveur.stop(0);
        }
    }

    private void repondre(HttpExchange e, String corps) throws IOException {
        appels.incrementAndGet();
        dernierChemin.set(e.getRequestURI().toString());
        derniereAutorisation.set(String.valueOf(e.getRequestHeaders().getFirst("Authorization")));
        byte[] octets = corps.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().add("Content-Type", "application/json");
        e.sendResponseHeaders(200, octets.length);
        try (OutputStream flux = e.getResponseBody()) {
            flux.write(octets);
        }
    }

    private String urlLocale() {
        return "http://127.0.0.1:" + serveur.getAddress().getPort();
    }

    @Test
    void openRouteServiceRenvoieLaGeometrieRoutiere() {
        RouteService service = new RouteService(RouteService.FOURNISSEUR_ORS, URL_INVALIDE,
                urlLocale(), "cle-de-test", "driving-car", 0);

        RouteDto route = service.calculerRoute(-18.9100, 47.5250, -18.9000, 47.5320);

        assertFalse(route.isApproximative(), "Un appel ORS reussi doit produire un itineraire non approximatif");
        assertEquals(3, route.getPoints().size());
        assertEquals(-18.9100, route.getPoints().get(0)[0], 1e-9);
        assertEquals(47.5250, route.getPoints().get(0)[1], 1e-9);
        assertEquals(-18.9000, route.getPoints().get(2)[0], 1e-9);
        assertEquals(47.5320, route.getPoints().get(2)[1], 1e-9);
        assertEquals(2.4507, route.getDistanceKm(), 1e-4);
        assertEquals(5.2066, route.getDurationMinutes(), 1e-4);
    }

    @Test
    void openRouteServiceRecoitLaCleEtLesCoordonneesOrdonnees() {
        RouteService service = new RouteService(RouteService.FOURNISSEUR_ORS, URL_INVALIDE,
                urlLocale(), "cle-de-test", "driving-car", 0);

        service.calculerRoute(-18.9100, 47.5250, -18.9000, 47.5320);

        String chemin = URLDecoder.decode(dernierChemin.get(), StandardCharsets.UTF_8);
        assertEquals("cle-de-test", derniereAutorisation.get());
        assertTrue(chemin.contains("/v2/directions/driving-car"), chemin);
        assertTrue(chemin.contains("start=47.525,-18.91"), chemin);
        assertTrue(chemin.contains("end=47.532,-18.9"), chemin);
    }

    @Test
    void openRouteServiceAppliqueLesDistancesAuFormatHeiGit() {
        corpsOrs.set(JSON_ORS_HEIGIT);
        RouteService service = new RouteService(RouteService.FOURNISSEUR_ORS, URL_INVALIDE,
                urlLocale(), "cle-de-test", "driving-car", 0);

        RouteDto route = service.calculerRoute(-18.9100, 47.5250, -18.9000, 47.5320);

        assertFalse(route.isApproximative(), "Le format FeatureCollection des API HeiGIT doit etre lu");
        assertEquals(3, route.getPoints().size());
        assertEquals(-18.9101, route.getPoints().get(0)[0], 1e-9);
        assertEquals(47.5249, route.getPoints().get(0)[1], 1e-9);
        assertEquals(2.8551, route.getDistanceKm(), 1e-4);
        assertEquals(5.5450, route.getDurationMinutes(), 1e-4);
    }

    @Test
    void reponseHeiGitSansItineraireReplieLigneDroite() {
        corpsOrs.set(JSON_ORS_SANS_GEOMETRIE);
        RouteService service = new RouteService(RouteService.FOURNISSEUR_ORS, URL_INVALIDE,
                urlLocale(), "cle-de-test", "driving-car", 0);

        RouteDto route = service.calculerRoute(-18.9100, 47.5250, -18.9000, 47.5320);

        assertTrue(route.isApproximative());
        assertEquals(2, route.getPoints().size());
    }

    @Test
    void osrmContinueDeFonctionner() {
        RouteService service = new RouteService(RouteService.FOURNISSEUR_OSRM, urlLocale(),
                URL_INVALIDE, "", "driving-car", 0);

        RouteDto route = service.calculerRoute(-18.9100, 47.5250, -18.9000, 47.5320);

        assertFalse(route.isApproximative());
        assertEquals(2, route.getPoints().size());
        assertEquals(2.41, route.getDistanceKm(), 1e-6);
        assertEquals(5.0, route.getDurationMinutes(), 1e-6);
    }

    @Test
    void openRouteServiceSansCleReplieLigneDroite() {
        RouteService service = new RouteService(RouteService.FOURNISSEUR_ORS, URL_INVALIDE,
                urlLocale(), "  ", "driving-car", 0);

        RouteDto route = service.calculerRoute(-18.9100, 47.5250, -18.9000, 47.5320);

        assertTrue(route.isApproximative());
        assertEquals(0, appels.get(), "Aucun appel HTTP ne doit partir sans cle API");
    }

    @Test
    void reseauInjoignableReplieLigneDroite() {
        RouteService service = new RouteService(RouteService.FOURNISSEUR_ORS, URL_INVALIDE,
                URL_INVALIDE, "cle-de-test", "driving-car", 0);

        RouteDto route = service.calculerRoute(-18.9100, 47.5250, -18.9000, 47.5320);

        assertTrue(route.isApproximative());
        assertEquals(2, route.getPoints().size());
        assertEquals(-18.9100, route.getPoints().get(0)[0], 1e-9);
        assertEquals(-18.9000, route.getPoints().get(1)[0], 1e-9);
    }

    @Test
    void urlOpenRouteServiceParDefautNestPlusLAncienneDepreciee() {
        assertEquals("https://api.heigit.org/openrouteservice", RouteService.ORS_BASE_URL);
        assertFalse(RouteService.ORS_BASE_URL.contains("api.openrouteservice.org"),
                "api.openrouteservice.org est coupe le 28/09/2026");
    }

    @Test
    void distanceDuRepliCorrespondAuCalculHaversine() {
        RouteService service = new RouteService(RouteService.FOURNISSEUR_OSRM, URL_INVALIDE,
                URL_INVALIDE, "", "driving-car", 0);

        RouteDto route = service.calculerRoute(-18.9100, 47.5250, -18.9000, 47.5320);
        double attendu = new GeoService().distanceKm(-18.9100, 47.5250, -18.9000, 47.5320);

        assertEquals(attendu, route.getDistanceKm(), 1e-6);
    }

    @Test
    void fournisseurInconnuReplieLigneDroite() {
        RouteService service = new RouteService("inconnu", URL_INVALIDE,
                URL_INVALIDE, "cle-de-test", "driving-car", 0);

        assertTrue(service.calculerRoute(-18.9100, 47.5250, -18.9000, 47.5320).isApproximative());
        assertEquals(0, appels.get());
    }

    @Test
    void cacheEviteLesAppelsRepetes() {
        RouteService service = new RouteService(RouteService.FOURNISSEUR_ORS, URL_INVALIDE,
                urlLocale(), "cle-de-test", "driving-car", 60);

        for (int i = 0; i < 5; i++) {
            service.calculerRoute(-18.9100, 47.5250, -18.9000, 47.5320);
        }

        assertEquals(1, appels.get(), "Le cache doit fusionner les appels identiques");
    }

    @Test
    void sansCacheChaqueAppelPart() {
        RouteService service = new RouteService(RouteService.FOURNISSEUR_ORS, URL_INVALIDE,
                urlLocale(), "cle-de-test", "driving-car", 0);

        service.calculerRoute(-18.9100, 47.5250, -18.9000, 47.5320);
        service.calculerRoute(-18.9100, 47.5250, -18.9000, 47.5320);

        assertEquals(2, appels.get());
    }

    @Test
    void cacheSertUneCopieDeLaGeometrie() {
        RouteService service = new RouteService(RouteService.FOURNISSEUR_ORS, URL_INVALIDE,
                urlLocale(), "cle-de-test", "driving-car", 60);

        RouteDto premiere = service.calculerRoute(-18.9100, 47.5250, -18.9000, 47.5320);
        RouteDto seconde = service.calculerRoute(-18.9100, 47.5250, -18.9000, 47.5320);

        List<double[]> points = seconde.getPoints();
        assertFalse(premiere.isApproximative());
        assertEquals(points.size(), seconde.getPoints().size());
        assertEquals(1, appels.get());
    }
}
