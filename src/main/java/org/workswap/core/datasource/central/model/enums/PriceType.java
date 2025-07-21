package org.workswap.core.datasource.central.model.enums;

public enum PriceType {
    PER_DAY("per_day"),
    PER_HOUR("per_hour"),
    FIXED("fixed"),
    NEGOTIABLE("negotiable");

    private final String displayName;

    PriceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}