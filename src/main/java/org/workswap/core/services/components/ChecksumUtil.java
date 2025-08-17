package org.workswap.core.services.components;

import java.io.InputStream;
import java.security.MessageDigest;
import org.springframework.web.multipart.MultipartFile;

public class ChecksumUtil {

    public static String getSHA256Checksum(MultipartFile file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = file.getInputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();

        // Переводим в hex строку
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}