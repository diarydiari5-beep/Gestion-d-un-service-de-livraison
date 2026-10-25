package com.livraison.controller;

import com.livraison.dto.LivreurPositionDto;
import com.livraison.dto.PositionUpdateRequest;
import com.livraison.entity.Livreur;
import com.livraison.service.LivreurService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LivreurPositionController {

    private final LivreurService livreurService;

    public LivreurPositionController(LivreurService livreurService) {
        this.livreurService = livreurService;
    }

    @PostMapping({"/livreurs/{id}/position", "/api/livreurs/{id}/position"})
    public ResponseEntity<Map<String, Object>> updatePosition(@PathVariable Long id,
                                                              @RequestBody PositionUpdateRequest request) {
        return livreurService.findById(id)
                .map(livreur -> {
                    livreur.setLatitude(request.latitude());
                    livreur.setLongitude(request.longitude());
                    livreurService.save(livreur);
                    return ResponseEntity.ok(Map.<String, Object>of("success", true));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/livreurs/positions")
    public List<LivreurPositionDto> positions() {
        return livreurService.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    private LivreurPositionDto toDto(Livreur livreur) {
        return new LivreurPositionDto(
                livreur.getId(),
                livreur.getNom(),
                livreur.getPrenom(),
                livreur.getLatitude(),
                livreur.getLongitude(),
                livreur.getStatut() != null ? livreur.getStatut().name() : null,
                livreur.getZoneActivite());
    }
}