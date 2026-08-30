package com.mycompany.examgenai_backend.controller;

import com.mycompany.examgenai_backend.dto.QuestionCreateDTO;
import com.mycompany.examgenai_backend.dto.QuestionDTO;
import com.mycompany.examgenai_backend.dto.QuestionUpdateDTO;
import com.mycompany.examgenai_backend.service.QuestionService;
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
    public ResponseEntity<QuestionDTO> create(@Valid @RequestBody QuestionCreateDTO dto) {
        QuestionDTO created = questionService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/exam/{examId}")
    public ResponseEntity<List<QuestionDTO>> getByExam(@PathVariable Long examId) {
        return ResponseEntity.ok(questionService.getByExam(examId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody QuestionUpdateDTO dto) {
        return ResponseEntity.ok(questionService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}