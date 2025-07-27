package com.mycompany.examgenai_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterDTO {
    private Long id;
    private String title;
    private String content;
    private Integer chapterNumber;
    private Integer pageStart;
    private Integer pageEnd;
    private Long courseId;
    private LocalDateTime createdAt;
}