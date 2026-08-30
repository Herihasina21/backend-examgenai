package com.mycompany.examgenai_backend.service;

import com.mycompany.examgenai_backend.dto.AnswerDTO;
import com.mycompany.examgenai_backend.dto.ExamDTO;
import com.mycompany.examgenai_backend.dto.ExamGenerationRequestDTO;
import com.mycompany.examgenai_backend.dto.QuestionDTO;
import com.mycompany.examgenai_backend.dto.openai.GeneratedAnswerDTO;
import com.mycompany.examgenai_backend.dto.openai.GeneratedQuestionDTO;
import com.mycompany.examgenai_backend.entity.Answer;
import com.mycompany.examgenai_backend.entity.Chapter;
import com.mycompany.examgenai_backend.entity.Exam;
import com.mycompany.examgenai_backend.entity.Question;
import com.mycompany.examgenai_backend.enums.DifficultyLevel;
import com.mycompany.examgenai_backend.enums.QuestionType;
import com.mycompany.examgenai_backend.repository.ChapterRepository;
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
    private GeminiService geminiService;

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
        if (LEGACY_PLACEHOLDER.equals(content.trim())) {
            throw new RuntimeException("Le contenu du chapitre n'a pas encore été extrait. Ré-uploadez le cours ou attendez la mise à jour du chapitre.");
        }
    }

    private List<QuestionType> resolveQuestionTypes(List<QuestionType> questionTypes) {
        if (questionTypes == null || questionTypes.isEmpty()) {
            return List.of(QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE, QuestionType.OPEN_ENDED);
        }
        return questionTypes;
    }

    private List<Question> buildQuestions(List<GeneratedQuestionDTO> generatedQuestions, Exam exam, DifficultyLevel defaultDifficulty) {
        List<Question> questions = new ArrayList<>();

        for (GeneratedQuestionDTO generatedQuestion : generatedQuestions) {
            Question question = new Question();
            question.setQuestionText(generatedQuestion.getQuestionText());
            question.setQuestionType(generatedQuestion.getQuestionType());
            question.setPoints(generatedQuestion.getPoints() != null ? generatedQuestion.getPoints() : 1);
            question.setDifficultyLevel(
                    generatedQuestion.getDifficultyLevel() != null ? generatedQuestion.getDifficultyLevel() : defaultDifficulty
            );
            question.setExam(exam);

            List<Answer> answers = buildAnswers(generatedQuestion.getAnswers(), question);
            question.setAnswers(answers);
            questions.add(question);
        }

        return questions;
    }

    private List<Answer> buildAnswers(List<GeneratedAnswerDTO> generatedAnswers, Question question) {
        if (generatedAnswers == null || generatedAnswers.isEmpty()) {
            return new ArrayList<>();
        }

        List<Answer> answers = new ArrayList<>();
        int order = 1;

        for (GeneratedAnswerDTO generatedAnswer : generatedAnswers) {
            Answer answer = new Answer();
            answer.setAnswerText(generatedAnswer.getAnswerText());
            answer.setIsCorrect(generatedAnswer.getIsCorrect() != null && generatedAnswer.getIsCorrect());
            answer.setAnswerOrder(generatedAnswer.getAnswerOrder() != null ? generatedAnswer.getAnswerOrder() : order);
            answer.setQuestion(question);
            answers.add(answer);
            order++;
        }

        return answers;
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
                    .map(this::toQuestionDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private QuestionDTO toQuestionDTO(Question question) {
        QuestionDTO dto = new QuestionDTO();
        dto.setId(question.getId());
        dto.setQuestionText(question.getQuestionText());
        dto.setQuestionType(question.getQuestionType());
        dto.setPoints(question.getPoints());
        dto.setDifficultyLevel(question.getDifficultyLevel());
        dto.setExamId(question.getExam() != null ? question.getExam().getId() : null);

        if (question.getAnswers() != null) {
            dto.setAnswers(question.getAnswers().stream()
                    .map(this::toAnswerDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private AnswerDTO toAnswerDTO(Answer answer) {
        AnswerDTO dto = new AnswerDTO();
        dto.setId(answer.getId());
        dto.setAnswerText(answer.getAnswerText());
        dto.setIsCorrect(answer.getIsCorrect());
        dto.setAnswerOrder(answer.getAnswerOrder());
        dto.setQuestionId(answer.getQuestion() != null ? answer.getQuestion().getId() : null);
        return dto;
    }
}
