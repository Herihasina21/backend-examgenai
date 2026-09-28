package com.mycompany.examgenai_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.examgenai_backend.dto.gemini.GeneratedExamResponseDTO;
import com.mycompany.examgenai_backend.enums.DifficultyLevel;
import com.mycompany.examgenai_backend.enums.QuestionType;
import com.mycompany.examgenai_backend.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private static final int MAX_CHAPTER_CHARS = 12_000;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.models:gemini-2.5-flash,gemini-2.0-flash,gemini-1.5-flash,gemini-1.5-flash-8b}")
    private List<String> models;

    @Value("${gemini.api-url:https://generativelanguage.googleapis.com/v1beta}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeneratedExamResponseDTO generateQuestions(
            String chapterTitle,
            String chapterContent,
            int numberOfQuestions,
            List<QuestionType> questionTypes,
            DifficultyLevel difficultyLevel) {

        var trimmedContent = truncate(chapterContent);
        var typesLabel = questionTypes.stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));

        var systemPrompt = """
                Tu es un professeur qui crée des examens. Réponds UNIQUEMENT avec un JSON valide, sans markdown.
                Format exact :
                {
                  "questions": [
                    {
                      "questionText": "texte de la question",
                      "questionType": "QCM",
                      "points": 2,
                      "difficultyLevel": "MEDIUM",
                      "answers": [
                        { "answerText": "réponse A", "isCorrect": false, "answerOrder": 1 },
                        { "answerText": "réponse B", "isCorrect": true, "answerOrder": 2 }
                      ]
                    }
                  ]
                }
                Règles :
                - questionType : QCM, TRUE_FALSE, OPEN ou FILL_IN_BLANK
                - QCM : 4 réponses, une seule isCorrect true
                - TRUE_FALSE : 2 réponses (Vrai/Faux), une seule isCorrect true
                - OPEN ou FILL_IN_BLANK : answers vide ou une réponse modèle avec isCorrect true
                - difficultyLevel : EASY, MEDIUM ou HARD
                """;

        var userPrompt = String.format("""
                Chapitre : %s
                Nombre de questions : %d
                Types autorisés : %s
                Niveau de difficulté : %s

                Contenu du chapitre :
                %s
                """, chapterTitle, numberOfQuestions, typesLabel, difficultyLevel.name(), trimmedContent);

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        body.put("contents", List.of(
                Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))
        ));
        body.put("generationConfig", Map.of("responseMimeType", "application/json"));

        var modelCandidates = resolveModels();
        Exception lastFailure = null;

        for (var i = 0; i < modelCandidates.size(); i++) {
            var model = modelCandidates.get(i);
            var url = apiUrl + "/models/" + model + ":generateContent?key=" + apiKey;
            var hasNext = i < modelCandidates.size() - 1;

            try {
                ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                        url,
                        new HttpEntity<>(body, headers),
                        JsonNode.class
                );

                var responseBody = response.getBody();
                if (responseBody == null) {
                    throw new ExternalServiceException("Réponse Gemini vide");
                }
                if (responseBody.has("error")) {
                    var errorNode = responseBody.get("error");
                    var errorMessage = errorNode.path("message").asText("Erreur Gemini inconnue");
                    var errorStatus = errorNode.path("status").asText("");
                    var errorCode = errorNode.path("code").asInt(0);
                    var failure = new ExternalServiceException("Erreur Gemini : " + errorMessage);
                    if (hasNext && isRetryable(errorCode, errorStatus, errorMessage)) {
                        log.warn("Gemini modèle {} indisponible ({}), essai du suivant", model, errorStatus.isBlank() ? errorCode : errorStatus);
                        lastFailure = failure;
                        continue;
                    }
                    throw failure;
                }
                if (!responseBody.has("candidates") || responseBody.get("candidates").isEmpty()) {
                    throw new ExternalServiceException("Réponse Gemini sans candidat");
                }

                var candidate = responseBody.get("candidates").get(0);
                if (!candidate.has("content")
                        || !candidate.get("content").has("parts")
                        || candidate.get("content").get("parts").isEmpty()) {
                    throw new ExternalServiceException("Réponse Gemini invalide (pas de contenu)");
                }

                var content = candidate.get("content").get("parts").get(0).get("text").asText();
                var generated = objectMapper.readValue(content, GeneratedExamResponseDTO.class);

                if (generated.getQuestions() == null || generated.getQuestions().isEmpty()) {
                    throw new ExternalServiceException("Gemini n'a généré aucune question");
                }

                if (i > 0) {
                    log.info("Génération Gemini réussie avec le modèle de secours {}", model);
                }
                return generated;
            } catch (RestClientResponseException ex) {
                lastFailure = ex;
                if (hasNext && isRetryable(ex.getStatusCode().value(), null, ex.getResponseBodyAsString())) {
                    log.warn("Gemini modèle {} a renvoyé HTTP {}, essai du suivant", model, ex.getStatusCode().value());
                    continue;
                }
                throw new ExternalServiceException("Erreur lors de l'appel Gemini : " + ex.getMessage(), ex);
            } catch (RestClientException ex) {
                throw new ExternalServiceException("Erreur lors de l'appel Gemini : " + ex.getMessage(), ex);
            } catch (ExternalServiceException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new ExternalServiceException("Impossible de parser la réponse Gemini : " + ex.getMessage(), ex);
            }
        }

        throw new ExternalServiceException("Tous les modèles Gemini ont échoué", lastFailure);
    }

    private List<String> resolveModels() {
        var resolved = new ArrayList<String>();
        if (models != null) {
            for (var model : models) {
                if (model == null) {
                    continue;
                }
                var trimmed = model.trim();
                if (!trimmed.isEmpty() && !resolved.contains(trimmed)) {
                    resolved.add(trimmed);
                }
            }
        }
        if (resolved.isEmpty()) {
            throw new ExternalServiceException("Aucun modèle Gemini configuré (gemini.models)");
        }
        return resolved;
    }

    private boolean isRetryable(int httpOrApiCode, String status, String bodyOrMessage) {
        if (httpOrApiCode == HttpStatus.TOO_MANY_REQUESTS.value()
                || httpOrApiCode == HttpStatus.SERVICE_UNAVAILABLE.value()
                || httpOrApiCode == HttpStatus.NOT_FOUND.value()
                || httpOrApiCode == HttpStatus.BAD_GATEWAY.value()
                || httpOrApiCode == HttpStatus.GATEWAY_TIMEOUT.value()) {
            return true;
        }
        var haystack = ((status == null ? "" : status) + " " + (bodyOrMessage == null ? "" : bodyOrMessage))
                .toUpperCase();
        return haystack.contains("UNAVAILABLE")
                || haystack.contains("RESOURCE_EXHAUSTED")
                || haystack.contains("HIGH DEMAND")
                || haystack.contains("NOT_FOUND")
                || haystack.contains("NO LONGER AVAILABLE");
    }

    private String truncate(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= MAX_CHAPTER_CHARS) {
            return content;
        }
        return content.substring(0, MAX_CHAPTER_CHARS);
    }
}
