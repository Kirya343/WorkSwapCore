package org.workswap.core.services;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.workswap.core.objects.TranslationEntry;

public interface LocalizationService {
    
    Map<String, TranslationEntry> loadTranslations(Path langDir, String groupName) throws IOException;
    List<String> loadGroups(Path baseDir) throws IOException;
    void createTranslation(String pathToFile, String localizationCode, String lang, String translation) throws IOException;
    void removeTranslation(String directoryPath, String localizationCode) throws IOException;
    
}
