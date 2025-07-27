package com.mycompany.examgenai_backend.dto;

import com.mycompany.examgenai_backend.enums.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamDTO {
    private Long id;
    private String title;
    private String description;
    private Integer totalQuestions;
    private Integer durationMinutes;
    private DifficultyLevel difficultyLevel;
    private Long courseId;
    private Long chapterId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<QuestionDTO> questions;
}
