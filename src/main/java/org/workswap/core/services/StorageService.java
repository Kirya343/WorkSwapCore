package org.workswap.core.services;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;
import org.workswap.common.enums.FileType;

public interface StorageService {

    String storeFile(MultipartFile file, FileType fileType, Long entityId, String ownerSub) throws Exception;

    String storeListingImage(MultipartFile file, Long listingId, String userSub) throws Exception;
    String storeAvatar(MultipartFile file, Long userId, String userSub) throws Exception;
    String storeNewsImage(MultipartFile file, Long newsId) throws Exception;
    String storeResume(MultipartFile file, Long userId, String userSub) throws Exception;

    void deleteFile(String filePath) throws IOException;

    String getExtension(String filename);
    String getBaseName(String filename);
}
