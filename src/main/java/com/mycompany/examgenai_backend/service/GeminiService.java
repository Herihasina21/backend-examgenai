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

    private static final String MSG_OVERLOAD =
            "L'IA est temporairement saturée. Réessayez dans quelques minutes.";
    private static final String MSG_ALL_FAILED =
            "Aucun modèle Gemini n'est disponible pour le moment. Réessayez plus tard.";
    private static final String MSG_AUTH =
            "Clé API Gemini invalide ou non autorisée. Vérifiez la configuration.";
    private static final String MSG_PARSE =
            "L'IA a renvoyé une réponse inutilisable. Réessayez la génération.";
    private static final String MSG_EMPTY =
            "L'IA n'a généré aucune question. Réessayez avec un autre chapitre ou moins de questions.";
    private static final String MSG_NETWORK =
            "Impossible de joindre le service Gemini. Vérifiez votre connexion et réessayez.";

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.models:gemini-2.5-flash-lite,gemini-flash-lite-latest,gemini-3.1-flash-lite,gemini-3.5-flash-lite}")
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
                    throw new ExternalServiceException("Réponse vide du service d'IA. Réessayez.");
                }
                if (responseBody.has("error")) {
                    var errorNode = responseBody.get("error");
                    var errorMessage = errorNode.path("message").asText("");
                    var errorStatus = errorNode.path("status").asText("");
                    var errorCode = errorNode.path("code").asInt(0);
                    var userMessage = mapApiError(errorCode, errorStatus, errorMessage);
                    var failure = new ExternalServiceException(userMessage);
                    if (hasNext && isRetryable(errorCode, errorStatus, errorMessage)) {
                        log.warn("Gemini modèle {} indisponible ({}), essai du suivant", model,
                                errorStatus.isBlank() ? errorCode : errorStatus);
                        lastFailure = failure;
                        continue;
                    }
                    throw failure;
                }
                if (!responseBody.has("candidates") || responseBody.get("candidates").isEmpty()) {
                    throw new ExternalServiceException(MSG_EMPTY);
                }

                var candidate = responseBody.get("candidates").get(0);
                if (!candidate.has("content")
                        || !candidate.get("content").has("parts")
                        || candidate.get("content").get("parts").isEmpty()) {
                    throw new ExternalServiceException(MSG_PARSE);
                }

                var content = candidate.get("content").get("parts").get(0).get("text").asText();
                var generated = objectMapper.readValue(content, GeneratedExamResponseDTO.class);

                if (generated.getQuestions() == null || generated.getQuestions().isEmpty()) {
                    throw new ExternalServiceException(MSG_EMPTY);
                }

                if (i > 0) {
                    log.info("Génération Gemini réussie avec le modèle de secours {}", model);
                }
                return generated;
            } catch (RestClientResponseException ex) {
                lastFailure = ex;
                var status = ex.getStatusCode().value();
                var bodyText = ex.getResponseBodyAsString();
                if (hasNext && isRetryable(status, null, bodyText)) {
                    log.warn("Gemini modèle {} a renvoyé HTTP {}, essai du suivant", model, status);
                    continue;
                }
                throw new ExternalServiceException(mapHttpError(status, bodyText), ex);
            } catch (RestClientException ex) {
                throw new ExternalServiceException(MSG_NETWORK, ex);
            } catch (ExternalServiceException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new ExternalServiceException(MSG_PARSE, ex);
            }
        }

        throw new ExternalServiceException(MSG_ALL_FAILED, lastFailure);
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
            throw new ExternalServiceException("Aucun modèle Gemini configuré. Vérifiez gemini.models.");
        }
        return resolved;
    }

    private String mapHttpError(int status, String body) {
        if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
            return MSG_AUTH;
        }
        if (isRetryable(status, null, body)) {
            return MSG_OVERLOAD;
        }
        return "La génération a échoué. Réessayez dans quelques instants.";
    }

    private String mapApiError(int code, String status, String message) {
        if (code == HttpStatus.UNAUTHORIZED.value() || code == HttpStatus.FORBIDDEN.value()
                || "PERMISSION_DENIED".equalsIgnoreCase(status)
                || "UNAUTHENTICATED".equalsIgnoreCase(status)) {
            return MSG_AUTH;
        }
        if (isRetryable(code, status, message)) {
            return MSG_OVERLOAD;
        }
        if (message != null && !message.isBlank() && message.length() < 180) {
            return "Erreur Gemini : " + message;
        }
        return "La génération a échoué. Réessayez dans quelques instants.";
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
