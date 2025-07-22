package org.workswap.core.services.components;

import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.workswap.config.LocalisationConfig.LanguageUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Component
public class LangFileScheduler {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl = "http://localhost:8082/api/lang";
    private final Path destinationBasePath = Path.of("lang");

    @Scheduled(fixedRate = 5 * 30 * 1000) // каждые 30 секунд
    public void downloadAllLangFiles() {
        List<String> savedLangs = LanguageUtils.SUPPORTED_LANGUAGES;
        try {
            for(String locale : LanguageUtils.SUPPORTED_LANGUAGES) {
                // 1. Получаем список всех файлов с нужной локалью
                String listUrl = baseUrl + "/files/" + locale;
                ResponseEntity<String[]> response = restTemplate.getForEntity(listUrl, String[].class);
                String[] filePaths = response.getBody();

                if (filePaths == null || filePaths.length == 0) {
                    System.err.println("Файлы локализации не найдены.");
                    return;
                }

                for (String filePathStr : filePaths) {
                    // 2. Разбиваем путь: "global/messages_ru.properties"
                    String[] parts = filePathStr.split("/");
                    if (parts.length != 2) {
                        System.err.println("Некорректный путь файла: " + filePathStr);
                        continue;
                    }

                    String folder = parts[0];
                    String fileName = parts[1];

                    // 3. Формируем URL запроса файла
                    String fileUrl = baseUrl + "/" + folder + "/" + fileName;
                    Path destinationPath = destinationBasePath.resolve(folder).resolve(fileName);

                    try {
                        byte[] fileBytes = restTemplate.getForObject(fileUrl, byte[].class);
                        if (fileBytes != null) {
                            Files.createDirectories(destinationPath.getParent());
                            Files.write(destinationPath, fileBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                            //System.out.println("Файл обновлён: " + destinationPath);
                        } else {
                            //System.err.println("Файл не получен (пустой): " + fileUrl);
                            savedLangs.remove(locale);
                        }
                    } catch (HttpClientErrorException.NotFound e) {
                        //System.out.println("Файл не найден (и это нормально): " + fileUrl);
                        savedLangs.remove(locale);
                    } catch (Exception e) {
                        System.err.println("Ошибка при загрузке " + fileUrl + ": " + e.getMessage());
                        savedLangs.remove(locale);
                    }
                }
            }
            System.out.println("Файлы локализации обновлёны: " + savedLangs);

        } catch (Exception e) {
            System.err.println("Ошибка при получении списка файлов: " + e.getMessage());
        }
    }
}