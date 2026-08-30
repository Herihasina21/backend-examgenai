package com.mycompany.examgenai_backend.dto.gemini;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedAnswerDTO {
    private String answerText;
    private Boolean isCorrect;
    private Integer answerOrder;
}
