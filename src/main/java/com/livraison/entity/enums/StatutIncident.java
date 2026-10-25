package com.livraison.entity.enums;

/**
 * Cycle de vie d'un incident signale sur une livraison ou sur un livreur.
 *
 * SIGNALEE ----> EN_TRAITEMENT ----> RESOLU
 *      |                |
 *      +----------------+----> REJETE
 *
 * Seul un incident RESOLU peut avoir applique une transition proposee sur la
 * commande ; un incident REJETE laisse le workflow de la commande intact.
 */
public enum StatutIncident {

    /** Signale, pas encore pris en charge par un operateur. */
    SIGNALEE,

    /** Un operateur travaille le incident. */
    EN_TRAITEMENT,

    /** Traite : resolution renseignee, transition eventuellement appliquee. */
    RESOLU,

    /** Refuse apres examen (information erronee, incident hors perimetre). */
    REJETE;

    /** Un incident encore ouvert occupe un operateur : il n'est ni resolu ni rejete. */
    public boolean estOuvert() {
        return this == SIGNALEE || this == EN_TRAITEMENT;
    }

    public boolean estCloture() {
        return !estOuvert();
    }
}
