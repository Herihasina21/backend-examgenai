package com.mycompany.examgenai_backend.dto.gemini;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedExamResponseDTO {
    private List<GeneratedQuestionDTO> questions;
}
