package com.mycompany.examgenai_backend.repository;

import com.mycompany.examgenai_backend.entity.Question;
import com.mycompany.examgenai_backend.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByExamId(Long examId);
    List<Question> findByQuestionType(QuestionType questionType);
    List<Question> findByExamIdAndQuestionType(Long examId, QuestionType questionType);
    Long countByExamId(Long examId);
}