package com.mycompany.examgenai_backend.controller;

import com.mycompany.examgenai_backend.dto.CourseDTO;
import com.mycompany.examgenai_backend.exception.ResourceNotFoundException;
import com.mycompany.examgenai_backend.service.CourseService;
import com.mycompany.examgenai_backend.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseDTO>>> getAllCourses() {
        List<CourseDTO> courses = courseService.getAllCourses();
        return ResponseEntity.ok(new ApiResponse<>(true, "Cours récupéré avec succès", courses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseDTO>> getCourseById(@PathVariable Long id) {
        return courseService.getCourseById(id)
                .map(course -> ResponseEntity.ok(new ApiResponse<>(true, "Cours récupéré avec succès", course)))
                .orElseThrow(() -> new ResourceNotFoundException("Cours introuvable avec id: " + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourseDTO>> createCourse(@RequestBody CourseDTO courseDTO) {
        CourseDTO createdCourse = courseService.createCourse(courseDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Cours créé avec succès", createdCourse));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseDTO>> updateCourse(
            @PathVariable Long id,
            @RequestBody CourseDTO courseDetailsDto) {
        CourseDTO updatedCourse = courseService.updateCourse(id, courseDetailsDto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cours mis à jour avec succès", updatedCourse));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cours supprimé avec succès", null));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<CourseDTO>> uploadCourseFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description) throws IOException {

        CourseDTO newCourse = courseService.uploadCourseFile(file, title, description);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Cours téléchargé avec succès", newCourse));
    }
}
