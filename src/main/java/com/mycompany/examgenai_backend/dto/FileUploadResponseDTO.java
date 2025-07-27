package com.mycompany.examgenai_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponseDTO {
    private String message;
    private String fileName;
    private Long courseId;
    private List<ChapterDTO> extractedChapters;
}