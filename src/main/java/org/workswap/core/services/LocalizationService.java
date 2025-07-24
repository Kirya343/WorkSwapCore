package org.workswap.core.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public void createTranslation(String pathToFile, String localizationCode, String lang, String translation) throws IOException {
        String lineToAdd = localizationCode + "=" + translation;

        // Формируем путь к файлу локализации
        String filename = pathToFile + "_" + lang + ".properties";
        File file = new File(filename);

        // Убедимся, что файл существует
        if (!file.exists()) {
            file.getParentFile().mkdirs(); // Создаём папки, если нужно
            file.createNewFile();          // Создаём сам файл
        }

        // Загружаем все свойства из файла
        Properties props = new Properties();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            props.load(reader);
        }

        // Проверяем, есть ли уже такой ключ
        if (props.containsKey(localizationCode)) {
            return; // Ничего не делаем — перевод уже есть
        }

        // Добавляем новую строку в конец файла
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
            writer.newLine();
            writer.write(lineToAdd);
        }
    }

    public void removeTranslation(String directoryPath, String localizationCode) throws IOException {
        Path dir = Paths.get(directoryPath);

        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            System.out.println("Директория не найдена: " + directoryPath);
            return;
        }

        List<String> groups = loadGroups(dir); // <- возвращает список поддиректорий

        for (String lang : LanguageUtils.SUPPORTED_LANGUAGES) {
            boolean found = false;

            for (String group : groups) {

                System.out.println("Обрабатываем группу: " + group);
                Path groupPath = dir.resolve(group); // <-- вот тут важно

                try (DirectoryStream<Path> stream = Files.newDirectoryStream(groupPath, "*_" + lang + ".properties")) {
                    for (Path filePath : stream) {
                        String fileName = filePath.getFileName().toString();

                        System.out.println("Обрабатываем файл: " + fileName);

                        File file = filePath.toFile();
                        Properties props = new Properties();

                        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                            props.load(reader);
                        }

                        if (!props.containsKey(localizationCode)) {
                            continue;
                        }

                        props.remove(localizationCode);
                        found = true;

                        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                                new FileOutputStream(file, false), StandardCharsets.UTF_8))) {
                            props.store(writer, null);
                        }

                        System.out.println("Удалён перевод (" + lang + ") из файла: " + group + "/" + fileName + ", ключ: " + localizationCode);
                    }
                }
            }

            if (!found) {
                System.out.println("Ключ не найден для языка " + lang + ": " + localizationCode);
            }
        }
    }
}
