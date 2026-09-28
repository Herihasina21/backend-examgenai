package com.mycompany.examgenai_backend.service.export;

import com.mycompany.examgenai_backend.entity.Exam;

import java.text.Normalizer;

/**
 * Formatage partagé pour les exports PDF / Word.
 */
public final class ExportFormatUtils {

    private ExportFormatUtils() {
    }

    /**
     * Affiche la durée stockée en minutes au format lisible :
     * 45 → "45 min", 90 → "01 h 30", 60 → "01 h 00".
     */
    public static String formatDuration(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            return "—";
        }
        var hours = durationMinutes / 60;
        var minutes = durationMinutes % 60;
        if (hours <= 0) {
            return minutes + " min";
        }
        return String.format("%02d h %02d", hours, minutes);
    }

    public static String resolveCourseTitle(Exam exam) {
        if (exam == null) {
            return "Cours";
        }
        if (exam.getCourse() != null && exam.getCourse().getTitle() != null
                && !exam.getCourse().getTitle().isBlank()) {
            return exam.getCourse().getTitle();
        }
        if (exam.getChapter() != null
                && exam.getChapter().getCourse() != null
                && exam.getChapter().getCourse().getTitle() != null
                && !exam.getChapter().getCourse().getTitle().isBlank()) {
            return exam.getChapter().getCourse().getTitle();
        }
        return "Cours";
    }

    /**
     * Normalise apostrophes / tirets typographiques pour Helvetica (WinAnsi),
     * afin d'éviter les "?" dans le PDF.
     */
    public static String sanitizeForPdf(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        var normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201A', '\'')
                .replace('\u201B', '\'')
                .replace('\u2032', '\'')
                .replace('\u201C', '"')
                .replace('\u201D', '"')
                .replace('\u201E', '"')
                .replace('\u00AB', '"')
                .replace('\u00BB', '"')
                .replace("\u2013", "-")
                .replace("\u2014", "-")
                .replace("\u2212", "-")
                .replace("\u2026", "...")
                .replace('\u00A0', ' ')
                .replace('\u202F', ' ')
                .replace('\u2009', ' ');

        var sb = new StringBuilder(normalized.length());
        for (var i = 0; i < normalized.length(); i++) {
            var c = normalized.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t') {
                sb.append(' ');
            } else if (c >= 32 && c <= 255) {
                sb.append(c);
            } else {
                // Dernier recours : retirer les diacritiques hors Latin-1
                var stripped = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}+", "");
                if (!stripped.isEmpty()) {
                    var fallback = stripped.charAt(0);
                    sb.append(fallback <= 255 ? fallback : '?');
                }
            }
        }
        return sb.toString().trim().replaceAll(" +", " ");
    }
}
