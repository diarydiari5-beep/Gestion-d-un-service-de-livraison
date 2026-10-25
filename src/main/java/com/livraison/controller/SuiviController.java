package com.livraison.controller;

import com.livraison.dto.AlerteSuivi;
import com.livraison.dto.SuiviLigneDto;
import com.livraison.service.SuiviService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SuiviController {

    private static final DateTimeFormatter FORMAT_HEURE =
            DateTimeFormatter.ofPattern("HH:mm:ss", Locale.FRANCE).withZone(ZoneId.systemDefault());

    private final SuiviService suiviService;

    public SuiviController(SuiviService suiviService) {
        this.suiviService = suiviService;
    }

    @GetMapping("/tableau-bord")
    public String tableauBord(Model model) {
        List<SuiviLigneDto> lignes = suiviService.lignes();

        long enRetard = lignes.stream().filter(l -> l.getAlertes().contains(AlerteSuivi.RETARD)).count();
        long livreursMobilises = lignes.stream()
                .map(SuiviLigneDto::getLivreurNom)
                .filter(nom -> nom != null)
                .distinct()
                .count();
        long avecIncident = lignes.stream().filter(l -> l.getIncidentsOuverts() > 0).count();

        model.addAttribute("lignes", lignes);
        model.addAttribute("enCours", lignes.size());
        model.addAttribute("enRetard", enRetard);
        model.addAttribute("livreursMobilises", livreursMobilises);
        model.addAttribute("avecIncident", avecIncident);
        model.addAttribute("dernierCalcul",
                suiviService.getDernierCalcul() == null ? null : FORMAT_HEURE.format(suiviService.getDernierCalcul()));
        return "suivi/tableau";
    }
}
