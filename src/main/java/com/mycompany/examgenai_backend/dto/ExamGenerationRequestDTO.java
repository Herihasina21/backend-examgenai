package com.mycompany.examgenai_backend.dto;

import com.mycompany.examgenai_backend.enums.DifficultyLevel;
import com.mycompany.examgenai_backend.enums.QuestionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamGenerationRequestDTO {

    @NotBlank(message = "Le titre de l'examen est obligatoire")
    private String examTitle;

    private String examDescription;

    @NotNull(message = "L'identifiant du chapitre est obligatoire")
    private Long chapterId;

    private Long courseId;

    @NotNull(message = "Le nombre de questions est obligatoire")
    @Min(value = 1, message = "Le nombre de questions doit être au moins 1")
    private Integer numberOfQuestions;

    @NotNull(message = "La durée de l'examen est obligatoire")
    @Min(value = 1, message = "La durée de l'examen doit être au moins 1 minute")
    private Integer durationMinutes;

    private DifficultyLevel difficultyLevel;

    private List<QuestionType> questionTypes;
}