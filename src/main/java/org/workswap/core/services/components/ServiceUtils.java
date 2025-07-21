package org.workswap.core.services.components;

import org.springframework.stereotype.Component;
import org.workswap.core.datasource.central.model.enums.SearchModelParamType;

@Component
public class ServiceUtils {
    public SearchModelParamType detectParamType(String param) {
        if (param == null || param.isBlank()) {
            throw new IllegalArgumentException("Search parameter cannot be null or empty");
        }

        if (param.length() >= 15 && param.matches("^[0-9]+$")) {
            return SearchModelParamType.SUB;
        }

        if (param.matches("^\\d+$")) {
            return SearchModelParamType.ID;
        }

        if (param.contains("@")) {
            return SearchModelParamType.EMAIL;
        }

        return SearchModelParamType.NAME;
    }
}
