package com.mycompany.examgenai_backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({InvalidQuestionException.class, BadRequestException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<Map<String, Object>> handleExternalService(ExternalServiceException ex) {
        log.warn("Erreur service externe : {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(ExportException.class)
    public ResponseEntity<Map<String, Object>> handleExport(ExportException ex) {
        log.error("Erreur export", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        String summary = fieldErrors.values().stream()
                .findFirst()
                .orElse("Données invalides.");

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", summary);
        body.put("errors", fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violation d'intégrité : {}", ex.getMessage());
        var detail = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        var haystack = detail == null ? "" : detail.toLowerCase();

        String message;
        if (haystack.contains("chapter_id") && haystack.contains("null")) {
            message = "Impossible d'enregistrer un examen sans chapitre. "
                    + "Exécutez sur la base : ALTER TABLE exams ALTER COLUMN chapter_id DROP NOT NULL;";
        } else if (haystack.contains("foreign key") || haystack.contains("still referenced")) {
            message = "Impossible de supprimer cet élément : d'autres données y sont encore liées.";
        } else {
            message = "Impossible de supprimer cet élément : des examens ou questions y sont encore liés.";
        }

        return buildResponse(HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Erreur inattendue", ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur inattendue s'est produite."
        );
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
