package com.mycompany.examgenai_backend.service;

import com.mycompany.examgenai_backend.dto.CourseDTO;
import com.mycompany.examgenai_backend.entity.Chapter;
import com.mycompany.examgenai_backend.entity.Course;
import com.mycompany.examgenai_backend.enums.FileType;
import com.mycompany.examgenai_backend.exception.ResourceNotFoundException;
import com.mycompany.examgenai_backend.repository.ChapterRepository;
import com.mycompany.examgenai_backend.repository.CourseRepository;
import jakarta.transaction.Transactional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional
@Service
public class CourseService {
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ChapterExtractor chapterExtractor;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(course -> modelMapper.map(course, CourseDTO.class))
                .collect(Collectors.toList());
    }

    public Optional<CourseDTO> getCourseById(Long id) {
        return courseRepository.findById(id)
                .map(course -> modelMapper.map(course, CourseDTO.class));
    }

    @Transactional
    public CourseDTO createCourse(CourseDTO courseDTO) {
        Course course = modelMapper.map(courseDTO, Course.class);
        course.setCreatedAt(LocalDate.now());
        course.setUpdatedAt(LocalDate.now());
        Course savedCourse = courseRepository.save(course);
        return modelMapper.map(savedCourse, CourseDTO.class);
    }

    @Transactional
    public CourseDTO updateCourse(Long id, CourseDTO courseDetailsDto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cours introuvable avec id: " + id));

        modelMapper.map(courseDetailsDto, course);
        course.setUpdatedAt(LocalDate.now());
        Course updatedCourse = courseRepository.save(course);
        return modelMapper.map(updatedCourse, CourseDTO.class);
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cours introuvable avec id: " + id));
        if (course.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(course.getFilePath()));
            } catch (IOException e) {
                System.err.println("Impossible de supprimer le fichier: " + course.getFilePath() + " - " + e.getMessage());
            }
        }
        courseRepository.delete(course);
    }

    @Transactional
    public CourseDTO uploadCourseFile(MultipartFile file, String title, String description) throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toUpperCase();
        }

        String fileName = UUID.randomUUID().toString() + "." + fileExtension;
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        Course course = new Course();
        course.setTitle(title);
        course.setDescription(description);
        course.setFilePath(filePath.toString());
        course.setFileType(determineFileType(fileExtension));
        course.setCreatedAt(LocalDate.now());
        course.setUpdatedAt(LocalDate.now());
        course = courseRepository.save(course);

        String fileText = extractTextFromFile(file, fileExtension);
        createChaptersFromText(course, fileText);

        return modelMapper.map(course, CourseDTO.class);
    }

    private FileType determineFileType(String fileExtension) {
        return switch (fileExtension) {
            case "PDF" -> FileType.PDF;
            case "DOC", "DOCX" -> FileType.WORD;
            case "TXT" -> FileType.TEXT;
            default -> null;
        };
    }

    private String extractTextFromFile(MultipartFile file, String fileExtension) throws IOException {
        switch (fileExtension) {
            case "PDF":
                try (PDDocument document = PDDocument.load(file.getInputStream())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(document);
                }
            case "DOC":
                try (HWPFDocument doc = new HWPFDocument(file.getInputStream())) {
                    WordExtractor extractor = new WordExtractor(doc);
                    return extractor.getText();
                }
            case "DOCX":
                try (XWPFDocument docx = new XWPFDocument(file.getInputStream())) {
                    XWPFWordExtractor extractor = new XWPFWordExtractor(docx);
                    return extractor.getText();
                }
            case "TXT":
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    return sb.toString();
                }
            default:
                throw new IOException("Type de fichier non pris en charge pour l'extraction de texte: " + fileExtension);
        }
    }

    // Découpe le texte en chapitres et les enregistre en base.
    private void createChaptersFromText(Course course, String text) {
        List<ChapterExtractor.ExtractedChapter> extractedChapters = chapterExtractor.extract(text);

        int chapterNumber = 1;
        for (ChapterExtractor.ExtractedChapter extracted : extractedChapters) {
            Chapter chapter = new Chapter();
            chapter.setTitle(extracted.title());
            chapter.setChapterNumber(chapterNumber++);
            chapter.setContent(extracted.content());
            chapter.setCourse(course);
            chapter.setCreatedAt(LocalDate.now());
            chapterRepository.save(chapter);
        }
    }

}