package com.mycompany.examgenai_backend.service;

import com.mycompany.examgenai_backend.dto.ExamDTO;
import com.mycompany.examgenai_backend.dto.ExamGenerationRequestDTO;
import com.mycompany.examgenai_backend.dto.gemini.GeneratedAnswerDTO;
import com.mycompany.examgenai_backend.dto.gemini.GeneratedExamResponseDTO;
import com.mycompany.examgenai_backend.dto.gemini.GeneratedQuestionDTO;
import com.mycompany.examgenai_backend.entity.Chapter;
import com.mycompany.examgenai_backend.entity.Course;
import com.mycompany.examgenai_backend.entity.Exam;
import com.mycompany.examgenai_backend.entity.Question;
import com.mycompany.examgenai_backend.enums.DifficultyLevel;
import com.mycompany.examgenai_backend.enums.QuestionType;
import com.mycompany.examgenai_backend.exception.BadRequestException;
import com.mycompany.examgenai_backend.exception.ResourceNotFoundException;
import com.mycompany.examgenai_backend.mapper.QuestionMapper;
import com.mycompany.examgenai_backend.repository.ChapterRepository;
import com.mycompany.examgenai_backend.repository.CourseRepository;
import com.mycompany.examgenai_backend.repository.ExamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExamService {

    private static final String LEGACY_PLACEHOLDER = "Contenu extrait automatiquement ou vide.";

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private QuestionMapper questionMapper;

    public ExamDTO generateExam(ExamGenerationRequestDTO request) {
        validateGenerationRequest(request);

        var difficulty = request.getDifficultyLevel() != null ? request.getDifficultyLevel() : DifficultyLevel.MEDIUM;
        var questionTypes = resolveQuestionTypes(request.getQuestionTypes());
        var numberOfQuestions = request.getNumberOfQuestions();

        Exam exam = new Exam();
        exam.setTitle(request.getExamTitle());
        exam.setDescription(request.getExamDescription());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setDifficultyLevel(difficulty);

        GeneratedExamResponseDTO generated;

        if (request.getChapterId() != null) {
            // Génération classique : un seul chapitre
            Chapter chapter = chapterRepository.findById(request.getChapterId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Chapitre introuvable. Vérifiez la sélection et réessayez."));

            validateChapterContent(chapter.getContent());

            generated = geminiService.generateQuestions(
                    chapter.getTitle(),
                    chapter.getContent(),
                    numberOfQuestions,
                    questionTypes,
                    difficulty
            );

            exam.setChapter(chapter);
            exam.setCourse(chapter.getCourse());
        } else {
            // Génération sur "toutes les chapitres" d'un cours
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cours introuvable. Vérifiez la sélection et réessayez."));

            List<Chapter> chapters = chapterRepository.findByCourseId(course.getId());
            if (chapters.isEmpty()) {
                throw new BadRequestException(
                        "Ce cours ne contient aucun chapitre exploitable pour la génération.");
            }

            String combinedContent = buildCombinedContent(chapters);
            validateChapterContent(combinedContent);

            generated = geminiService.generateQuestions(
                    course.getTitle(),
                    combinedContent,
                    numberOfQuestions,
                    questionTypes,
                    difficulty
            );

            exam.setChapter(null);
            exam.setCourse(course);
        }

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

    public List<ExamDTO> getExamsByCourse(Long courseId) {
        return examRepository.findByCourseId(courseId).stream()
                .map(this::toExamDTO)
                .collect(Collectors.toList());
    }

    public void deleteExam(Long id) {
        if (!examRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Cet examen est introuvable. Il a peut-être déjà été supprimé.");
        }
        examRepository.deleteById(id);
    }

    private void validateGenerationRequest(ExamGenerationRequestDTO request) {
        if (request.getExamTitle() == null || request.getExamTitle().isBlank()) {
            throw new BadRequestException("Le titre de l'examen est obligatoire.");
        }
        if (request.getChapterId() == null && request.getCourseId() == null) {
            throw new BadRequestException("Choisissez un chapitre ou un cours pour générer l'examen.");
        }
        if (request.getNumberOfQuestions() == null || request.getNumberOfQuestions() < 1) {
            throw new BadRequestException("Le nombre de questions doit être au moins 1.");
        }
        if (request.getDurationMinutes() == null || request.getDurationMinutes() < 1) {
            throw new BadRequestException("La durée de l'examen doit être au moins 1 minute.");
        }
    }

    private void validateChapterContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BadRequestException(
                    "Le contenu source ne contient pas de texte exploitable pour la génération.");
        }
        if (LEGACY_PLACEHOLDER.equals(content.trim())) {
            throw new BadRequestException(
                    "Le contenu n'a pas encore été extrait. Ré-uploadez le cours.");
        }
    }

    private String buildCombinedContent(List<Chapter> chapters) {
        StringBuilder sb = new StringBuilder();
        for (Chapter chapter : chapters) {
            sb.append("== Chapitre ")
                    .append(chapter.getChapterNumber())
                    .append(" : ")
                    .append(chapter.getTitle())
                    .append(" ==\n");
            sb.append(chapter.getContent() != null ? chapter.getContent() : "");
            sb.append("\n\n");
        }
        return sb.toString().trim();
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