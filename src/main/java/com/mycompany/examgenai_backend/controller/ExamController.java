package com.mycompany.examgenai_backend.controller;

import com.mycompany.examgenai_backend.dto.ExamDTO;
import com.mycompany.examgenai_backend.dto.ExamGenerationRequestDTO;
import com.mycompany.examgenai_backend.service.ExamService;
import com.mycompany.examgenai_backend.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    @Autowired
    private ExamService examService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ExamDTO>> generateExam(@RequestBody ExamGenerationRequestDTO request) {
        ExamDTO exam = examService.generateExam(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Examen généré avec succès", exam));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamDTO>> getExamById(@PathVariable Long id) {
        return examService.getExamById(id)
                .map(exam -> ResponseEntity.ok(new ApiResponse<>(true, "Examen récupéré avec succès", exam)))
                .orElseThrow(() -> new RuntimeException("Examen introuvable avec l'id " + id));
    }

    @GetMapping("/chapter/{chapterId}")
    public ResponseEntity<ApiResponse<List<ExamDTO>>> getExamsByChapter(@PathVariable Long chapterId) {
        List<ExamDTO> exams = examService.getExamsByChapter(chapterId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examens récupérés avec succès", exams));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<ExamDTO>>> getExamsByCourse(@PathVariable Long courseId) {
        List<ExamDTO> exams = examService.getExamsByCourse(courseId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examens récupérés avec succès", exams));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examen supprimé avec succès", null));
    }
}