package com.mycompany.examgenai_backend.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFound_renvoieMessage() {
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(
                new ResourceNotFoundException("Examen introuvable")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Examen introuvable", response.getBody().get("message"));
    }

    @Test
    void handleBadRequest_renvoieMessage() {
        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(
                new BadRequestException("Données invalides")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Données invalides", response.getBody().get("message"));
    }

    @Test
    void handleExternalService_renvoie502() {
        ResponseEntity<Map<String, Object>> response = handler.handleExternalService(
                new ExternalServiceException("Erreur Gemini")
        );

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("Erreur Gemini", response.getBody().get("message"));
    }
}
