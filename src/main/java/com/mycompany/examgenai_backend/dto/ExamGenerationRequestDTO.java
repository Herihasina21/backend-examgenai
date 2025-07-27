package com.mycompany.examgenai_backend.dto;

import com.mycompany.examgenai_backend.enums.DifficultyLevel;
import com.mycompany.examgenai_backend.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamGenerationRequestDTO {
    private Long chapterId;
    private Integer numberOfQuestions;
    private DifficultyLevel difficultyLevel;
    private List<QuestionType> questionTypes;
    private Integer durationMinutes;
    private String examTitle;
    private String examDescription;
}

