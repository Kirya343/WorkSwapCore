package org.workswap.core.services;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;
import org.workswap.common.enums.FileType;

public interface StorageService {

    String storeFile(MultipartFile file, FileType fileType, Long entityId) throws IOException;

    String storeListingImage(MultipartFile file, Long listingId) throws IOException;
    String storeAvatar(MultipartFile file, Long userId) throws IOException;
    String storeNewsImage(MultipartFile file, Long newsId) throws IOException;
    String storeResume(MultipartFile file, Long userId) throws IOException;

    void deleteFile(String filePath) throws IOException;
    void deleteImage(String filename) throws IOException;

    String getExtension(String filename);
    String getBaseName(String filename);
}
