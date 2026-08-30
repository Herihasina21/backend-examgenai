package com.mycompany.examgenai_backend.controller;

import com.mycompany.examgenai_backend.dto.QuestionCreateDTO;
import com.mycompany.examgenai_backend.dto.QuestionDTO;
import com.mycompany.examgenai_backend.dto.QuestionUpdateDTO;
import com.mycompany.examgenai_backend.service.QuestionService;
import com.mycompany.examgenai_backend.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    public ResponseEntity<ApiResponse<QuestionDTO>> create(@Valid @RequestBody QuestionCreateDTO dto) {
        QuestionDTO created = questionService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Question créée avec succès", created));
    }

    @GetMapping("/exam/{examId}")
    public ResponseEntity<ApiResponse<List<QuestionDTO>>> getByExam(@PathVariable Long examId) {
        List<QuestionDTO> questions = questionService.getByExam(examId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Questions récupérées avec succès", questions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionDTO>> getById(@PathVariable Long id) {
        QuestionDTO question = questionService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Question récupérée avec succès", question));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody QuestionUpdateDTO dto) {
        QuestionDTO updated = questionService.update(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Question modifiée avec succès", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Question supprimée avec succès", null));
    }
}
