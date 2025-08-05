package org.workswap.core.objects;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

@Getter
public class TranslationEntry {

    private String code; // ключ переменной
    private Map<String, String> translations = new HashMap<>(); // язык → перевод

    public TranslationEntry(String code) {
        this.code = code;
    }

    public void addTranslation(String lang, String value) {
        translations.put(lang, value);
    }
}

