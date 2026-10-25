package com.livraison.entity.enums;

/**
 * Origine d'un changement de statut, conservee dans l'historique pour
 * savoir qui a fait le changement et par quel canal.
 */
public enum OrigineHistorique {

    /** Interface Thymeleaf. */
    WEB,

    /** API REST. */
    API,

    /** Attribution automatique d'un livreur. */
    AFFECTATION,

    /** Traitement d'un incident signale. */
    INCIDENT
}
