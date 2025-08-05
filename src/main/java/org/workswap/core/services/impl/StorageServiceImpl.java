package org.workswap.core.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.workswap.common.enums.FileType;
import org.workswap.core.services.StorageService;

import net.coobird.thumbnailator.Thumbnails;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class StorageServiceImpl implements StorageService {
    private final Path rootLocation = Paths.get("uploads").toAbsolutePath().normalize();
    private final long maxFileSize = 20 * 1024 * 1024; // 20MB

    public StorageServiceImpl() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, FileType fileType, Long entityId) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file");
        }
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File size exceeds size limit");
        }

        // Получаем расширение оригинального файла
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase() : "";

        if (!fileType.getAllowedExtensions().contains(fileExtension)) {
            throw new RuntimeException("Only " + fileType.getAllowedExtensions() + " files are allowed");
        }

        // Папка для хранения
        Path targetPath = this.rootLocation.resolve(fileType.getDirectory());
        if (!Files.exists(targetPath)) {
            Files.createDirectories(targetPath);
        }

        // Новое имя файла — с .webp если изображение
        String finalExtension = (fileType.getWidth() != null) ? ".webp" : fileExtension;
        String filename = fileType.getPrefix() +
                (entityId != null ? entityId + "_" : "") +
                UUID.randomUUID().toString() + finalExtension;

        Path destinationFile = targetPath.resolve(filename).normalize();

        if (!destinationFile.getParent().equals(targetPath)) {
            throw new RuntimeException("Cannot store file outside the target directory");
        }

        // Обработка
        if (fileType.getWidth() != null) {
            try (var inputStream = file.getInputStream()) {
                Thumbnails.of(inputStream)
                        .size(fileType.getWidth(), fileType.getHeight())
                        .outputQuality(fileType.getQuality())
                        .outputFormat("webp")
                        .toFile(destinationFile.toFile());
            }
        } else {
            Files.copy(file.getInputStream(), destinationFile);
        }

        return "/" + fileType.getDirectory() + "/" + filename;
    }


    @Override
    public String storeListingImage(MultipartFile file, Long listingId) throws IOException {
        return storeFile(file, FileType.LISTING_IMAGE, listingId);
    }

    @Override
    public String storeAvatar(MultipartFile file, Long userId) throws IOException {
        return storeFile(file, FileType.AVATAR, userId);
    }

    @Override
    public String storeNewsImage(MultipartFile file, Long newsId) throws IOException {
        return storeFile(file, FileType.NEWS_IMAGE, newsId);
    }

    @Override
    public String storeResume(MultipartFile file, Long userId) throws IOException {
        return storeFile(file, FileType.RESUME, userId);
    }

    @Override
    public void deleteFile(String filePath) throws IOException {
        Path fileToDelete = rootLocation.resolve(filePath).normalize();
        Files.deleteIfExists(fileToDelete);
    }

    @Override
    public void deleteImage(String filename) throws IOException {
        Path filePath = rootLocation.resolve(filename).normalize();
        Files.deleteIfExists(filePath);
    }

    @Override
    public String getExtension(String filename) {
        int lastDot = filename.lastIndexOf(".");
        return lastDot != -1 ? filename.substring(lastDot).toLowerCase() : "";
    }

    @Override
    public String getBaseName(String filename) {
        int lastDot = filename.lastIndexOf(".");
        return lastDot != -1 ? filename.substring(0, lastDot) : filename;
    }
}