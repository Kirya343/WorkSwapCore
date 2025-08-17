package org.workswap.core.services.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.workswap.common.enums.FileType;
import org.workswap.core.services.StorageService;
import org.workswap.core.services.components.ChecksumUtil;
import org.workswap.datasource.cloud.model.File;
import org.workswap.datasource.cloud.repository.FileRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Profile("cloud")
public class StorageServiceImpl implements StorageService {

    private final Path rootLocation = Paths.get("files").toAbsolutePath().normalize();
    private final long maxFileSize = 20 * 1024 * 1024; // 20MB
    private final FileRepository fileRepository;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, FileType fileType, Long entityId, String ownerSub) throws Exception {
        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file");
        }
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File size exceeds size limit");
        }
        if (!fileType.getAllowedMimeTypes().contains(file.getContentType())) {
            throw new RuntimeException("Неподдерживаемый MIME-тип файла");
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

        Long fileSize = file.getSize();

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

        File fileItem = new File(filename,
                                 fileExtension,
                                 file.getContentType(),
                                 ChecksumUtil.getSHA256Checksum(file),
                                 fileType.getVisibility(),
                                 fileSize,
                                 "/" + fileType.getDirectory() + "/" + filename,
                                 ownerSub);

        File savedFile = fileRepository.save(fileItem);

        return savedFile.getStoragePath();
    }


    @Override
    public String storeListingImage(MultipartFile file, Long listingId, String ownerSub) throws Exception {
        return storeFile(file, FileType.LISTING_IMAGE, listingId, ownerSub);
    }

    @Override
    public String storeAvatar(MultipartFile file, Long userId, String userSub) throws Exception {
        return storeFile(file, FileType.AVATAR, userId, userSub);
    }

    @Override
    public String storeNewsImage(MultipartFile file, Long newsId) throws Exception {
        return storeFile(file, FileType.NEWS_IMAGE, newsId, null);
    }

    @Override
    public String storeResume(MultipartFile file, Long userId, String userSub) throws Exception {
        return storeFile(file, FileType.RESUME, userId, userSub);
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