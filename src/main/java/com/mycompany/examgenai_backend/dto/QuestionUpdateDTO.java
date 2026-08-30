package com.mycompany.examgenai_backend.dto;

import com.mycompany.examgenai_backend.enums.QuestionType;
import com.mycompany.examgenai_backend.enums.DifficultyLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionUpdateDTO {

    @NotBlank(message = "Le libellé de la question est obligatoire")
    private String statement;

    @NotNull(message = "Le type de question est obligatoire")
    private QuestionType questionType;

    @NotNull(message = "La difficulté est obligatoire")
    private DifficultyLevel difficulty;

    @NotNull(message = "Le nombre de points est obligatoire")
    @Min(value = 1, message = "Le nombre de points doit être >= 1")
    private Integer points;

    private List<String> options;

    @NotBlank(message = "La réponse correcte est obligatoire")
    private String correctAnswer;

    private String explanation;
}