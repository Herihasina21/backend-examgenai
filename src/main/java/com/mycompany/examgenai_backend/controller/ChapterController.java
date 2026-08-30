package com.mycompany.examgenai_backend.controller;

import com.mycompany.examgenai_backend.dto.ChapterDTO;
import com.mycompany.examgenai_backend.exception.ResourceNotFoundException;
import com.mycompany.examgenai_backend.service.ChapterService;
import com.mycompany.examgenai_backend.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chapters")
public class ChapterController {

    @Autowired
    private ChapterService chapterService;

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<ChapterDTO>>> getChaptersByCourse(@PathVariable Long courseId) {
        List<ChapterDTO> chapters = chapterService.getChaptersByCourse(courseId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Chapitres récupérés avec succès pour le cours " + courseId, chapters));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChapterDTO>> getChapterById(@PathVariable Long id) {
        return chapterService.getChapterById(id)
                .map(chapter -> ResponseEntity.ok(new ApiResponse<>(true, "Chapitre récupéré avec succès", chapter)))
                .orElseThrow(() -> new ResourceNotFoundException("Chapitre introuvable avec id: " + id));
    }
}

