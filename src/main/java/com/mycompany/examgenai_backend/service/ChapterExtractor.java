package com.mycompany.examgenai_backend.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Découpe le cours en chapitres de manière isolée pour permettre des tests JUnit sans Spring ni base de données. */

@Component
public class ChapterExtractor {

    private static final String DEFAULT_CHAPTER_TITLE = "Cours complet";

    // Publics car réutilisés ailleurs (ex. ExamService) pour détecter un chapitre
    // sans contenu exploitable avant d'appeler l'IA. Garder ces deux constantes en
    // phase avec le contenu réellement produit ci-dessous.
    public static final String EMPTY_CONTENT_FALLBACK = "Ce chapitre ne contient pas de texte détectable.";
    public static final String EMPTY_DOCUMENT_FALLBACK = "Aucun contenu n'a pu être extrait de ce document.";

    private static final Pattern CHAPTER_TITLE_PATTERN = Pattern.compile(
            "(?im)^\\s*(Chapitre|CHAPITRE|Chap|CH)\\s*[\\dIVXLC]+\\s*[:\\-]?\\s*(.*)$",
            Pattern.MULTILINE
    );

    public record ExtractedChapter(String title, String content) {
    }

    public List<ExtractedChapter> extract(String rawText) {
        String text = rawText == null ? "" : rawText;

        List<String> titles = new ArrayList<>();
        List<Integer> matchStarts = new ArrayList<>();
        List<Integer> matchEnds = new ArrayList<>();

        Matcher matcher = CHAPTER_TITLE_PATTERN.matcher(text);
        while (matcher.find()) {
            String captured = matcher.group(2) == null ? "" : matcher.group(2).trim();
            titles.add(captured.isEmpty() ? matcher.group(0).trim() : captured);
            matchStarts.add(matcher.start());
            matchEnds.add(matcher.end());
        }

        if (titles.isEmpty()) {
            String fullContent = fallbackIfBlank(text.trim(), EMPTY_DOCUMENT_FALLBACK);
            return List.of(new ExtractedChapter(DEFAULT_CHAPTER_TITLE, fullContent));
        }

        List<ExtractedChapter> chapters = new ArrayList<>();
        for (int i = 0; i < titles.size(); i++) {
            int contentStart = matchEnds.get(i);
            int contentEnd = (i + 1 < titles.size()) ? matchStarts.get(i + 1) : text.length();
            String content = text.substring(contentStart, contentEnd).trim();
            chapters.add(new ExtractedChapter(titles.get(i), fallbackIfBlank(content, EMPTY_CONTENT_FALLBACK)));
        }
        return chapters;
    }

    private String fallbackIfBlank(String value, String fallback) {
        return value.isBlank() ? fallback : value;
    }
}