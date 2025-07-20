package org.workswap.core.datasource.main.model.DTOs;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class NewsForm {
    private Map<String, NewsTranslationDTO> translations = new HashMap<>();
}
