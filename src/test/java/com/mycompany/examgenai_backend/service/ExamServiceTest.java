package com.mycompany.examgenai_backend.service;

import com.mycompany.examgenai_backend.dto.ExamGenerationRequestDTO;
import com.mycompany.examgenai_backend.entity.Chapter;
import com.mycompany.examgenai_backend.exception.BadRequestException;
import com.mycompany.examgenai_backend.exception.ResourceNotFoundException;
import com.mycompany.examgenai_backend.mapper.QuestionMapper;
import com.mycompany.examgenai_backend.repository.ChapterRepository;
import com.mycompany.examgenai_backend.repository.ExamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private GeminiService geminiService;

    @Mock
    private QuestionMapper questionMapper;

    @InjectMocks
    private ExamService examService;

    @Test
    void generateExam_chapitreInexistantLeve404() {
        ExamGenerationRequestDTO request = buildRequest();
        request.setChapterId(99L);

        when(chapterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> examService.generateExam(request));
    }

    @Test
    void generateExam_chapitreSansContenuLeve400() {
        Chapter chapter = new Chapter();
        chapter.setId(1L);
        chapter.setContent(ChapterExtractor.EMPTY_CONTENT_FALLBACK);

        when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));

        assertThrows(BadRequestException.class, () -> examService.generateExam(buildRequest()));
    }

    private ExamGenerationRequestDTO buildRequest() {
        ExamGenerationRequestDTO request = new ExamGenerationRequestDTO();
        request.setExamTitle("Examen test");
        request.setChapterId(1L);
        request.setNumberOfQuestions(3);
        request.setDurationMinutes(30);
        return request;
    }
}
