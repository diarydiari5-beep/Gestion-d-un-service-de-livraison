package com.livraison.service;

import com.livraison.dto.StatistiqueLivreurDto;
import com.livraison.dto.StatistiquesDto;
import com.livraison.entity.Commande;
import com.livraison.entity.Livreur;
import com.livraison.entity.enums.StatutCommande;
import com.livraison.repository.CommandeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatistiquesService {

    private final CommandeRepository commandeRepository;

    public StatistiquesService(CommandeRepository commandeRepository) {
        this.commandeRepository = commandeRepository;
    }

    public StatistiquesDto calculer(LocalDate debut, LocalDate fin, Long livreurId) {
        return calculer(debut, fin, livreurId, null);
    }

    /**
     * Aucune requete SQL nouvelle : la liste est chargee puis filtree en Java par
     * zone et par livreur. Le filtre zone est insensible a la casse.
     */
    public StatistiquesDto calculer(LocalDate debut, LocalDate fin, Long livreurId, String zone) {
        LocalDateTime debutDtf = debut != null ? debut.atStartOfDay() : null;
        LocalDateTime finDtf = fin != null ? fin.plusDays(1).atStartOfDay() : null;
        List<Commande> commandes = chargerCommandes(debutDtf, finDtf, livreurId).stream()
                .filter(c -> correspondZone(c, zone))
                .toList();

        long livrees = count(commandes, StatutCommande.LIVREE);
        long echecs = count(commandes, StatutCommande.ECHEC_LIVRAISON);
        long annulees = count(commandes, StatutCommande.ANNULEE);
        long retournees = count(commandes, StatutCommande.RETOURNEE);

        StatistiquesDto stats = new StatistiquesDto();
        stats.setNombreLivraisons(livrees);
        stats.setNombreEchecs(echecs);
        stats.setNombreAnnulees(annulees);
        stats.setNombreRetournes(retournees);
        stats.setChiffreAffaires(somme(commandes, StatutCommande.LIVREE, true));
        stats.setDistanceTotaleKm(somme(commandes, StatutCommande.LIVREE, false));

        long total = commandes.size();
        stats.setTauxReussite(pourcentage(livrees, total));
        stats.setTauxEchec(pourcentage(echecs + retournees, total));
        stats.setDelaiMoyenHeures(delaiMoyenHeures(commandes));
        stats.setParLivreur(repartitionParLivreur(commandes));

        BigDecimal chiffreAffaires = stats.getChiffreAffaires();
        if (chiffreAffaires == null) {
            chiffreAffaires = BigDecimal.ZERO;
        }
        stats.setRevenuMoyen(livrees == 0
                ? BigDecimal.ZERO
                : chiffreAffaires.divide(BigDecimal.valueOf(livrees), 2, RoundingMode.HALF_UP));
        return stats;
    }

    /** Zones presentes dans les commandes, pour alimenter le selecteur de l'ecran. */
    public List<String> zonesDisponibles() {
        TreeSet<String> zones = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        commandeRepository.findAll().stream()
                .map(Commande::getZoneLivraison)
                .filter(z -> z != null && !z.isBlank())
                .forEach(zones::add);
        return new ArrayList<>(zones);
    }

    private List<Commande> chargerCommandes(LocalDateTime debutDtf, LocalDateTime finDtf, Long livreurId) {
        if (livreurId != null) {
            List<Commande> parLivreur = commandeRepository.findByLivreurId(livreurId);
            if (debutDtf == null && finDtf == null) {
                return parLivreur;
            }
            return parLivreur.stream()
                    .filter(c -> dansPeriode(c.getDateCreation(), debutDtf, finDtf))
                    .toList();
        }
        if (debutDtf != null && finDtf != null) {
            return commandeRepository.findByDateCreationBetween(debutDtf, finDtf);
        }
        return commandeRepository.findAll();
    }

    private boolean correspondZone(Commande commande, String zone) {
        if (zone == null || zone.isBlank()) {
            return true;
        }
        return commande.getZoneLivraison() != null
                && commande.getZoneLivraison().equalsIgnoreCase(zone.trim());
    }

    private boolean dansPeriode(LocalDateTime date, LocalDateTime debut, LocalDateTime fin) {
        if (date == null) {
            return false;
        }
        if (debut != null && date.isBefore(debut)) {
            return false;
        }
        return fin == null || !date.isAfter(fin);
    }

    private long count(List<Commande> commandes, StatutCommande statut) {
        return commandes.stream().filter(c -> c.getStatut() == statut).count();
    }

    private BigDecimal somme(List<Commande> commandes, StatutCommande statut, boolean prix) {
        return commandes.stream()
                .filter(c -> c.getStatut() == statut)
                .map(c -> prix ? c.getPrix() : c.getDistance())
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Duree moyenne en heures entre creation et livraison (preuve, sinon mise a jour). */
    private BigDecimal delaiMoyenHeures(List<Commande> commandes) {
        long totalMinutes = 0;
        long echantillons = 0;
        for (Commande commande : commandes) {
            if (commande.getStatut() != StatutCommande.LIVREE || commande.getDateCreation() == null) {
                continue;
            }
            LocalDateTime livraison = commande.getPreuveLivraison() != null
                    && commande.getPreuveLivraison().getDateHeure() != null
                    ? commande.getPreuveLivraison().getDateHeure()
                    : commande.getDateMiseAJour();
            if (livraison == null || livraison.isBefore(commande.getDateCreation())) {
                continue;
            }
            totalMinutes += Duration.between(commande.getDateCreation(), livraison).toMinutes();
            echantillons++;
        }
        if (echantillons == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(echantillons * 60L), 2, RoundingMode.HALF_UP);
    }

    private List<StatistiqueLivreurDto> repartitionParLivreur(List<Commande> commandes) {
        Map<Long, List<Commande>> parLivreur = new LinkedHashMap<>();
        for (Commande commande : commandes) {
            Livreur livreur = commande.getLivreur();
            if (livreur == null || livreur.getId() == null) {
                continue;
            }
            parLivreur.computeIfAbsent(livreur.getId(), id -> new ArrayList<>()).add(commande);
        }

        List<StatistiqueLivreurDto> lignes = new ArrayList<>();
        for (Map.Entry<Long, List<Commande>> entree : parLivreur.entrySet()) {
            List<Commande> commandesLivreur = entree.getValue();
            long livrees = count(commandesLivreur, StatutCommande.LIVREE);
            StatistiqueLivreurDto dto = new StatistiqueLivreurDto(
                    entree.getKey(),
                    nomLivreur(commandesLivreur.get(0).getLivreur()),
                    livrees,
                    somme(commandesLivreur, StatutCommande.LIVREE, true),
                    somme(commandesLivreur, StatutCommande.LIVREE, false),
                    pourcentage(livrees, commandesLivreur.size()));
            lignes.add(dto);
        }
        lignes.sort(Comparator.comparingLong(StatistiqueLivreurDto::getLivraisons)
                .reversed()
                .thenComparing(StatistiqueLivreurDto::getNom, Comparator.nullsLast(String::compareToIgnoreCase)));
        return lignes;
    }

    private String nomLivreur(Livreur livreur) {
        if (livreur == null) {
            return null;
        }
        String nom = livreur.getNom() != null ? livreur.getNom() : "";
        String prenom = livreur.getPrenom() != null ? livreur.getPrenom() : "";
        String complet = (prenom + " " + nom).trim();
        return complet.isEmpty() ? null : complet;
    }

    private BigDecimal pourcentage(long part, long total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
