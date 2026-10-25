package com.livraison.controller;

import com.livraison.dto.StatistiquesDto;
import com.livraison.entity.Livreur;
import com.livraison.repository.LivreurRepository;
import com.livraison.service.StatistiquesService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StatistiquesController {

    private final StatistiquesService statistiquesService;
    private final LivreurRepository livreurRepository;

    public StatistiquesController(StatistiquesService statistiquesService,
                                  LivreurRepository livreurRepository) {
        this.statistiquesService = statistiquesService;
        this.livreurRepository = livreurRepository;
    }

    @GetMapping("/statistiques")
    public String statistiques(
            @RequestParam(required = false) String periode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Long livreurId,
            @RequestParam(required = false) String zone,
            Model model) {

        String periodeRetenue = (periode == null || periode.isBlank()) ? "mois" : periode;
        boolean personnalise = debut != null || fin != null;
        if (!personnalise) {
            LocalDate aujourdHui = LocalDate.now();
            switch (periodeRetenue) {
                case "jour" -> {
                    debut = aujourdHui;
                    fin = aujourdHui;
                }
                case "semaine" -> {
                    debut = aujourdHui.with(DayOfWeek.MONDAY);
                    fin = aujourdHui;
                }
                case "tout" -> {
                    debut = null;
                    fin = null;
                }
                default -> {
                    debut = aujourdHui.withDayOfMonth(1);
                    fin = aujourdHui;
                    periodeRetenue = "mois";
                }
            }
        }

        StatistiquesDto statistiques = statistiquesService.calculer(debut, fin, livreurId, zone);
        List<Livreur> livreurs = livreurRepository.findAll().stream()
                .sorted(Comparator.comparing(Livreur::getNom, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(Livreur::getPrenom, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        model.addAttribute("statistiques", statistiques);
        model.addAttribute("livreurs", livreurs);
        model.addAttribute("zones", statistiquesService.zonesDisponibles());
        model.addAttribute("periode", periodeRetenue);
        model.addAttribute("debut", debut);
        model.addAttribute("fin", fin);
        model.addAttribute("livreurSelectionne", livreurId);
        model.addAttribute("zoneSelectionnee", zone);
        model.addAttribute("personnalise", personnalise);
        return "statistiques/list";
    }
}
