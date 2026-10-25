package com.livraison.service;

import com.livraison.dto.CommandePositionDto;
import com.livraison.dto.LivreurScoreDto;
import com.livraison.dto.PrixCalculeDto;
import com.livraison.entity.Client;
import com.livraison.entity.Commande;
import com.livraison.entity.Livreur;
import com.livraison.entity.embeddable.PreuveLivraison;
import com.livraison.entity.enums.OrigineHistorique;
import com.livraison.entity.enums.PrioriteCommande;
import com.livraison.entity.enums.StatutCommande;
import com.livraison.entity.enums.StatutLivreur;
import com.livraison.exception.PreuveLivraisonIncompleteException;
import com.livraison.exception.TransitionInvalideException;
import com.livraison.repository.ClientRepository;
import com.livraison.repository.CommandeRepository;
import com.livraison.repository.IncidentRepository;
import com.livraison.repository.LivreurRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CommandeService {

    private static final List<StatutCommande> STATUTS_GEOLOCALISABLES = List.of(
            StatutCommande.ASSIGNEE,
            StatutCommande.PRISE_EN_CHARGE,
            StatutCommande.EN_TRANSIT);

    private final CommandeRepository commandeRepository;
    private final ClientRepository clientRepository;
    private final LivreurRepository livreurRepository;
    private final IncidentRepository incidentRepository;
    private final StatutWorkflowService statutWorkflowService;
    private final LivreurService livreurService;
    private final HistoriqueService historiqueService;
    private final TarifService tarifService;
    private final AttributionService attributionService;

    public CommandeService(CommandeRepository commandeRepository,
                           ClientRepository clientRepository,
                           LivreurRepository livreurRepository,
                           IncidentRepository incidentRepository,
                           StatutWorkflowService statutWorkflowService,
                           LivreurService livreurService,
                           HistoriqueService historiqueService,
                           TarifService tarifService,
                           AttributionService attributionService) {
        this.commandeRepository = commandeRepository;
        this.clientRepository = clientRepository;
        this.livreurRepository = livreurRepository;
        this.incidentRepository = incidentRepository;
        this.statutWorkflowService = statutWorkflowService;
        this.livreurService = livreurService;
        this.historiqueService = historiqueService;
        this.tarifService = tarifService;
        this.attributionService = attributionService;
    }

    @Transactional(readOnly = true)
    public List<Commande> findAll() {
        return commandeRepository.findAll();
    }

    /**
     * Commandes en attente d'affectation (statut CREEE), triees par priorite
     * decroissante puis date prevue croissante. Enveloppe la requete existante
     * deja utilisee par l'attribution automatique.
     */
    @Transactional(readOnly = true)
    public List<Commande> findEnAttente() {
        return commandeRepository.findCommandesEnAttenteAffectation();
    }

    /** Sous-ensemble des commandes en attente qui n'ont pas encore de livreur. */
    @Transactional(readOnly = true)
    public List<Commande> findAffectables() {
        return findEnAttente().stream()
                .filter(commande -> commande.getLivreur() == null)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Commande> findById(Long id) {
        return commandeRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Commande> findByNumero(String numero) {
        return commandeRepository.findByNumero(numero);
    }

    @Transactional(readOnly = true)
    public List<Commande> findByStatut(StatutCommande statut) {
        return commandeRepository.findByStatut(statut);
    }

    @Transactional(readOnly = true)
    public Optional<Client> findClientById(Long id) {
        return clientRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<StatutCommande> getTransitionsPossibles(Long id) {
        return commandeRepository.findById(id)
                .map(commande -> List.copyOf(statutWorkflowService.transitionsPossibles(commande.getStatut())))
                .orElseGet(List::of);
    }

    public Commande save(Commande commande) {
        return save(commande, OrigineHistorique.WEB);
    }

    public Commande save(Commande commande, OrigineHistorique origine) {
        boolean creation = commande.getId() == null;
        if (commande.getPrix() == null) {
            // Prix derive du bareme : le serveur est la seule source de verite
            // des que l'operateur ne fournit pas de montant.
            commande.setPrix(tarifService.calculer(commande).getPrix());
        }
        Commande enregistree = commandeRepository.save(commande);
        if (creation) {
            historiqueService.enregistrer(enregistree, null, enregistree.getStatut(),
                    origine, HistoriqueService.COMMENTAIRE_CREATION);
        }
        return enregistree;
    }

    /**
     * Recalcule le prix depuis le bareme courant et renvoie le detail du
     * calcul, indipendamment du prix enregistre precedemment.
     */
    public PrixCalculeDto recalculerPrix(Long id) {
        Commande commande = chargerCommande(id);
        PrixCalculeDto detail = tarifService.calculer(commande);
        commande.setPrix(detail.getPrix());
        commandeRepository.save(commande);
        return detail;
    }

    /** Detail du prix d'une commande sans rien modifier. */
    @Transactional(readOnly = true)
    public PrixCalculeDto detailPrix(Long id) {
        return tarifService.calculer(chargerCommande(id));
    }

    public Commande changerStatut(Long id, StatutCommande cible) {
        return changerStatut(id, cible, null, OrigineHistorique.WEB);
    }

    public Commande changerStatut(Long id, StatutCommande cible, PreuveLivraison preuve) {
        return changerStatut(id, cible, preuve, OrigineHistorique.WEB);
    }

    public Commande changerStatut(Long id, StatutCommande cible, PreuveLivraison preuve,
                                  OrigineHistorique origine) {
        Commande commande = chargerCommande(id);
        changerStatut(commande, cible, preuve, origine);
        return commandeRepository.save(commande);
    }

    public Commande assigner(Long id, Livreur livreur) {
        if (livreur == null) {
            throw new EntityNotFoundException("Livreur introuvable");
        }
        Commande commande = chargerCommande(id);
        commande.setLivreur(livreur);
        changerStatut(commande, StatutCommande.ASSIGNEE, null, OrigineHistorique.AFFECTATION);
        return commandeRepository.save(commande);
    }

    /**
     * Affecte automatiquement le livreur au meilleur score pour la commande.
     * Retourne le livreur retenu, ou vide si aucun candidat eligible.
     */
    public Optional<Livreur> affecterMeilleurLivreur(Long id) {
        Commande commande = chargerCommande(id);
        Optional<LivreurScoreDto> meilleur = attributionService.trouverMeilleurLivreur(commande);
        if (meilleur.isEmpty()) {
            return Optional.empty();
        }
        Livreur livreur = livreurService.findById(meilleur.get().getLivreurId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Livreur " + meilleur.get().getLivreurId() + " introuvable"));
        commande.setLivreur(livreur);
        changerStatut(commande, StatutCommande.ASSIGNEE, null, OrigineHistorique.AFFECTATION);
        commandeRepository.save(commande);
        return Optional.of(livreur);
    }

    private Commande chargerCommande(Long id) {
        return commandeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Commande " + id + " introuvable"));
    }

    private void changerStatut(Commande commande, StatutCommande cible, PreuveLivraison preuve,
                               OrigineHistorique origine) {
        StatutCommande actuel = commande.getStatut();
        if (cible == null) {
            throw new TransitionInvalideException("Statut cible manquant");
        }
        if (!statutWorkflowService.estAutorisee(actuel, cible)) {
            throw new TransitionInvalideException(
                    "Transition interdite : " + actuel + " -> " + cible);
        }
        if (cible == StatutCommande.ASSIGNEE && commande.getLivreur() == null) {
            throw new TransitionInvalideException(
                    "Un livreur doit etre affecte avant de passer la commande en ASSIGNEE");
        }
        if (cible == StatutCommande.LIVREE) {
            PreuveLivraison preuveEffective = preuve != null ? preuve : commande.getPreuveLivraison();
            if (!preuveEstComplete(preuveEffective)) {
                throw new PreuveLivraisonIncompleteException(
                        "La date/heure, le nom du réceptionnaire et le code de confirmation sont obligatoires");
            }
            commande.setPreuveLivraison(preuveEffective);
        }
        commande.setStatut(cible);
        historiqueService.enregistrer(commande, actuel, cible, origine, null);
        Livreur livreur = commande.getLivreur();
        if (livreur != null) {
            appliquerStatutLivreur(livreur, cible);
        }
    }

    private boolean preuveEstComplete(PreuveLivraison preuve) {
        return preuve != null
                && preuve.getDateHeure() != null
                && nonVide(preuve.getNomReceptionnaire())
                && nonVide(preuve.getCodeConfirmation());
    }

    private boolean nonVide(String valeur) {
        return valeur != null && !valeur.isBlank();
    }

    private void appliquerStatutLivreur(Livreur livreur, StatutCommande cible) {
        if (cible == StatutCommande.PRISE_EN_CHARGE) {
            livreur.setStatut(StatutLivreur.EN_LIVRAISON);
            livreurService.save(livreur);
        } else if (cible == StatutCommande.LIVREE || cible == StatutCommande.ECHEC_LIVRAISON
                || cible == StatutCommande.RETOURNEE || cible == StatutCommande.ANNULEE) {
            long restantes = commandeRepository.countByLivreurIdAndStatutIn(livreur.getId(), STATUTS_GEOLOCALISABLES);
            if (restantes == 0 && livreur.getStatut() == StatutLivreur.EN_LIVRAISON) {
                livreur.setStatut(StatutLivreur.DISPONIBLE);
                livreurService.save(livreur);
            }
        }
    }

    public void deleteById(Long id) {
        incidentRepository.deleteByCommandeId(id);
        historiqueService.supprimer(id);
        commandeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Client> findAllClients() {
        return clientRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Livreur> findAllLivreurs() {
        return livreurRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<String> getAllZones() {
        return livreurRepository.findAll().stream()
                .map(Livreur::getZoneActivite)
                .filter(zone -> zone != null && !zone.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public StatutCommande[] getAllStatuts() {
        return StatutCommande.values();
    }

    @Transactional(readOnly = true)
    public PrioriteCommande[] getAllPriorites() {
        return PrioriteCommande.values();
    }

    @Transactional(readOnly = true)
    public List<Commande> findGeolocalisees() {
        return commandeRepository.findByStatutIn(STATUTS_GEOLOCALISABLES).stream()
                .filter(commande -> commande.getLatitudeEnlevement() != null
                        && commande.getLongitudeEnlevement() != null
                        && commande.getLatitudeLivraison() != null
                        && commande.getLongitudeLivraison() != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommandePositionDto> findAllPositionsGeolocalisees() {
        return findGeolocalisees().stream().map(this::toPositionDto).toList();
    }

    private CommandePositionDto toPositionDto(Commande commande) {
        Livreur livreur = commande.getLivreur();
        return new CommandePositionDto(
                commande.getId(),
                commande.getNumero(),
                commande.getStatut() != null ? commande.getStatut().name() : null,
                commande.getPriorite() != null ? commande.getPriorite().name() : null,
                commande.getLatitudeEnlevement(),
                commande.getLongitudeEnlevement(),
                commande.getLatitudeLivraison(),
                commande.getLongitudeLivraison(),
                livreur != null ? livreur.getId() : null,
                livreur != null ? livreur.getPrenom() + " " + livreur.getNom() : null);
    }
}
