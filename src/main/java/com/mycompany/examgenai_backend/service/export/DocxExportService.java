package com.mycompany.examgenai_backend.service.export;

import com.mycompany.examgenai_backend.entity.Exam;
import com.mycompany.examgenai_backend.entity.Question;
import com.mycompany.examgenai_backend.exception.ResourceNotFoundException;
import com.mycompany.examgenai_backend.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service de génération d'export Word (.docx) d'un examen, via Apache POI.
 * Produit un document avec :
 *  - une page de garde (titre, chapitre, date, durée)
 *  - la liste numérotée des questions avec espace de réponse
 *  - une page de corrigé séparée (réponses + explications)
 */
@Service
@RequiredArgsConstructor
public class DocxExportService {

    private final ExamRepository examRepository;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Génère le fichier Word d'un examen et retourne son contenu binaire.
     */
    public byte[] generateExamDocx(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Exam introuvable avec id: " + examId));

        try (XWPFDocument document = new XWPFDocument()) {

            addCoverPage(document, exam);
            addQuestionsSection(document, exam.getQuestions());
            addAnswerKeySection(document, exam.getQuestions());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du document Word", e);
        }
    }

    /**
     * Ajoute la page de garde : titre, métadonnées, puis saut de page.
     */
    private void addCoverPage(XWPFDocument document, Exam exam) {
        // Espace vertical avant le titre pour centrer visuellement sur la page
        for (int i = 0; i < 6; i++) {
            document.createParagraph();
        }

        XWPFParagraph titleParagraph = document.createParagraph();
        titleParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titleParagraph.createRun();
        titleRun.setText(exam.getTitle());
        titleRun.setBold(true);
        titleRun.setFontSize(28);
        titleRun.setFontFamily("Calibri");

        XWPFParagraph subtitleParagraph = document.createParagraph();
        subtitleParagraph.setAlignment(ParagraphAlignment.CENTER);
        subtitleParagraph.setSpacingBefore(300);
        XWPFRun subtitleRun = subtitleParagraph.createRun();
        subtitleRun.setText("Examen genere par ExamGenAI");
        subtitleRun.setItalic(true);
        subtitleRun.setFontSize(14);
        subtitleRun.setColor("666666");

        String chapterName = exam.getChapter() != null
                ? exam.getChapter().getTitle()
                : "Non specifie";
        String dateStr = exam.getCreatedAt() != null
                ? exam.getCreatedAt().format(DATE_FORMATTER)
                : "Non specifiee";

        XWPFParagraph metaParagraph = document.createParagraph();
        metaParagraph.setAlignment(ParagraphAlignment.CENTER);
        metaParagraph.setSpacingBefore(600);
        addMetaLine(metaParagraph, "Chapitre : " + chapterName);
        addMetaLineBreak(metaParagraph);
        addMetaLine(metaParagraph, "Date : " + dateStr);
        addMetaLineBreak(metaParagraph);
        addMetaLine(metaParagraph, "Duree : " + exam.getDurationMinutes() + " minutes");
        addMetaLineBreak(metaParagraph);
        addMetaLine(metaParagraph, "Nombre de questions : " + exam.getQuestions().size());

        // Saut de page vers la section questions
        XWPFParagraph pageBreak = document.createParagraph();
        pageBreak.createRun().addBreak(BreakType.PAGE);
    }

    private void addMetaLine(XWPFParagraph paragraph, String text) {
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(12);
    }

    private void addMetaLineBreak(XWPFParagraph paragraph) {
        paragraph.createRun().addBreak();
    }

    /**
     * Ajoute la section des questions (sans les réponses).
     */
    private void addQuestionsSection(XWPFDocument document, List<Question> questions) {
        XWPFParagraph sectionTitle = document.createParagraph();
        XWPFRun sectionTitleRun = sectionTitle.createRun();
        sectionTitleRun.setText("Questions");
        sectionTitleRun.setBold(true);
        sectionTitleRun.setFontSize(18);
        sectionTitle.setSpacingAfter(200);

        int index = 1;
        for (Question question : questions) {
            addSingleQuestion(document, question, index);
            index++;
        }

        // Saut de page vers le corrigé
        XWPFParagraph pageBreak = document.createParagraph();
        pageBreak.createRun().addBreak(BreakType.PAGE);
    }

    private void addSingleQuestion(XWPFDocument document, Question question, int index) {
        XWPFParagraph statementParagraph = document.createParagraph();
        statementParagraph.setSpacingBefore(200);
        XWPFRun statementRun = statementParagraph.createRun();
        statementRun.setText(index + ". " + question.getStatement()
                + "  (" + question.getPoints() + " pt)");
        statementRun.setBold(true);
        statementRun.setFontSize(12);

        switch (question.getQuestionType()) {
            case QCM -> addOptions(document, question.getOptions());
            case TRUE_FALSE -> addTrueFalseOptions(document);
            case OPEN, FILL_IN_BLANK -> addAnswerLines(document);
        }
    }

    private void addOptions(XWPFDocument document, List<String> options) {
        char letter = 'a';
        for (String option : options) {
            XWPFParagraph optionParagraph = document.createParagraph();
            optionParagraph.setIndentationLeft(400);
            XWPFRun optionRun = optionParagraph.createRun();
            optionRun.setText(letter + ") " + option);
            optionRun.setFontSize(11);
            letter++;
        }
    }

    private void addTrueFalseOptions(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setIndentationLeft(400);
        XWPFRun run = paragraph.createRun();
        run.setText("[ ] Vrai        [ ] Faux");
        run.setFontSize(11);
    }

    /**
     * Lignes vides pour la réponse d'une question ouverte (bordure basse simulée par soulignés).
     */
    private void addAnswerLines(XWPFDocument document) {
        for (int i = 0; i < 3; i++) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setIndentationLeft(400);
            paragraph.setBorderBottom(Borders.SINGLE);
            paragraph.createRun().setText(" ");
        }
    }

    /**
     * Ajoute la page de corrigé : réponses correctes et explications.
     */
    private void addAnswerKeySection(XWPFDocument document, List<Question> questions) {
        XWPFParagraph sectionTitle = document.createParagraph();
        XWPFRun sectionTitleRun = sectionTitle.createRun();
        sectionTitleRun.setText("Corrige");
        sectionTitleRun.setBold(true);
        sectionTitleRun.setFontSize(18);
        sectionTitle.setSpacingAfter(200);

        int index = 1;
        for (Question question : questions) {
            XWPFParagraph answerParagraph = document.createParagraph();
            answerParagraph.setSpacingBefore(150);

            XWPFRun numberRun = answerParagraph.createRun();
            numberRun.setText(index + ". Reponse : ");
            numberRun.setBold(true);
            numberRun.setFontSize(12);

            XWPFRun answerRun = answerParagraph.createRun();
            answerRun.setText(question.getCorrectAnswer());
            answerRun.setFontSize(12);

            if (question.getExplanation() != null && !question.getExplanation().isBlank()) {
                XWPFParagraph explanationParagraph = document.createParagraph();
                explanationParagraph.setIndentationLeft(400);
                XWPFRun explanationRun = explanationParagraph.createRun();
                explanationRun.setText("Explication : " + question.getExplanation());
                explanationRun.setItalic(true);
                explanationRun.setFontSize(10);
                explanationRun.setColor("555555");
            }

            index++;
        }
    }
}