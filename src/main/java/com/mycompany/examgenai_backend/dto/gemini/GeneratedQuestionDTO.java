package com.mycompany.examgenai_backend.dto.gemini;

import com.mycompany.examgenai_backend.enums.DifficultyLevel;
import com.mycompany.examgenai_backend.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedQuestionDTO {
    private String questionText;
    private QuestionType questionType;
    private Integer points;
    private DifficultyLevel difficultyLevel;
    private List<GeneratedAnswerDTO> answers;
}
