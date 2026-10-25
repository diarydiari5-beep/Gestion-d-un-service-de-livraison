package com.livraison.service;

import com.livraison.dto.IncidentDto;
import com.livraison.entity.Commande;
import com.livraison.entity.Incident;
import com.livraison.entity.Livreur;
import com.livraison.entity.enums.OrigineHistorique;
import com.livraison.entity.enums.StatutCommande;
import com.livraison.entity.enums.StatutIncident;
import com.livraison.entity.enums.TypeIncident;
import com.livraison.exception.TransitionInvalideException;
import com.livraison.repository.CommandeRepository;
import com.livraison.repository.IncidentRepository;
import com.livraison.repository.LivreurRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Traitement des incidents de livraison.
 *
 * Un incident est signale puis traite : operateur en charge (EN_TRAITEMENT),
 * puis cloture. A la resolution, l'operateur peut proposer une transition de
 * la commande concernee ; elle est alors appliquee par CommandeService avec
 * l'origine INCIDENT, donc l'historique des statuts conserve la trace du
 * changement et de sa cause.
 *
 * Les transitions proposables sont celles du workflow de la commande, moins
 * LIVREE : une resolution d'incident ne vaut pas preuve de livraison, et
 * LIVREE exige une preuve de reception complete.
 */
@Service
@Transactional
public class IncidentService {

