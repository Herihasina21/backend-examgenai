package com.mycompany.examgenai_backend.service;

import com.mycompany.examgenai_backend.dto.QuestionCreateDTO;
import com.mycompany.examgenai_backend.entity.Exam;
import com.mycompany.examgenai_backend.entity.Question;
import com.mycompany.examgenai_backend.enums.DifficultyLevel;
import com.mycompany.examgenai_backend.enums.QuestionType;
import com.mycompany.examgenai_backend.exception.InvalidQuestionException;
import com.mycompany.examgenai_backend.exception.ResourceNotFoundException;
import com.mycompany.examgenai_backend.mapper.QuestionMapper;
import com.mycompany.examgenai_backend.repository.ExamRepository;
import com.mycompany.examgenai_backend.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private QuestionMapper questionMapper;

    @InjectMocks
    private QuestionService questionService;

    @Test
    void create_synchroniseTotalQuestions() {
        Exam exam = new Exam();
        exam.setId(1L);
        exam.setTotalQuestions(2);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            question.setId(10L);
            return question;
        });
        when(questionRepository.countByExamId(1L)).thenReturn(3L);
        when(questionMapper.toDTO(any(Question.class))).thenReturn(
                com.mycompany.examgenai_backend.dto.QuestionDTO.builder().id(10L).build()
        );

        QuestionCreateDTO dto = buildOpenQuestionDto();

        questionService.create(dto);

        ArgumentCaptor<Exam> examCaptor = ArgumentCaptor.forClass(Exam.class);
        verify(examRepository).save(examCaptor.capture());
        assertEquals(3, examCaptor.getValue().getTotalQuestions());
    }

    @Test
    void create_qcmSansOptionsLeveErreur() {
        Exam exam = new Exam();
        exam.setId(1L);
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));

        QuestionCreateDTO dto = buildOpenQuestionDto();
        dto.setQuestionType(QuestionType.QCM);
        dto.setOptions(List.of("A"));
        dto.setCorrectAnswer("A");

        assertThrows(InvalidQuestionException.class, () -> questionService.create(dto));
    }

    @Test
    void create_examenInexistantLeve404() {
        when(examRepository.findById(99L)).thenReturn(Optional.empty());

        QuestionCreateDTO dto = buildOpenQuestionDto();
        dto.setExamId(99L);

        assertThrows(ResourceNotFoundException.class, () -> questionService.create(dto));
    }

    @Test
    void delete_synchroniseTotalQuestions() {
        Exam exam = new Exam();
        exam.setId(1L);
        exam.setTotalQuestions(3);

        Question question = Question.builder()
                .id(5L)
                .exam(exam)
                .build();

        when(questionRepository.findById(5L)).thenReturn(Optional.of(question));
        when(questionRepository.countByExamId(1L)).thenReturn(2L);

        questionService.delete(5L);

        ArgumentCaptor<Exam> examCaptor = ArgumentCaptor.forClass(Exam.class);
        verify(examRepository).save(examCaptor.capture());
        assertEquals(2, examCaptor.getValue().getTotalQuestions());
    }

    private QuestionCreateDTO buildOpenQuestionDto() {
        QuestionCreateDTO dto = new QuestionCreateDTO();
        dto.setExamId(1L);
        dto.setStatement("Quelle est la capitale de Madagascar ?");
        dto.setQuestionType(QuestionType.OPEN);
        dto.setDifficulty(DifficultyLevel.MEDIUM);
        dto.setPoints(2);
        dto.setOptions(List.of());
        dto.setCorrectAnswer("Antananarivo");
        return dto;
    }
}
