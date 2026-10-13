package com.futmanager.service;

import com.futmanager.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class StorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    private final Path rootLocation;
    private final String storageType;

    public StorageService(
            @Value("${futmanager.storage.local-dir:./uploads}") String localDir,
            @Value("${futmanager.storage.type:local}") String storageType) {
        this.rootLocation = Paths.get(localDir).toAbsolutePath().normalize();
        this.storageType = storageType;
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            log.error("Não foi possível criar o diretório de uploads local: {}", e.getMessage());
        }
    }

    /**
     * Valida e armazena arquivo de foto de forma segura.
     * Retorna a URL relativa para acesso.
     */
    public String storePhoto(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("O arquivo de foto enviado está vazio.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("O arquivo excede o tamanho máximo permitido de 5MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("Formato inválido. Permitidos apenas arquivos JPEG, PNG ou WEBP.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "foto.jpg");
        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException("Extensão de arquivo não permitida.");
        }

        // Nome de arquivo opaco gerado aleatoriamente (prevenção contra Directory Traversal e colisão)
        String safeFileName = UUID.randomUUID() + "." + extension.toLowerCase();

        try {
            Path destinationFile = this.rootLocation.resolve(safeFileName).normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                throw new BusinessException("Tentativa de armazenamento fora do diretório permitido.");
            }

            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Foto armazenada com sucesso: {}", safeFileName);

            // Retorna endpoint de acesso seguro da API
            return "/api/photos/" + safeFileName;
        } catch (IOException e) {
            log.error("Falha ao salvar arquivo de foto: {}", e.getMessage());
            throw new BusinessException("Falha ao processar o upload da foto.");
        }
    }

    /**
     * Carrega arquivo como recurso.
     */
    public Resource loadAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize().toAbsolutePath();
            if (!file.startsWith(this.rootLocation.toAbsolutePath())) {
                throw new BusinessException("Acesso negado ao arquivo solicitado.");
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new BusinessException("Arquivo de foto não encontrado ou não legível.");
            }
        } catch (MalformedURLException e) {
            throw new BusinessException("URL de foto inválida.");
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "jpg";
    }
}
