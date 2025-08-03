package com.mycompany.examgenai_backend.service;


import com.mycompany.examgenai_backend.dto.ChapterDTO;
import com.mycompany.examgenai_backend.repository.ChapterRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@Service
public class ChapterService {
    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private ModelMapper modelMapper;

    public List<ChapterDTO> getChaptersByCourse(Long courseId) {
        return chapterRepository.findByCourseId(courseId).stream()
                .map(chapter -> modelMapper.map(chapter, ChapterDTO.class))
                .collect(Collectors.toList());
    }

    public Optional<ChapterDTO> getChapterById(Long id) {
        return chapterRepository.findById(id)
                .map(chapter -> modelMapper.map(chapter, ChapterDTO.class));
    }
}
