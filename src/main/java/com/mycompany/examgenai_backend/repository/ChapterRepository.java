package com.mycompany.examgenai_backend.repository;

import com.mycompany.examgenai_backend.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByCourseId(Long courseId);
    List<Chapter> findByCourseIdOrderByChapterNumberAsc(Long courseId);
    Optional<Chapter> findByCourseIdAndChapterNumber(Long courseId, Integer chapterNumber);
}
