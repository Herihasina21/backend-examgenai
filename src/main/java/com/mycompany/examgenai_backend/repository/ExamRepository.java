package com.mycompany.examgenai_backend.repository;

import com.mycompany.examgenai_backend.entity.Exam;
import com.mycompany.examgenai_backend.enums.DifficultyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByCourseId(Long courseId);
    List<Exam> findByChapterId(Long chapterId);
    List<Exam> findByDifficultyLevel(DifficultyLevel difficultyLevel);
    List<Exam> findByCourseIdAndChapterId(Long courseId, Long chapterId);

    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.chapter LEFT JOIN FETCH e.questions WHERE e.id = :id")
    Optional<Exam> findByIdWithChapterAndQuestions(@Param("id") Long id);

    @Query("SELECT DISTINCT e FROM Exam e LEFT JOIN FETCH e.chapter LEFT JOIN FETCH e.questions WHERE e.chapter.id = :chapterId")
    List<Exam> findByChapterIdWithQuestions(@Param("chapterId") Long chapterId);
}
