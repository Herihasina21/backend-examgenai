package com.mycompany.examgenai_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.examgenai_backend.dto.openai.GeneratedExamResponseDTO;
import com.mycompany.examgenai_backend.enums.DifficultyLevel;
import com.mycompany.examgenai_backend.enums.QuestionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    private static final int MAX_CHAPTER_CHARS = 12_000;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.6-flash}")
    private String model;

    @Value("${gemini.api-url:https://generativelanguage.googleapis.com/v1beta}")
    private String apiUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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

        var url = apiUrl + "/models/" + model + ":generateContent?key=" + apiKey;

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(body, headers),
                    JsonNode.class
            );

            var responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("Réponse Gemini vide");
            }
            if (responseBody.has("error")) {
                var errorMessage = responseBody.get("error").path("message").asText("Erreur Gemini inconnue");
                throw new RuntimeException("Erreur Gemini : " + errorMessage);
            }
            if (!responseBody.has("candidates") || responseBody.get("candidates").isEmpty()) {
                throw new RuntimeException("Réponse Gemini sans candidat");
            }

            var candidate = responseBody.get("candidates").get(0);
            if (!candidate.has("content") || !candidate.get("content").has("parts") || candidate.get("content").get("parts").isEmpty()) {
                throw new RuntimeException("Réponse Gemini invalide (pas de contenu)");
            }

            var content = candidate.get("content").get("parts").get(0).get("text").asText();
            var generated = objectMapper.readValue(content, GeneratedExamResponseDTO.class);

            if (generated.getQuestions() == null || generated.getQuestions().isEmpty()) {
                throw new RuntimeException("Gemini n'a généré aucune question");
            }

            return generated;
        } catch (RestClientException ex) {
            throw new RuntimeException("Erreur lors de l'appel Gemini : " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Impossible de parser la réponse Gemini : " + ex.getMessage(), ex);
        }
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
