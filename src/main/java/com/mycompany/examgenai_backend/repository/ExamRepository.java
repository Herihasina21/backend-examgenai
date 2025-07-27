package com.mycompany.examgenai_backend.repository;

import com.mycompany.examgenai_backend.entity.Exam;
import com.mycompany.examgenai_backend.enums.DifficultyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByCourseId(Long courseId);
    List<Exam> findByChapterId(Long chapterId);
    List<Exam> findByDifficultyLevel(DifficultyLevel difficultyLevel);
    List<Exam> findByCourseIdAndChapterId(Long courseId, Long chapterId);
}