    /** Ordre d'affichage et de tri des statuts d'incident. */
    public static final List<StatutIncident> TOUS_STATUTS = List.of(
            StatutIncident.SIGNALEE,
            StatutIncident.EN_TRAITEMENT,
            StatutIncident.RESOLU,
            StatutIncident.REJETE);

    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRANCE);

    private final IncidentRepository incidentRepository;
    private final CommandeRepository commandeRepository;
    private final LivreurRepository livreurRepository;
    private final CommandeService commandeService;
    private final StatutWorkflowService statutWorkflowService;

    public IncidentService(IncidentRepository incidentRepository,
                           CommandeRepository commandeRepository,
                           LivreurRepository livreurRepository,
                           CommandeService commandeService,
                           StatutWorkflowService statutWorkflowService) {
        this.incidentRepository = incidentRepository;
        this.commandeRepository = commandeRepository;
        this.livreurRepository = livreurRepository;
        this.commandeService = commandeService;
        this.statutWorkflowService = statutWorkflowService;
    }

    // ---------------------------------------------------------------- signalement

    /**
     * Signale un incident. La commande est obligatoire ou, a defaut, le livreur :
     * un incident porte toujours au moins un contexte. Le livreur rattache a la
     * commande est repris automatiquement pour que le suivi par livreur soit complet.
     */
    public Incident signaler(TypeIncident type, String description,
                             Long commandeId, Long livreurId) {
        if (type == null) {
            throw new IllegalArgumentException("Le type d'incident est obligatoire");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("La description de l'incident est obligatoire");
        }
        if (commandeId == null && livreurId == null) {
            throw new IllegalArgumentException(
                    "Un incident doit etre rattache a une commande ou a un livreur");
        }

        Commande commande = null;
        Livreur livreur = null;
        if (commandeId != null) {
            commande = chargerCommande(commandeId);
            livreur = commande.getLivreur();
        }
        if (livreurId != null) {
            Livreur demande = livreurRepository.findById(livreurId)
                    .orElseThrow(() -> new EntityNotFoundException("Livreur " + livreurId + " introuvable"));
            if (commande != null && livreur != null && !livreur.getId().equals(demande.getId())) {
                throw new IllegalArgumentException(
                        "Le livreur " + livreurId + " n'est pas affecte a la commande " + commandeId);
            }
            if (livreur == null) {
                livreur = demande;
            }
        }

        Incident incident = new Incident(type, LocalDateTime.now(), description.trim(),
                commande, livreur);
        return incidentRepository.save(incident);
    }

    // ------------------------------------------------------------------ traitement

    /** Prend un incident signale en charge. */
    public Incident traiter(Long id) {
        Incident incident = charger(id);
        if (incident.getStatut() != StatutIncident.SIGNALEE) {
            throw new TransitionInvalideException(
                    "Seul un incident signale peut etre mis en traitement (statut actuel : "
                            + incident.getStatut() + ")");
        }
        incident.setStatut(StatutIncident.EN_TRAITEMENT);
        return incidentRepository.save(incident);
    }

    /**
     * Cloture un incident en le resolvant.
     *
     * @param resolution        compte rendu de traitement ; obligatoire
     * @param transitionProposee statut a appliquer a la commande concernee, ou null
     *                           si l'incident n'a pas d'impact sur le workflow
     * @throws TransitionInvalideException si l'incident est deja cloture ou si la
     *                                   transition proposee n'est pas autorisee
     */
    public Incident resoudre(Long id, String resolution, StatutCommande transitionProposee) {
        Incident incident = charger(id);
        if (incident.getStatut().estCloture()) {
            throw new TransitionInvalideException(
                    "Incident deja cloture (" + incident.getStatut() + ")");
        }
        if (resolution == null || resolution.isBlank()) {
            throw new IllegalArgumentException("La resolution de l'incident est obligatoire");
        }

        StatutCommande appliquee = appliquerTransitionProposee(incident, transitionProposee);

        incident.setStatut(StatutIncident.RESOLU);
        incident.setResolution(resolution.trim());
        incident.setDateResolution(LocalDateTime.now());
        incident.setTransitionProposee(appliquee);
        return incidentRepository.save(incident);
    }

    /**
     * Rejette un incident apres examen. Le workflow de la commande reste
     * intact : c'est la distinction avec resoudre(..., transition).
     */
    public Incident rejeter(Long id, String motif) {
        Incident incident = charger(id);
        if (incident.getStatut().estCloture()) {
            throw new TransitionInvalideException(
                    "Incident deja cloture (" + incident.getStatut() + ")");
        }
        if (motif == null || motif.isBlank()) {
            throw new IllegalArgumentException("Le motif du rejet est obligatoire");
        }
        incident.setStatut(StatutIncident.REJETE);
        incident.setResolution(motif.trim());
        incident.setDateResolution(LocalDateTime.now());
        incident.setTransitionProposee(null);
        return incidentRepository.save(incident);
    }

    public void supprimer(Long id) {
        incidentRepository.deleteById(charger(id).getId());
    }

    // -------------------------------------------------------------------- lectures

    @Transactional(readOnly = true)
    public List<Incident> findAll() {
        return incidentRepository.findAllByOrderByDateDesc();
    }

    @Transactional(readOnly = true)
    public List<IncidentDto> findAllDto() {
        return findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentDto> findByStatutDto(StatutIncident statut) {
        return (statut == null
                ? incidentRepository.findAllByOrderByDateDesc()
                : incidentRepository.findByStatutOrderByDateDesc(statut)).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentDto> findByCommandeDto(Long commandeId) {
        return incidentRepository.findByCommandeIdOrderByDateDesc(commandeId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentDto> findByLivreurDto(Long livreurId) {
        return incidentRepository.findByLivreurIdOrderByDateDesc(livreurId).stream()
                .map(this::toDto)
                .toList();
    }

    /** Incidents d'une commande encore ouverts : ils justifient une alerte au suivi. */
    @Transactional(readOnly = true)
    public List<Incident> findOuvertsParCommande(Long commandeId) {
        return incidentRepository.findByCommandeIdAndStatutInOrderByDateDesc(
                commandeId, List.of(StatutIncident.SIGNALEE, StatutIncident.EN_TRAITEMENT));
    }

    @Transactional(readOnly = true)
    public Incident findById(Long id) {
        return charger(id);
    }

    @Transactional(readOnly = true)
    public IncidentDto findDto(Long id) {
        return toDto(charger(id));
    }

    /** Compteurs par statut et par type pour le tableau de bord des incidents. */
    @Transactional(readOnly = true)
    public Map<String, Long> compteursParStatut() {
        Map<String, Long> compteurs = new LinkedHashMap<>();
        for (StatutIncident statut : TOUS_STATUTS) {
            compteurs.put(statut.name(), incidentRepository.countByStatut(statut));
        }
        return compteurs;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> compteursParType() {
        Map<String, Long> compteurs = new LinkedHashMap<>();
        for (TypeIncident type : TypeIncident.values()) {
            compteurs.put(type.name(), incidentRepository.countByType(type));
        }
        return compteurs;
    }

    /** Nombre d'incidents encore ouverts, tous types confondus. */
    @Transactional(readOnly = true)
    public long nombreOuverts() {
        return incidentRepository.countByStatut(StatutIncident.SIGNALEE)
                + incidentRepository.countByStatut(StatutIncident.EN_TRAITEMENT);
    }

    /**
     * Transitions proposables pour un incident : celles du workflow de la
     * commande concernee, moins LIVREE (qui exige une preuve de reception).
     */
    @Transactional(readOnly = true)
    public List<StatutCommande> transitionsProposables(Long incidentId) {
        Incident incident = charger(incidentId);
        if (incident.getCommande() == null || incident.getCommande().getStatut() == null) {
            return List.of();
        }
        return statutWorkflowService.transitionsPossibles(incident.getCommande().getStatut()).stream()
                .filter(cible -> cible != StatutCommande.LIVREE)
                .sorted()
                .toList();
    }

    // ------------------------------------------------------------------- interne

    private StatutCommande appliquerTransitionProposee(Incident incident,
                                                      StatutCommande transitionProposee) {
        if (transitionProposee == null) {
            return null;
        }
        if (incident.getCommande() == null) {
            throw new TransitionInvalideException(
                    "Cet incident n'est rattache a aucune commande : pas de transition a appliquer");
        }
        if (transitionProposee == StatutCommande.LIVREE) {
            throw new TransitionInvalideException(
                    "Une resolution d'incident ne peut pas valider la livraison : "
                            + "une preuve de reception est requise");
        }
        commandeService.changerStatut(incident.getCommande().getId(), transitionProposee,
                null, OrigineHistorique.INCIDENT);
        return transitionProposee;
    }

    private Incident charger(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Incident " + id + " introuvable"));
    }

    private Commande chargerCommande(Long id) {
        return commandeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Commande " + id + " introuvable"));
    }

    public IncidentDto toDto(Incident incident) {
        IncidentDto dto = new IncidentDto();
        dto.setId(incident.getId());
        dto.setType(incident.getType() == null ? null : incident.getType().name());
        dto.setTypeLibelle(libelleType(incident.getType()));
        dto.setStatut(incident.getStatut() == null ? null : incident.getStatut().name());
        dto.setStatutLibelle(libelleStatut(incident.getStatut()));
        dto.setDescription(incident.getDescription());
        dto.setResolution(incident.getResolution());
        dto.setDate(incident.getDate());
        dto.setDateAffichee(afficherDate(incident.getDate()));
        dto.setDateResolution(incident.getDateResolution());
        dto.setDateResolutionAffichee(afficherDate(incident.getDateResolution()));
        dto.setTransitionProposee(incident.getTransitionProposee() == null
                ? null : incident.getTransitionProposee().name());
        dto.setCommandeId(incident.getCommande() == null ? null : incident.getCommande().getId());
        dto.setCommandeNumero(incident.getCommande() == null ? null : incident.getCommande().getNumero());
        dto.setCommandeStatut(incident.getCommande() == null || incident.getCommande().getStatut() == null
                ? null : incident.getCommande().getStatut().name());
        dto.setLivreurId(incident.getLivreur() == null ? null : incident.getLivreur().getId());
        dto.setLivreurNom(incident.getLivreur() == null ? null
                : (incident.getLivreur().getPrenom() + " " + incident.getLivreur().getNom()).trim());
        return dto;
    }

    private String afficherDate(LocalDateTime date) {
        return date == null ? null : FORMAT_DATE.format(date);
    }

    public static String libelleType(TypeIncident type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case CLIENT_ABSENT -> "Client absent";
            case ADRESSE_INCORRECTE -> "Adresse incorrecte";
            case COLIS_ENDOMMAGE -> "Colis endommage";
            case VEHICULE_EN_PANNE -> "Vehicule en panne";
            case REFUS_CLIENT -> "Refus du client";
        };
    }

    public static String libelleStatut(StatutIncident statut) {
        if (statut == null) {
            return null;
        }
        return switch (statut) {
            case SIGNALEE -> "Signale";
            case EN_TRAITEMENT -> "En traitement";
            case RESOLU -> "Resolu";
            case REJETE -> "Rejete";
        };
    }

    /** Statuts considers ouverts, utilises par les filtres et les compteurs. */
    public static Collection<StatutIncident> statutsOuverts() {
        return List.of(StatutIncident.SIGNALEE, StatutIncident.EN_TRAITEMENT);
    }
}
