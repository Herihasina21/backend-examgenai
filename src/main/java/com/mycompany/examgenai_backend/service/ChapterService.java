package com.mycompany.examgenai_backend.service;

import com.mycompany.examgenai_backend.dto.ChapterDTO;
import com.mycompany.examgenai_backend.exception.ResourceNotFoundException;
import com.mycompany.examgenai_backend.repository.ChapterRepository;
import com.mycompany.examgenai_backend.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@Service
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final CourseRepository courseRepository;
    private final ModelMapper modelMapper;

    public List<ChapterDTO> getChaptersByCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Cours introuvable avec id: " + courseId);
        }
        return chapterRepository.findByCourseId(courseId).stream()
                .map(chapter -> modelMapper.map(chapter, ChapterDTO.class))
                .collect(Collectors.toList());
    }

    public Optional<ChapterDTO> getChapterById(Long id) {
        return chapterRepository.findById(id)
                .map(chapter -> modelMapper.map(chapter, ChapterDTO.class));
    }
}
