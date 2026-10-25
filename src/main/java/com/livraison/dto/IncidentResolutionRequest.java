package com.livraison.dto;

import com.livraison.entity.enums.StatutCommande;

/**
 * Corps de POST /api/incidents/{id}/resoudre et .../rejeter.
 *
 * Pour une resolution, transition est le statut a appliquer a la commande de
 * l'incident ; il reste facultatif (incident resolu sans impact sur le
 * workflow). Pour un rejet, transition est ignoree et resolution tient lieu
 * de motif.
 */
public class IncidentResolutionRequest {

    private String resolution;
    private StatutCommande transition;

    public IncidentResolutionRequest() {
    }

    public IncidentResolutionRequest(String resolution, StatutCommande transition) {
        this.resolution = resolution;
        this.transition = transition;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public StatutCommande getTransition() {
        return transition;
    }

    public void setTransition(StatutCommande transition) {
        this.transition = transition;
    }
}
