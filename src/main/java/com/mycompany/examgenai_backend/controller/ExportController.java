package com.mycompany.examgenai_backend.controller;

import com.mycompany.examgenai_backend.service.export.DocxExportService;
import com.mycompany.examgenai_backend.service.export.PdfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final PdfExportService pdfExportService;
    private final DocxExportService docxExportService;

    @GetMapping("/pdf/{examId}")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long examId) {
        byte[] pdfBytes = pdfExportService.generateExamPdf(examId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("exam-" + examId + ".pdf")
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @GetMapping("/docx/{examId}")
    public ResponseEntity<byte[]> exportDocx(@PathVariable Long examId) {
        byte[] docxBytes = docxExportService.generateExamDocx(examId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("exam-" + examId + ".docx")
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(docxBytes);
    }
}