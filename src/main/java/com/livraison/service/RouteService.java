package com.livraison.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.livraison.dto.RouteDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class RouteService {

    private static final Logger LOG = LoggerFactory.getLogger(RouteService.class);

    static final String FOURNISSEUR_OSRM = "osrm";
    static final String FOURNISSEUR_ORS = "openrouteservice";
    static final String OSRM_BASE_URL = "https://router.project-osrm.org";
    // api.openrouteservice.org est deprecie depuis avril 2026 et coupe le 28/09/2026 :
    // les API HeiGIT suivent api.heigit.org/<service>/<version>.
    static final String ORS_BASE_URL = "https://api.heigit.org/openrouteservice";
    static final String ORS_PROFIL_PAR_DEFAUT = "driving-car";
    static final double VITESSE_MOYENNE_KMH = 25.0;
    static final double PRECISION_CLE = 10000.0;
    static final int TAILLE_CACHE_MAX = 200;

    private final RestClient restClient;
    private final String fournisseur;
    private final String osrmBaseUrl;
    private final String orsBaseUrl;
    private final String orsApiKey;
    private final String orsProfil;
    private final long cacheSecondes;
    private final Map<String, Entree> cache = new ConcurrentHashMap<>();

    public RouteService(@Value("${livraison.route.provider:osrm}") String fournisseur,
                        @Value("${livraison.route.osrm-url:" + OSRM_BASE_URL + "}") String osrmBaseUrl,
                        @Value("${livraison.route.ors-url:" + ORS_BASE_URL + "}") String orsBaseUrl,
                        @Value("${livraison.route.ors-api-key:}") String orsApiKey,
                        @Value("${livraison.route.ors-profile:" + ORS_PROFIL_PAR_DEFAUT + "}") String orsProfil,
                        @Value("${livraison.route.cache-seconds:60}") long cacheSecondes) {
        this.fournisseur = fournisseur;
        this.osrmBaseUrl = osrmBaseUrl;
        this.orsBaseUrl = orsBaseUrl;
        this.orsApiKey = orsApiKey;
        this.orsProfil = orsProfil;
        this.cacheSecondes = cacheSecondes;
        this.restClient = RestClient.builder().build();
    }

    public RouteDto calculerRoute(double latDepart, double lonDepart, double latArrivee, double lonArrivee) {
        double distanceLigneDroite = new GeoService().distanceKm(latDepart, lonDepart, latArrivee, lonArrivee);
        String cle = cleCache(latDepart, lonDepart, latArrivee, lonArrivee);
        RouteDto enCache = lireCache(cle);
        if (enCache != null) {
            return enCache;
        }
        RouteDto route = calculerHorsCache(latDepart, lonDepart, latArrivee, lonArrivee, distanceLigneDroite);
        ecrireCache(cle, route);
        return route;
    }

    private RouteDto calculerHorsCache(double latDepart, double lonDepart, double latArrivee,
                                       double lonArrivee, double distanceLigneDroite) {
        if (FOURNISSEUR_ORS.equalsIgnoreCase(fournisseur)) {
            if (orsApiKey == null || orsApiKey.isBlank()) {
                LOG.warn("Fournisseur {} selectionne mais aucune cle API configuree : repli ligne droite.",
                        FOURNISSEUR_ORS);
            } else {
                RouteDto route = viaOpenRouteService(latDepart, lonDepart, latArrivee, lonArrivee);
                if (route != null) {
                    return route;
                }
                return ligneDroite(latDepart, lonDepart, latArrivee, lonArrivee, distanceLigneDroite);
            }
        }
        if (FOURNISSEUR_OSRM.equalsIgnoreCase(fournisseur)) {
            RouteDto route = viaOsrm(latDepart, lonDepart, latArrivee, lonArrivee);
            if (route != null) {
                return route;
            }
            return ligneDroite(latDepart, lonDepart, latArrivee, lonArrivee, distanceLigneDroite);
        }
        return ligneDroite(latDepart, lonDepart, latArrivee, lonArrivee, distanceLigneDroite);
    }

    private RouteDto viaOpenRouteService(double latDepart, double lonDepart,
                                         double latArrivee, double lonArrivee) {
        try {
            OrsReponse reponse = restClient.get()
                    .uri(orsBaseUrl + "/v2/directions/{profil}?start={depart}&end={arrivee}",
                            orsProfil, lonDepart + "," + latDepart, lonArrivee + "," + latArrivee)
                    .header("Authorization", orsApiKey)
                    .retrieve()
                    .body(OrsReponse.class);
            RouteDto route = convertirReponseOrs(reponse);
            if (route != null) {
                return route;
            }
            LOG.warn("OpenRouteService a repondu sans itineraire exploitable : repli ligne droite.");
        } catch (RestClientException e) {
            LOG.warn("Appel OpenRouteService impossible ({} {}) : repli ligne droite.",
                    e.getClass().getSimpleName(), e.getMessage());
        }
        return null;
    }

    /**
     * L'API HeiGIT renvoie une FeatureCollection GeoJSON (resume dans
     * features[].properties.summary) alors que l'API historique renvoyait
     * routes[].summary : les deux formes sont acceptees.
     */
    private RouteDto convertirReponseOrs(OrsReponse reponse) {
        if (reponse == null) {
            return null;
        }
        if (reponse.routes() != null) {
            for (OrsRoute route : reponse.routes()) {
                RouteDto dto = construireRoute(route.geometry(), route.summary());
                if (dto != null) {
                    return dto;
                }
            }
        }
        if (reponse.features() != null) {
            for (OrsFeature feature : reponse.features()) {
                OrsSummary resume = feature.properties() == null ? null : feature.properties().summary();
                RouteDto dto = construireRoute(feature.geometry(), resume);
                if (dto != null) {
                    return dto;
                }
            }
        }
        return null;
    }

    private RouteDto construireRoute(OrsGeometry geometry, OrsSummary resume) {
        List<double[]> points = extraireGeoJson(geometry);
        if (points.isEmpty() || resume == null) {
            return null;
        }
        return new RouteDto(points, metresEnKm(resume.distance()),
                secondesEnMinutes(resume.duration()), false);
    }

    private RouteDto viaOsrm(double latDepart, double lonDepart, double latArrivee, double lonArrivee) {
        try {
            OsrmResponse reponse = restClient.get()
                    .uri(osrmBaseUrl + "/route/v1/driving/{coords}?overview=full&geometries=geojson",
                            lonDepart + "," + latDepart + ";" + lonArrivee + "," + latArrivee)
                    .retrieve()
                    .body(OsrmResponse.class);
            if (reponse != null && reponse.isValide()) {
                OsrmRoute route = reponse.routes().get(0);
                List<double[]> points = extrairePoints(route.geometry());
                if (!points.isEmpty()) {
                    double distanceKm = route.distance() / 1000.0;
                    return new RouteDto(points, distanceKm, route.duration() / 60.0, false);
                }
            }
        } catch (RestClientException e) {
            LOG.warn("Appel OSRM impossible ({} {}) : repli ligne droite.",
                    e.getClass().getSimpleName(), e.getMessage());
        }
        return null;
    }

    private RouteDto ligneDroite(double latDepart, double lonDepart, double latArrivee,
                                 double lonArrivee, double distanceKm) {
        List<double[]> points = List.of(
                new double[]{latDepart, lonDepart},
                new double[]{latArrivee, lonArrivee});
        return new RouteDto(points, distanceKm, distanceKm / VITESSE_MOYENNE_KMH * 60.0, true);
    }

    private List<double[]> extrairePoints(OsrmGeometry geometry) {
        List<double[]> points = new ArrayList<>();
        if (geometry == null || geometry.coordinates() == null) {
            return points;
        }
        for (double[] couple : geometry.coordinates()) {
            if (couple != null && couple.length == 2) {
                points.add(new double[]{couple[1], couple[0]});
            }
        }
        return points;
    }

    private List<double[]> extraireGeoJson(OrsGeometry geometry) {
        List<double[]> points = new ArrayList<>();
        if (geometry == null || geometry.coordinates() == null) {
            return points;
        }
        for (double[] couple : geometry.coordinates()) {
            if (couple != null && couple.length == 2) {
                points.add(new double[]{couple[1], couple[0]});
            }
        }
        return points;
    }

    private double metresEnKm(Double metres) {
        return metres == null ? 0.0 : metres / 1000.0;
    }

    private double secondesEnMinutes(Double secondes) {
        return secondes == null ? 0.0 : secondes / 60.0;
    }

    private String cleCache(double latDepart, double lonDepart, double latArrivee, double lonArrivee) {
        return fournisseur + "|" + arrondi(latDepart) + "|" + arrondi(lonDepart) + "|"
                + arrondi(latArrivee) + "|" + arrondi(lonArrivee);
    }

    private String arrondi(double valeur) {
        return String.valueOf(Math.round(valeur * PRECISION_CLE));
    }

    private RouteDto lireCache(String cle) {
        if (cacheSecondes <= 0) {
            return null;
        }
        Entree entree = cache.get(cle);
        if (entree == null) {
            return null;
        }
        if (!entree.expiration().isAfter(Instant.now())) {
            cache.remove(cle);
            return null;
        }
        return entree.route();
    }

    private void ecrireCache(String cle, RouteDto route) {
        if (cacheSecondes <= 0) {
            return;
        }
        if (cache.size() >= TAILLE_CACHE_MAX) {
            cache.values().removeIf(entree -> !entree.expiration().isAfter(Instant.now()));
        }
        cache.put(cle, new Entree(route, Instant.now().plusSeconds(cacheSecondes)));
    }

    private record Entree(RouteDto route, Instant expiration) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrsReponse(List<OrsRoute> routes, List<OrsFeature> features) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrsFeature(OrsGeometry geometry, OrsProperties properties) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrsProperties(OrsSummary summary) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrsRoute(OrsGeometry geometry, OrsSummary summary) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrsGeometry(String type, List<double[]> coordinates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrsSummary(Double distance, Double duration) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OsrmResponse(String code, List<OsrmRoute> routes) {
        public boolean isValide() {
            return "Ok".equals(code) && routes != null && !routes.isEmpty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OsrmRoute(double distance, double duration, OsrmGeometry geometry) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OsrmGeometry(String type, List<double[]> coordinates) {
    }
}
