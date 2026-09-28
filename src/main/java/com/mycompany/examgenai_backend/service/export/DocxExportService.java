package com.mycompany.examgenai_backend.service.export;

import com.mycompany.examgenai_backend.entity.Exam;
import com.mycompany.examgenai_backend.entity.Question;
import com.mycompany.examgenai_backend.exception.ExportException;
import com.mycompany.examgenai_backend.exception.ResourceNotFoundException;
import com.mycompany.examgenai_backend.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static com.mycompany.examgenai_backend.service.export.ExportFormatUtils.formatDuration;
import static com.mycompany.examgenai_backend.service.export.ExportFormatUtils.resolveCourseTitle;

@Service
@RequiredArgsConstructor
public class DocxExportService {

    private final ExamRepository examRepository;

    @Transactional(readOnly = true)
    public byte[] generateExamDocx(Long examId) {
        Exam exam = examRepository.findByIdWithChapterAndQuestions(examId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Exam introuvable avec id: " + examId));

        try (XWPFDocument document = new XWPFDocument()) {

            addFramedHeader(document, exam);
            addQuestionsSection(document, exam.getQuestions());
            addAnswerKeySection(document, exam.getQuestions());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new ExportException("Erreur lors de la génération du document Word", e);
        }
    }

    private void addFramedHeader(XWPFDocument document, Exam exam) {
        var durationLabel = "Durée : " + formatDuration(exam.getDurationMinutes());
        var headerTitle = "Examen - " + resolveCourseTitle(exam);

        XWPFTable table = document.createTable(1, 2);
        table.setWidth("100%");

        XWPFTableRow row = table.getRow(0);
        row.setHeight(900);

        XWPFTableCell left = row.getCell(0);
        XWPFTableCell right = row.getCell(1);

        styleHeaderCell(left, 7200);
        styleHeaderCell(right, 2800);

        setCellText(left, headerTitle, true, 13, ParagraphAlignment.CENTER);
        setCellText(right, durationLabel, true, 11, ParagraphAlignment.CENTER);

        document.createParagraph();
    }

    private void styleHeaderCell(XWPFTableCell cell, long widthTwips) {
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        var borders = tcPr.isSetTcBorders() ? tcPr.getTcBorders() : tcPr.addNewTcBorders();
        setBorder(borders.isSetTop() ? borders.getTop() : borders.addNewTop());
        setBorder(borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom());
        setBorder(borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft());
        setBorder(borders.isSetRight() ? borders.getRight() : borders.addNewRight());
        if (!tcPr.isSetVAlign()) {
            tcPr.addNewVAlign().setVal(STVerticalJc.CENTER);
        }
        var tcW = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
        tcW.setW(BigInteger.valueOf(widthTwips));
    }

    private void setBorder(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder border) {
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(12));
        border.setColor("000000");
    }

    private void setCellText(XWPFTableCell cell, String text, boolean bold, int fontSize,
                             ParagraphAlignment alignment) {
        if (!cell.getParagraphs().isEmpty()) {
            XWPFParagraph p = cell.getParagraphs().get(0);
            p.setAlignment(alignment);
            while (!p.getRuns().isEmpty()) {
                p.removeRun(0);
            }
            XWPFRun run = p.createRun();
            run.setText(text == null ? "" : text);
            run.setBold(bold);
            run.setFontSize(fontSize);
            run.setFontFamily("Calibri");
        } else {
            addCellLine(cell, text, bold, fontSize, alignment);
        }
    }

    private void addCellLine(XWPFTableCell cell, String text, boolean bold, int fontSize,
                             ParagraphAlignment alignment) {
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(alignment);
        XWPFRun run = p.createRun();
        run.setText(text == null ? "" : text);
        run.setBold(bold);
        run.setFontSize(fontSize);
        run.setFontFamily("Calibri");
    }

    private void addQuestionsSection(XWPFDocument document, List<Question> questions) {
        int index = 1;
        for (Question question : questions) {
            addSingleQuestion(document, question, index);
            index++;
        }

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

    private void addAnswerLines(XWPFDocument document) {
        for (int i = 0; i < 3; i++) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setIndentationLeft(400);
            paragraph.setBorderBottom(Borders.SINGLE);
            paragraph.createRun().setText(" ");
        }
    }

    private void addAnswerKeySection(XWPFDocument document, List<Question> questions) {
        XWPFParagraph sectionTitle = document.createParagraph();
        XWPFRun sectionTitleRun = sectionTitle.createRun();
        sectionTitleRun.setText("Corrigé");
        sectionTitleRun.setBold(true);
        sectionTitleRun.setFontSize(18);
        sectionTitle.setSpacingAfter(200);

        int index = 1;
        for (Question question : questions) {
            XWPFParagraph answerParagraph = document.createParagraph();
            answerParagraph.setSpacingBefore(150);

            XWPFRun numberRun = answerParagraph.createRun();
            numberRun.setText(index + ". Réponse : ");
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
