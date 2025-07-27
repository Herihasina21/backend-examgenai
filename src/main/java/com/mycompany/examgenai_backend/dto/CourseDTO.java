package com.mycompany.examgenai_backend.dto;

import com.mycompany.examgenai_backend.enums.FileType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private Long id;
    private String title;
    private String description;
    private String filePath;
    private FileType fileType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChapterDTO> chapters;
}
