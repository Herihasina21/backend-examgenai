package com.mycompany.examgenai_backend.service;

import com.mycompany.examgenai_backend.dto.QuestionCreateDTO;
import com.mycompany.examgenai_backend.dto.QuestionDTO;
import com.mycompany.examgenai_backend.dto.QuestionUpdateDTO;
import com.mycompany.examgenai_backend.entity.Exam;
import com.mycompany.examgenai_backend.entity.Question;
import com.mycompany.examgenai_backend.enums.QuestionType;
import com.mycompany.examgenai_backend.exception.InvalidQuestionException;
import com.mycompany.examgenai_backend.exception.ResourceNotFoundException;
import com.mycompany.examgenai_backend.mapper.QuestionMapper;
import com.mycompany.examgenai_backend.repository.ExamRepository;
import com.mycompany.examgenai_backend.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service métier pour la gestion CRUD des questions.
 * ⚠️ Ceci remplace toute ancienne version de ce fichier basée sur
 * QuestionRequest / optionA-D — cette architecture utilise une liste
 * générique d'options (adaptée au QCM à nombre variable de choix).
 */
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final QuestionMapper questionMapper;

    @Transactional
    public QuestionDTO create(QuestionCreateDTO dto) {
        Exam exam = examRepository.findById(dto.getExamId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Exam introuvable avec id: " + dto.getExamId()));

        validateOptions(dto.getQuestionType(), dto.getOptions());

        Question question = Question.builder()
                .statement(dto.getStatement())
                .questionType(dto.getQuestionType())
                .difficulty(dto.getDifficulty())
                .points(dto.getPoints())
                .options(dto.getOptions())
                .correctAnswer(dto.getCorrectAnswer())
                .explanation(dto.getExplanation())
                .exam(exam)
                .build();

        Question saved = questionRepository.save(question);
        return questionMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<QuestionDTO> getByExam(Long examId) {
        if (!examRepository.existsById(examId)) {
            throw new ResourceNotFoundException("Exam introuvable avec id: " + examId);
        }
        return questionRepository.findByExamId(examId)
                .stream()
                .map(questionMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionDTO getById(Long id) {
        Question question = findQuestionOrThrow(id);
        return questionMapper.toDTO(question);
    }

    @Transactional
    public QuestionDTO update(Long id, QuestionUpdateDTO dto) {
        Question question = findQuestionOrThrow(id);

        validateOptions(dto.getQuestionType(), dto.getOptions());

        question.setStatement(dto.getStatement());
        question.setQuestionType(dto.getQuestionType());
        question.setDifficulty(dto.getDifficulty());
        question.setPoints(dto.getPoints());
        question.setOptions(dto.getOptions());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        question.setExplanation(dto.getExplanation());

        Question updated = questionRepository.save(question);
        return questionMapper.toDTO(updated);
    }

    @Transactional
    public void delete(Long id) {
        Question question = findQuestionOrThrow(id);
        questionRepository.delete(question);
    }

    private Question findQuestionOrThrow(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Question introuvable avec id: " + id));
    }

    private void validateOptions(QuestionType type, List<String> options) {
        if (type == QuestionType.QCM) {
            if (options == null || options.size() < 2) {
                throw new InvalidQuestionException(
                        "Une question QCM doit avoir au moins 2 options");
            }
        }
    }
}