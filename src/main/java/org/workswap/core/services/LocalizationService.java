package org.workswap.core.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.workswap.config.LocalisationConfig.LanguageUtils;
import org.workswap.datasource.admin.model.objects.TranslationEntry;

@Service
public class LocalizationService {

    public Map<String, TranslationEntry> loadTranslations(Path langDir, String groupName) throws IOException {
        System.out.println("Начинаем загрузку переводов из: " + langDir);
        Map<String, TranslationEntry> entries = new TreeMap<>();

        for (String lang : LanguageUtils.SUPPORTED_LANGUAGES) {
            Path filePath = langDir.resolve(groupName + "_" + lang + ".properties");

            if (!Files.exists(filePath)) {
                System.out.println("Файл не найден для языка: " + lang + " по пути: " + filePath);
                continue;
            }

            System.out.println("Файл найден, загружаем: " + filePath);
            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(filePath)) {
                props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
                System.out.println("Загружено ключей: " + props.size() + " для языка: " + lang);
            } catch (IOException e) {
                System.out.println("Ошибка чтения файла для языка " + lang + ": " + e.getMessage());
                throw e;
            }

            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                entries
                    .computeIfAbsent(key, TranslationEntry::new)
                    .addTranslation(lang, value);
            }
        }

        System.out.println("Загрузка завершена. Всего ключей: " + entries.size());
        return entries;
    }

    public List<String> loadGroups(Path baseDir) throws IOException {
        try (Stream<Path> paths = Files.list(baseDir)) {
            return paths
                .filter(Files::isDirectory)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
        }
    }
}
