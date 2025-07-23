package org.workswap.core.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.workswap.config.LocalisationConfig.LanguageUtils;
import org.workswap.datasource.admin.model.objects.TranslationEntry;

@Service
public class LocalizationService {
    public List<TranslationEntry> loadTranslations(Path langDir) throws IOException {
        Map<String, TranslationEntry> entries = new TreeMap<>();

        for (String lang : LanguageUtils.SUPPORTED_LANGUAGES) {
            Path filePath = langDir.resolve("messages_" + lang + ".properties");

            if (!Files.exists(filePath)) {
                System.out.println("Файл не найден для языка: " + lang);
                continue;
            }

            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(filePath)) {
                props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
            }

            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                entries
                    .computeIfAbsent(key, TranslationEntry::new)
                    .addTranslation(lang, value);
            }
        }

        return new ArrayList<>(entries.values());
    }
}
