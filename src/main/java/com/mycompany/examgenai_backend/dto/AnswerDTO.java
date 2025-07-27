package com.mycompany.examgenai_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerDTO {
    private Long id;
    private String answerText;
    private Boolean isCorrect;
    private Integer answerOrder;
    private Long questionId;
}
