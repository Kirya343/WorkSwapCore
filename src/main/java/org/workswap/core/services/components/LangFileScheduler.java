package org.workswap.core.services.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.workswap.config.LocalisationConfig.LanguageUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class LangFileScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LangFileScheduler.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final Path destinationBasePath = Path.of("lang");

    @Value("${api.url}")
    private String apiUrl;

    @Value("${api.key.localization}")
    private String localizationApiKey;

    @Scheduled(fixedRate = 5 * 30 * 1000) // каждые 30 секунд
    public void downloadAllLangFiles() {

        String baseUrl = apiUrl + "/api/lang";

        List<String> savedLangs = new ArrayList<>(LanguageUtils.SUPPORTED_LANGUAGES);
        Set<Path> downloadedFiles = new HashSet<>();

        try {
            for (String locale : LanguageUtils.SUPPORTED_LANGUAGES) {
                // 1. Получаем список всех файлов с нужной локалью
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-API-KEY", localizationApiKey);

                HttpEntity<Void> entity = new HttpEntity<>(headers);

                String listUrl = baseUrl + "/files/" + locale;
                ResponseEntity<String[]> response = restTemplate.exchange(
                    listUrl, HttpMethod.GET, entity, String[].class);
                String[] filePaths = response.getBody();

                if (filePaths == null || filePaths.length == 0) {
                    logger.error("Файлы локализации не найдены для {}", locale);
                    continue;
                }

                for (String filePathStr : filePaths) {
                    String[] parts = filePathStr.split("/");
                    if (parts.length != 2) {
                    logger.error("Некорректный путь файла: {}", filePathStr);
                        continue;
                    }

                    String folder = parts[0];
                    String fileName = parts[1];

                    String fileUrl = baseUrl + "/" + folder + "/" + fileName;
                    Path destinationPath = destinationBasePath.resolve(folder).resolve(fileName);

                    try {
                        byte[] fileBytes = restTemplate.getForObject(fileUrl, byte[].class);
                        if (fileBytes != null) {
                            Files.createDirectories(destinationPath.getParent());
                            Files.write(destinationPath, fileBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                            downloadedFiles.add(destinationPath);
                        } else {
                            savedLangs.remove(locale);
                        }
                    } catch (HttpClientErrorException.NotFound e) {
                        savedLangs.remove(locale);
                    } catch (Exception e) {
                        logger.error("Ошибка при загрузке {}: {}", fileUrl, e.getMessage());
                        savedLangs.remove(locale);
                    }
                }
            }

            // 4. Удаляем файлы, которых нет в downloadedFiles
            if (Files.exists(destinationBasePath)) {
                Files.walk(destinationBasePath)
                    .filter(Files::isRegularFile)
                    .filter(path -> !downloadedFiles.contains(path))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            logger.debug("Удалён устаревший файл: {}", path);
                        } catch (IOException e) {
                            logger.error("Не удалось удалить файл: {} — {}", path, e.getMessage());
                        }
                    });
            }

            logger.info("Файлы локализации синхронизированы: {}", savedLangs);

        } catch (Exception e) {
            logger.error("Ошибка при получении списка файлов: {}", e.getMessage());
        }
    }
}
