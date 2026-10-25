package com.livraison.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class GeoService {

    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org";
    private static final String USER_AGENT = "livraison-app/0.1 (contact@example.com)";
    private static final double RAYON_TERRE_KM = 6371.0;

    private final RestClient restClient;
    private final String baseUrl;

    public GeoService() {
        this(NOMINATIM_BASE_URL);
    }

    GeoService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.restClient = RestClient.builder().build();
    }

    public double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RAYON_TERRE_KM * c;
    }

    public GeoPoint geocoder(String adresse) {
        if (adresse == null || adresse.isBlank()) {
            return null;
        }
        List<GeocodingResult> results;
        try {
            results = restClient.get()
                    .uri(baseUrl + "/search?q={q}&format=json&limit=1", adresse)
                    .header("User-Agent", USER_AGENT)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException e) {
            return null;
        }
        if (results == null || results.isEmpty()) {
            return null;
        }
        GeocodingResult result = results.get(0);
        if (result.lat() == null || result.lon() == null) {
            return null;
        }
        return new GeoPoint(Double.parseDouble(result.lat()), Double.parseDouble(result.lon()));
    }

    public record GeoPoint(double latitude, double longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeocodingResult(String lat, String lon, String displayName) {
    }
}