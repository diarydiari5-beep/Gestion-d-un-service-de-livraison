package com.livraison.controller;

import com.livraison.entity.Tarif;
import com.livraison.service.TarifService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Administration des baremes : une ligne par zone, la zone TOUTES servant de
 * repli. Edition en place : toutes les lignes sont renvoyees dans un seul
 * formulaire.
 */
@Controller
@RequestMapping("/tarifs")
public class TarifController {

    /** Recupere tarifs[N].zone pour savoir quelles lignes le formulaire renvoie. */
    private static final Pattern CHAMP_ZONE = Pattern.compile("tarifs\\[(\\d+)]\\.zone");

    private final TarifService tarifService;

    public TarifController(TarifService tarifService) {
        this.tarifService = tarifService;
    }

    @GetMapping
    public String lister(Model model) {
        model.addAttribute("tarifs", tarifService.findAll());
        model.addAttribute("zoneParDefaut", Tarif.ZONE_PAR_DEFAUT);
        model.addAttribute("aucunBareme", tarifService.aucunBaremeConfigure());
        return "tarif/list";
    }

    /**
     * Edition en place : le navigateur renvoie tarifs[0].*, tarifs[1].*...
     * Une liste ne peut pas etre liee directement par @ModelAttribute, chaque
     * ligne est donc reconstruite a partir de ses champs indexes.
     */
    @PostMapping("/save")
    public String enregistrer(@RequestParam Map<String, String> champs,
                              RedirectAttributes redirectAttributes) {
        try {
            List<Tarif> tarifs = new ArrayList<>();
            Map<Integer, String> zonesParIndex = new LinkedHashMap<>();
            for (Map.Entry<String, String> champ : champs.entrySet()) {
                Matcher matcher = CHAMP_ZONE.matcher(champ.getKey());
                if (matcher.matches()) {
                    zonesParIndex.put(Integer.parseInt(matcher.group(1)), champ.getValue());
                }
            }
            for (Map.Entry<Integer, String> ligne : zonesParIndex.entrySet()) {
                int i = ligne.getKey();
                Tarif tarif = new Tarif();
                tarif.setZone(ligne.getValue());
                tarif.setId(longOuNull(champs.get("tarifs[" + i + "].id")));
                tarif.setPrixFixe(montant(champs.get("tarifs[" + i + "].prixFixe")));
                tarif.setPrixParKm(montant(champs.get("tarifs[" + i + "].prixParKm")));
                tarif.setPrixParKg(montant(champs.get("tarifs[" + i + "].prixParKg")));
                tarif.setPrixParM3(montant(champs.get("tarifs[" + i + "].prixParM3")));
                tarif.setMajorationUrgentePct(montant(champs.get("tarifs[" + i + "].majorationUrgentePct")));
                tarif.setMajorationExpressPct(montant(champs.get("tarifs[" + i + "].majorationExpressPct")));
                tarifs.add(tarif);
            }
            tarifService.enregistrerTous(tarifs);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Baremes enregistres !");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/tarifs";
    }

    private static Long longOuNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : Long.valueOf(valeur);
    }

    /** Un champ vide ou absent vaut zero : l'edition en place ne doit pas bloquer. */
    private static BigDecimal montant(String valeur) {
        return valeur == null || valeur.isBlank() ? BigDecimal.ZERO : new BigDecimal(valeur);
    }

    @PostMapping("/nouveau")
    public String nouveau(@ModelAttribute("tarif") Tarif tarif,
                          RedirectAttributes redirectAttributes) {
        try {
            tarifService.enregistrer(tarif);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Bareme " + tarif.getZone() + " ajoute !");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/tarifs";
    }
}