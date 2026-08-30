package com.mycompany.examgenai_backend.service;

import com.mycompany.examgenai_backend.exception.ResourceNotFoundException;
import com.mycompany.examgenai_backend.repository.ChapterRepository;
import com.mycompany.examgenai_backend.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChapterServiceTest {

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ChapterService chapterService;

    @Test
    void getChaptersByCourse_coursInexistantLeve404() {
        when(courseRepository.existsById(42L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> chapterService.getChaptersByCourse(42L));
    }
}
