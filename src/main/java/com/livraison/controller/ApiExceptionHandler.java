package com.livraison.controller;

import com.livraison.exception.PreuveLivraisonIncompleteException;
import com.livraison.exception.TransitionInvalideException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(EntityNotFoundException e) {
        return erreur(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(TransitionInvalideException.class)
    public ResponseEntity<Map<String, Object>> transitionInvalide(TransitionInvalideException e) {
        return erreur(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(PreuveLivraisonIncompleteException.class)
    public ResponseEntity<Map<String, Object>> preuveIncomplete(PreuveLivraisonIncompleteException e) {
        return erreur(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> parametreInvalide(IllegalArgumentException e) {
        return erreur(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private ResponseEntity<Map<String, Object>> erreur(HttpStatus status, String message) {
        String texte = message == null || message.isBlank() ? "Requete invalide" : message;
        return ResponseEntity.status(status)
                .body(Map.of("success", false, "message", texte));
    }
}
