package com.mycompany.examgenai_backend.repository;

import com.mycompany.examgenai_backend.entity.Course;
import com.mycompany.examgenai_backend.enums.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTitleContainingIgnoreCase(String title);
    List<Course> findByFileType(FileType fileType);
    Optional<Course> findByFilePath(String filePath);
}