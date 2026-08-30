package com.mycompany.examgenai_backend.service.export;

import com.mycompany.examgenai_backend.entity.Exam;
import com.mycompany.examgenai_backend.entity.Question;
import com.mycompany.examgenai_backend.exception.ExportException;
import com.mycompany.examgenai_backend.exception.ResourceNotFoundException;
import com.mycompany.examgenai_backend.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final ExamRepository examRepository;

    private static final float MARGIN = 50f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final float LINE_HEIGHT = 16f;
    private static final float BOTTOM_LIMIT = MARGIN + 30f;

    private static final PDFont FONT_TITLE = PDType1Font.HELVETICA_BOLD;
    private static final PDFont FONT_SUBTITLE = PDType1Font.HELVETICA;
    private static final PDFont FONT_QUESTION = PDType1Font.HELVETICA_BOLD;
    private static final PDFont FONT_OPTION = PDType1Font.HELVETICA;
    private static final PDFont FONT_FOOTER = PDType1Font.HELVETICA_OBLIQUE;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional(readOnly = true)
    public byte[] generateExamPdf(Long examId) {
        Exam exam = examRepository.findByIdWithChapterAndQuestions(examId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Exam introuvable avec id: " + examId));

        try (PDDocument document = new PDDocument()) {
            PageWriter writer = new PageWriter(document);

            writer.addHeader(exam);
            writer.addQuestions(exam.getQuestions());
            writer.finish();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new ExportException("Erreur lors de la génération du PDF", e);
        }
    }

    private class PageWriter {

        private final PDDocument document;
        private PDPageContentStream contentStream;
        private float yPosition;
        private int pageNumber;

        PageWriter(PDDocument document) throws IOException {
            this.document = document;
            startNewPage();
        }

        private void startNewPage() throws IOException {
            pageNumber++;
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            yPosition = PAGE_HEIGHT - MARGIN;
        }

        private void ensureSpace(float neededHeight) throws IOException {
            if (yPosition - neededHeight < BOTTOM_LIMIT) {
                closeCurrentPage();
                startNewPage();
            }
        }

        private void closeCurrentPage() throws IOException {
            drawFooter();
            contentStream.close();
        }

        private void drawFooter() throws IOException {
            String footerText = "Page " + pageNumber;
            float textWidth = FONT_FOOTER.getStringWidth(footerText) / 1000 * 9f;
            float x = (PAGE_WIDTH - textWidth) / 2;

            contentStream.beginText();
            contentStream.setFont(FONT_FOOTER, 9f);
            contentStream.newLineAtOffset(x, MARGIN - 20);
            contentStream.showText(footerText);
            contentStream.endText();
        }

        void finish() throws IOException {
            closeCurrentPage();
        }

        void addHeader(Exam exam) throws IOException {
            drawCenteredText(exam.getTitle(), FONT_TITLE, 20f, yPosition);
            yPosition -= 26f;

            String chapterName = exam.getChapter() != null
                    ? exam.getChapter().getTitle()
                    : "Non specifie";
            String dateStr = exam.getCreatedAt() != null
                    ? exam.getCreatedAt().format(DATE_FORMATTER)
                    : "Non specifiee";
            String metaLine = "Chapitre: " + chapterName
                    + "   |   Date: " + dateStr
                    + "   |   Duree: " + exam.getDurationMinutes() + " min";

            drawCenteredText(metaLine, FONT_SUBTITLE, 11f, yPosition);
            yPosition -= 20f;

            drawLine(yPosition);
            yPosition -= 20f;
        }

        void addQuestions(List<Question> questions) throws IOException {
            int index = 1;
            for (Question question : questions) {
                addSingleQuestion(question, index);
                index++;
            }
        }

        private void addSingleQuestion(Question question, int index) throws IOException {
            ensureSpace(LINE_HEIGHT * 2);
            yPosition -= 6f;

            String header = index + ". " + question.getStatement()
                    + "  (" + question.getPoints() + " pt)";
            writeWrappedText(header, FONT_QUESTION, 12f, 0);

            switch (question.getQuestionType()) {
                case QCM -> addOptions(question.getOptions());
                case TRUE_FALSE -> addTrueFalseOptions();
                case OPEN, FILL_IN_BLANK -> addAnswerSpace();
            }
        }

        private void addOptions(List<String> options) throws IOException {
            char letter = 'a';
            for (String option : options) {
                writeWrappedText(letter + ") " + option, FONT_OPTION, 11f, 15f);
                letter++;
            }
        }

        private void addTrueFalseOptions() throws IOException {
            writeWrappedText("[ ] Vrai        [ ] Faux", FONT_OPTION, 11f, 15f);
        }

        private void addAnswerSpace() throws IOException {
            writeWrappedText("Reponse:", FONT_FOOTER, 10f, 15f);
            for (int i = 0; i < 3; i++) {
                ensureSpace(LINE_HEIGHT);
                drawLine(yPosition);
                yPosition -= LINE_HEIGHT;
            }
        }

        private void writeWrappedText(String text, PDFont font, float fontSize, float indent) throws IOException {
            float maxWidth = CONTENT_WIDTH - indent;
            List<String> lines = wrapText(text, font, fontSize, maxWidth);

            for (String line : lines) {
                ensureSpace(LINE_HEIGHT);
                contentStream.beginText();
                contentStream.setFont(font, fontSize);
                contentStream.newLineAtOffset(MARGIN + indent, yPosition);
                contentStream.showText(line);
                contentStream.endText();
                yPosition -= LINE_HEIGHT;
            }
        }

        private void drawCenteredText(String text, PDFont font, float fontSize, float y) throws IOException {
            float textWidth = font.getStringWidth(text) / 1000 * fontSize;
            float x = (PAGE_WIDTH - textWidth) / 2;

            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(text);
            contentStream.endText();
        }

        private void drawLine(float y) throws IOException {
            contentStream.setLineWidth(0.5f);
            contentStream.moveTo(MARGIN, y);
            contentStream.lineTo(PAGE_WIDTH - MARGIN, y);
            contentStream.stroke();
        }

        private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            String[] words = text.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
                float width = font.getStringWidth(candidate) / 1000 * fontSize;

                if (width > maxWidth && !currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(candidate);
                }
            }
            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }
            return lines;
        }
    }
}