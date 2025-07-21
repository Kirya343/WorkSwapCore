package org.workswap.core.datasource.central.model.enums;

public enum PriceType {
    PER_DAY("per-day"),
    PER_HOUR("per-hour"),
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