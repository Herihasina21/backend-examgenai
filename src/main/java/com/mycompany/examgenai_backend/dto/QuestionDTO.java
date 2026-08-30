package com.mycompany.examgenai_backend.dto;

import com.mycompany.examgenai_backend.enums.QuestionType;
import com.mycompany.examgenai_backend.enums.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionDTO {

    private Long id;
    private String statement;
    private QuestionType questionType;
    private DifficultyLevel difficulty;
    private Integer points;
    private List<String> options;
    private String correctAnswer;
    private String explanation;
    private Long examId;
}