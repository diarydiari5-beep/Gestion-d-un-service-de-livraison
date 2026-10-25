package com.livraison.dto;

import com.livraison.entity.enums.TypeIncident;

/**
 * Corps de POST /api/incidents. La commande est obligatoire ou, a defaut, le
 * livreur : un incident porte toujours au moins un contexte.
 */
public class IncidentRequest {

    private TypeIncident type;
    private String description;
    private Long commandeId;
    private Long livreurId;

    public IncidentRequest() {
    }

    public IncidentRequest(TypeIncident type, String description, Long commandeId, Long livreurId) {
        this.type = type;
        this.description = description;
        this.commandeId = commandeId;
        this.livreurId = livreurId;
    }

    public TypeIncident getType() {
        return type;
    }

    public void setType(TypeIncident type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCommandeId() {
        return commandeId;
    }

    public void setCommandeId(Long commandeId) {
        this.commandeId = commandeId;
    }

    public Long getLivreurId() {
        return livreurId;
    }

    public void setLivreurId(Long livreurId) {
        this.livreurId = livreurId;
    }
}
