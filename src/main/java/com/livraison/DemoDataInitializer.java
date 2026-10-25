package com.livraison;

import com.livraison.entity.Client;
import com.livraison.entity.Colis;
import com.livraison.entity.Commande;
import com.livraison.entity.Livreur;
import com.livraison.entity.embeddable.PreuveLivraison;
import com.livraison.entity.embeddable.Vehicule;
import com.livraison.entity.enums.PrioriteCommande;
import com.livraison.entity.enums.StatutCommande;
import com.livraison.entity.enums.StatutLivreur;
import com.livraison.repository.ClientRepository;
import com.livraison.repository.CommandeRepository;
import com.livraison.repository.LivreurRepository;
import com.livraison.service.CommandeService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final String MARKER_EMAIL = "demo.fanja.rakotomalala@exemple.mg";

    private final ClientRepository clientRepository;
    private final LivreurRepository livreurRepository;
    private final CommandeRepository commandeRepository;
    private final CommandeService commandeService;

    public DemoDataInitializer(ClientRepository clientRepository,
                               LivreurRepository livreurRepository,
                               CommandeRepository commandeRepository,
                               CommandeService commandeService) {
        this.clientRepository = clientRepository;
        this.livreurRepository = livreurRepository;
        this.commandeRepository = commandeRepository;
        this.commandeService = commandeService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (clientRepository.existsByEmail(MARKER_EMAIL)) {
            log.info("Donnees de demonstration deja presentes");
            return;
        }

        List<Client> clients = List.of(
                client("Rakotomalala", "Fanja", "+261 34 12 345 67", MARKER_EMAIL,
                        "Lot II M 45 Bis Analakely, Antananarivo 101"),
                client("Rasoanaivo", "Tiana", "+261 32 45 678 90", "tiana.rasoanaivo@exemple.mg",
                        "Rue Rainandriamampandry, Antananarivo 101"),
                client("Randrianasolo", "Hanta", "+261 33 78 901 23", "hanta.randrianasolo@exemple.mg",
                        "Immeuble Fiaro, Ampefilo, Antananarivo 101"),
                client("Ratsimbazafy", "Mamy", "+261 34 56 789 01", "mamy.ratsimbazafy@exemple.mg",
                        "Lot IVK 27 Ter Ankadifotsy, Antananarivo 101"),
                client("Andrianina", "Toky", "+261 32 98 765 43", "toky.andrianina@exemple.mg",
                        "Villa Soafaniry, Ivato, Antananarivo 105")
        );

        List<Livreur> livreurs = List.of(
                livreur("Rakotoarisoa", "Naina", "+261 34 11 223 45", "Moto", "25.00", "1842 ABC",
                        "ambatomainty", -18.9089, 47.5241),
                livreur("Rasoanaivo", "Fandresena", "+261 33 22 334 56", "Voiture", "40.00", "5078 BCD",
                        "ivato", -18.9170, 47.5220),
                livreur("Randriamampionona", "Mamy", "+261 32 44 556 67", "Moto", "18.00", "7312 EFG",
                        "anosy", -18.9180, 47.5210),
                livreur("Razafindrakoto", "Tiana", "+261 34 66 778 89", "Velivole", "30.00", "9654 HIJ",
                        "isoraka", -18.9000, 47.5320),
                livreur("Ratsimihandrina", "Haja", "+261 33 87 654 32", "Voiture", "20.00", "2468 KLM",
                        "mahamasina", -18.9080, 47.5160)
        );

        clientRepository.saveAll(clients);
        livreurRepository.saveAll(livreurs);

        List<Commande> commandes = List.of(
                commande("DEMO-CMD-001", clients.get(0),
                        "Analakely, Antananarivo", "Ambatomainty, Antananarivo", "ambatomainty",
                        -18.9100, 47.5250, -18.9089, 47.5241, "3.50", "0.12", "4.80", "28000.00",
                        PrioriteCommande.EXPRESS, 0, "Documents etreth", false),
                commande("DEMO-CMD-002", clients.get(1),
                        "Ambatomainty, Antananarivo", "Ivato, Antananarivo", "ivato",
                        -18.9089, 47.5241, -18.9170, 47.5220, "8.00", "0.35", "7.20", "42000.00",
                        PrioriteCommande.URGENTE, 1, "Colis pharmaceuticals", true),
                commande("DEMO-CMD-003", clients.get(2),
                        "Anosy, Antananarivo", "Analakely, Antananarivo", "ambatomainty",
                        -18.9180, 47.5210, -18.9100, 47.5250, "2.00", "0.08", "5.10", "22000.00",
                        PrioriteCommande.NORMALE, 1, "Vetements", false),
                commande("DEMO-CMD-004", clients.get(3),
                        "Isoraka, Antananarivo", "Mahamasina, Antananarivo", "mahamasina",
                        -18.9000, 47.5320, -18.9080, 47.5160, "5.50", "0.25", "6.30", "35000.00",
                        PrioriteCommande.NORMALE, 2, "Materiel informatique", true),
                commande("DEMO-CMD-005", clients.get(4),
                        "Ivato, Antananarivo", "Mahamasina, Antananarivo", "mahamasina",
                        -18.9170, 47.5220, -18.9080, 47.5160, "1.50", "0.05", "4.20", "18000.00",
                        PrioriteCommande.NORMALE, 2, "Accessoires", false),
                commande("DEMO-CMD-006", clients.get(1),
                        "Analakely, Antananarivo", "Mahamasina, Antananarivo", "mahamasina",
                        -18.9100, 47.5250, -18.9080, 47.5160, "4.00", "0.15", "5.90", "31000.00",
                        PrioriteCommande.EXPRESS, 0, "Produits alimentaires", false),
                commande("DEMO-CMD-007", clients.get(3),
                        "Ankadifotsy, Antananarivo", "Analakely, Antananarivo", "ambatomainty",
                        -18.8890, 47.5400, -18.9100, 47.5250, "1.00", "0.04", "6.70", "15000.00",
                        PrioriteCommande.NORMALE, 3, "Document administratif", false),
                commande("DEMO-CMD-008", clients.get(4),
                        "Ivato, Antananarivo", "Analakely, Antananarivo", "ambatomainty",
                        -18.9170, 47.5220, -18.9100, 47.5250, "6.00", "0.30", "8.10", "45000.00",
                        PrioriteCommande.URGENTE, 2, "Piece automobile", true)
        );

        commandeRepository.saveAll(commandes);

        Livreur naina = livreurs.get(0);
        Livreur fandresena = livreurs.get(1);
        Livreur mamyLivreur = livreurs.get(2);
        Livreur tianaLivreur = livreurs.get(3);
        Livreur hajaLivreur = livreurs.get(4);

        Commande livree1 = commandes.get(0);
        commandeService.assigner(livree1.getId(), naina);
        commandeService.changerStatut(livree1.getId(), StatutCommande.PRISE_EN_CHARGE);
        commandeService.changerStatut(livree1.getId(), StatutCommande.EN_TRANSIT);
        commandeService.changerStatut(livree1.getId(), StatutCommande.LIVREE,
                new PreuveLivraison(LocalDateTime.now().minusHours(2), "Fanja Rakotomalala", "CONF-001",
                        "Colis remis a la reception"));

        Commande enTransit = commandes.get(1);
        commandeService.assigner(enTransit.getId(), fandresena);
        commandeService.changerStatut(enTransit.getId(), StatutCommande.PRISE_EN_CHARGE);
        commandeService.changerStatut(enTransit.getId(), StatutCommande.EN_TRANSIT);

        Commande priseEnCharge = commandes.get(2);
        commandeService.assigner(priseEnCharge.getId(), mamyLivreur);
        commandeService.changerStatut(priseEnCharge.getId(), StatutCommande.PRISE_EN_CHARGE);

        commandeService.assigner(commandes.get(3).getId(), tianaLivreur);

        Commande livree2 = commandes.get(5);
        commandeService.assigner(livree2.getId(), hajaLivreur);
        commandeService.changerStatut(livree2.getId(), StatutCommande.PRISE_EN_CHARGE);
        commandeService.changerStatut(livree2.getId(), StatutCommande.EN_TRANSIT);
        commandeService.changerStatut(livree2.getId(), StatutCommande.LIVREE,
                new PreuveLivraison(LocalDateTime.now().minusHours(1), "Tiana Rasoanaivo", "CONF-002",
                        "Livraison confirmee"));

        commandeService.changerStatut(commandes.get(6).getId(), StatutCommande.ANNULEE);

        log.info("Donnees de demonstration creees : {} clients, {} livreurs, {} commandes",
                clients.size(), livreurs.size(), commandes.size());
    }

    private Client client(String nom, String prenom, String telephone, String email, String adresse) {
        Client client = new Client();
        client.setNom(nom);
        client.setPrenom(prenom);
        client.setTelephone(telephone);
        client.setEmail(email);
        client.setAdresse(adresse);
        return client;
    }

    private Livreur livreur(String nom, String prenom, String telephone, String typeVehicule,
                           String capaciteVehicule, String immatriculation, String zoneActivite,
                           double latitude, double longitude) {
        Livreur livreur = new Livreur();
        livreur.setNom(nom);
        livreur.setPrenom(prenom);
        livreur.setTelephone(telephone);
        livreur.setVehicule(new Vehicule(typeVehicule, new BigDecimal(capaciteVehicule), immatriculation));
        livreur.setZoneActivite(zoneActivite);
        livreur.setLatitude(latitude);
        livreur.setLongitude(longitude);
        livreur.setStatut(StatutLivreur.DISPONIBLE);
        livreur.setCapaciteMax(new BigDecimal(capaciteVehicule));
        return livreur;
    }

    private Commande commande(String numero, Client client, String adresseEnlevement,
                              String adresseLivraison, String zoneLivraison,
                              double latitudeEnlevement, double longitudeEnlevement,
                              double latitudeLivraison, double longitudeLivraison,
                              String poids, String volume, String distance, String prix,
                              PrioriteCommande priorite, int joursPrevus, String descriptionColis,
                              boolean fragile) {
        Commande commande = new Commande();
        commande.setNumero(numero);
        commande.setClient(client);
        commande.setAdresseEnlevement(adresseEnlevement);
        commande.setAdresseLivraison(adresseLivraison);
        commande.setZoneLivraison(zoneLivraison);
        commande.setLatitudeEnlevement(latitudeEnlevement);
        commande.setLongitudeEnlevement(longitudeEnlevement);
        commande.setLatitudeLivraison(latitudeLivraison);
        commande.setLongitudeLivraison(longitudeLivraison);
        commande.setPoids(new BigDecimal(poids));
        commande.setVolume(new BigDecimal(volume));
        commande.setDistance(new BigDecimal(distance));
        commande.setPrix(new BigDecimal(prix));
        commande.setPriorite(priorite);
        commande.setDatePrevue(LocalDateTime.now().plusDays(joursPrevus));

        Colis colis = new Colis();
        colis.setCommande(commande);
        colis.setDescription(descriptionColis);
        colis.setPoids(new BigDecimal(poids));
        colis.setVolume(new BigDecimal(volume));
        colis.setFragile(fragile);
        commande.getColis().add(colis);
        return commande;
    }
}
