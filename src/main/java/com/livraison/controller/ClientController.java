package com.livraison.controller;

import com.livraison.entity.Client;
import com.livraison.service.ClientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public String list(Model model) {
        List<Client> clients = clientService.findAll();
        model.addAttribute("clients", clients);
        return "client/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        Client client = new Client();
        model.addAttribute("client", client);
        return "client/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Client client, Model model, RedirectAttributes redirectAttributes) {
        client.setNom(client.getNom() == null ? null : client.getNom().trim());
        client.setPrenom(client.getPrenom() == null ? null : client.getPrenom().trim());
        client.setTelephone(client.getTelephone() == null ? null : client.getTelephone().trim());
        client.setEmail(client.getEmail() == null ? null : client.getEmail().trim());
        client.setAdresse(client.getAdresse() == null ? null : client.getAdresse().trim());

        if (client.getNom() == null || client.getNom().isEmpty()
                || client.getPrenom() == null || client.getPrenom().isEmpty()) {
            model.addAttribute("client", client);
            model.addAttribute("errorMessage", "Le nom et le prenom sont obligatoires.");
            return "client/form";
        }
        // Validation d'unicité de l'email
        if (client.getEmail() != null && !client.getEmail().trim().isEmpty()) {
            boolean hasDuplicate = client.getId() == null 
                ? clientService.existsByEmail(client.getEmail()) 
                : clientService.existsByEmailAndIdNot(client.getEmail(), client.getId());
                
            if (hasDuplicate) {
                model.addAttribute("client", client);
                model.addAttribute("errorMessage", "Un client avec cet e-mail existe déjà !");
                return "client/form";
            }
        }
        
        clientService.save(client);
        redirectAttributes.addFlashAttribute("successMessage", "Client enregistré avec succès !");
        return "redirect:/clients";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return clientService.findById(id)
                .map(client -> {
                    model.addAttribute("client", client);
                    return "client/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Client non trouvé !");
                    return "redirect:/clients";
                });
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean deleted = clientService.deleteById(id);
        if (deleted) {
            redirectAttributes.addFlashAttribute("successMessage", "Client supprimé avec succès !");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Impossible de supprimer ce client car il possède des commandes en cours.");
        }
        return "redirect:/clients";
    }
}
