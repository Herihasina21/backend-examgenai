package com.mycompany.examgenai_backend.service;

import com.mycompany.examgenai_backend.dto.ExamDTO;
import com.mycompany.examgenai_backend.dto.ExamGenerationRequestDTO;
import com.mycompany.examgenai_backend.dto.openai.GeneratedAnswerDTO;
import com.mycompany.examgenai_backend.dto.openai.GeneratedQuestionDTO;
import com.mycompany.examgenai_backend.entity.Chapter;
import com.mycompany.examgenai_backend.entity.Exam;
import com.mycompany.examgenai_backend.entity.Question;
import com.mycompany.examgenai_backend.enums.DifficultyLevel;
import com.mycompany.examgenai_backend.enums.QuestionType;
import com.mycompany.examgenai_backend.mapper.QuestionMapper;
import com.mycompany.examgenai_backend.repository.ChapterRepository;
import com.mycompany.examgenai_backend.repository.ExamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExamService {

    // Ancien message placeholder, avant l'implémentation de l'extraction réelle des
    // chapitres (voir ChapterExtractor). Conservé au cas où d'anciennes données en
    // base contiendraient encore cette valeur.
    private static final String LEGACY_PLACEHOLDER = "Contenu extrait automatiquement ou vide.";

    // Un chapitre dont le contenu correspond à l'un de ces messages n'a rien
    // d'exploitable à envoyer à Gemini : mieux vaut échouer proprement ici que
    // de générer des questions à partir d'un message d'erreur interne.
    private static final Set<String> UNUSABLE_CHAPTER_CONTENTS = Set.of(
            LEGACY_PLACEHOLDER,
            ChapterExtractor.EMPTY_CONTENT_FALLBACK,
            ChapterExtractor.EMPTY_DOCUMENT_FALLBACK
    );

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private QuestionMapper questionMapper;

    public ExamDTO generateExam(ExamGenerationRequestDTO request) {
        validateGenerationRequest(request);

        Chapter chapter = chapterRepository.findById(request.getChapterId())
                .orElseThrow(() -> new RuntimeException("Chapitre introuvable avec l'id " + request.getChapterId()));

        validateChapterContent(chapter.getContent());

        var numberOfQuestions = request.getNumberOfQuestions();
        var difficulty = request.getDifficultyLevel() != null ? request.getDifficultyLevel() : DifficultyLevel.MEDIUM;
        var questionTypes = resolveQuestionTypes(request.getQuestionTypes());

        var generated = geminiService.generateQuestions(
                chapter.getTitle(),
                chapter.getContent(),
                numberOfQuestions,
                questionTypes,
                difficulty
        );

        Exam exam = new Exam();
        exam.setTitle(request.getExamTitle());
        exam.setDescription(request.getExamDescription());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setDifficultyLevel(difficulty);
        exam.setChapter(chapter);
        exam.setCourse(chapter.getCourse());

        List<Question> questions = buildQuestions(generated.getQuestions(), exam, difficulty);
        exam.setQuestions(questions);
        exam.setTotalQuestions(questions.size());

        Exam savedExam = examRepository.save(exam);
        return toExamDTO(savedExam);
    }

    public Optional<ExamDTO> getExamById(Long id) {
        return examRepository.findById(id).map(this::toExamDTO);
    }

    public List<ExamDTO> getExamsByChapter(Long chapterId) {
        return examRepository.findByChapterId(chapterId).stream()
                .map(this::toExamDTO)
                .collect(Collectors.toList());
    }

    public void deleteExam(Long id) {
        if (!examRepository.existsById(id)) {
            throw new RuntimeException("Examen introuvable avec l'id " + id);
        }
        examRepository.deleteById(id);
    }

    private void validateGenerationRequest(ExamGenerationRequestDTO request) {
        if (request.getExamTitle() == null || request.getExamTitle().isBlank()) {
            throw new RuntimeException("Le titre de l'examen est obligatoire");
        }
        if (request.getChapterId() == null) {
            throw new RuntimeException("L'id du chapitre est obligatoire");
        }
        if (request.getNumberOfQuestions() == null || request.getNumberOfQuestions() < 1) {
            throw new RuntimeException("Le nombre de questions doit être au moins 1");
        }
        if (request.getDurationMinutes() == null || request.getDurationMinutes() < 1) {
            throw new RuntimeException("La durée de l'examen doit être au moins 1 minute");
        }
    }

    private void validateChapterContent(String content) {
        if (content == null || content.isBlank()) {
            throw new RuntimeException("Le chapitre ne contient pas de texte exploitable pour la génération");
        }
        if (UNUSABLE_CHAPTER_CONTENTS.contains(content.trim())) {
            throw new RuntimeException("Le contenu de ce chapitre n'a pas pu être extrait correctement. Ré-uploadez le cours ou vérifiez son contenu avant de générer un examen.");
        }
    }

    private List<QuestionType> resolveQuestionTypes(List<QuestionType> questionTypes) {
        if (questionTypes == null || questionTypes.isEmpty()) {
            return List.of(QuestionType.QCM, QuestionType.TRUE_FALSE, QuestionType.OPEN);
        }
        return questionTypes;
    }

    private List<Question> buildQuestions(List<GeneratedQuestionDTO> generatedQuestions, Exam exam, DifficultyLevel defaultDifficulty) {
        List<Question> questions = new ArrayList<>();

        for (GeneratedQuestionDTO generatedQuestion : generatedQuestions) {
            var options = new ArrayList<String>();
            var correctAnswer = "";

            if (generatedQuestion.getAnswers() != null) {
                for (GeneratedAnswerDTO answer : generatedQuestion.getAnswers()) {
                    if (answer.getAnswerText() == null || answer.getAnswerText().isBlank()) {
                        continue;
                    }
                    options.add(answer.getAnswerText());
                    if (Boolean.TRUE.equals(answer.getIsCorrect()) && correctAnswer.isBlank()) {
                        correctAnswer = answer.getAnswerText();
                    }
                }
            }

            if (correctAnswer.isBlank() && !options.isEmpty()) {
                correctAnswer = options.get(0);
            }
            if (correctAnswer.isBlank()) {
                correctAnswer = "N/A";
            }

            Question question = new Question();
            question.setStatement(generatedQuestion.getQuestionText());
            question.setQuestionType(
                    generatedQuestion.getQuestionType() != null ? generatedQuestion.getQuestionType() : QuestionType.QCM
            );
            question.setPoints(generatedQuestion.getPoints() != null ? generatedQuestion.getPoints() : 1);
            question.setDifficulty(
                    generatedQuestion.getDifficultyLevel() != null ? generatedQuestion.getDifficultyLevel() : defaultDifficulty
            );
            question.setOptions(options);
            question.setCorrectAnswer(correctAnswer);
            question.setExam(exam);
            questions.add(question);
        }

        return questions;
    }

    private ExamDTO toExamDTO(Exam exam) {
        ExamDTO dto = new ExamDTO();
        dto.setId(exam.getId());
        dto.setTitle(exam.getTitle());
        dto.setDescription(exam.getDescription());
        dto.setTotalQuestions(exam.getTotalQuestions());
        dto.setDurationMinutes(exam.getDurationMinutes());
        dto.setDifficultyLevel(exam.getDifficultyLevel());
        dto.setCreatedAt(exam.getCreatedAt());

        if (exam.getChapter() != null) {
            dto.setChapterId(exam.getChapter().getId());
        }
        if (exam.getCourse() != null) {
            dto.setCourseId(exam.getCourse().getId());
        }

        if (exam.getQuestions() != null) {
            dto.setQuestions(exam.getQuestions().stream()
                    .map(questionMapper::toDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}