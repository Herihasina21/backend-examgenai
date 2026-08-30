package com.mycompany.examgenai_backend.mapper;

import com.mycompany.examgenai_backend.dto.QuestionDTO;
import com.mycompany.examgenai_backend.entity.Question;
import org.springframework.stereotype.Component;

@Component
public class QuestionMapper {

    public QuestionDTO toDTO(Question question) {
        return QuestionDTO.builder()
                .id(question.getId())
                .statement(question.getStatement())
                .questionType(question.getQuestionType())
                .difficulty(question.getDifficulty())
                .points(question.getPoints())
                .options(question.getOptions())
                .correctAnswer(question.getCorrectAnswer())
                .explanation(question.getExplanation())
                .examId(question.getExam() != null ? question.getExam().getId() : null)
                .build();
    }
}