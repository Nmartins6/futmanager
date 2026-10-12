package com.futmanager.controller;

import com.futmanager.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@RestController
@RequestMapping("/photos")
@RequiredArgsConstructor
@Tag(name = "Fotos e Arquivos", description = "Upload e exibição segura de fotos de alunos")
public class PhotoController {

    private final StorageService storageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Fazer upload de foto do aluno (JPEG, PNG ou WEBP até 5MB)")
    public ResponseEntity<Map<String, String>> uploadPhoto(@RequestParam("file") MultipartFile file) {
        String photoUrl = storageService.storePhoto(file);
        return ResponseEntity.ok(Map.of("photoUrl", photoUrl));
    }

    @GetMapping("/{filename:.+}")
    @Operation(summary = "Exibir foto do aluno com tipo de conteúdo dinâmico")
    public ResponseEntity<Resource> servePhoto(@PathVariable String filename) {
        Resource file = storageService.loadAsResource(filename);

        String contentType = "image/jpeg";
        try {
            String probeType = Files.probeContentType(file.getFile().toPath());
            if (probeType != null) {
                contentType = probeType;
            }
        } catch (IOException ignored) {
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(file);
    }
}
