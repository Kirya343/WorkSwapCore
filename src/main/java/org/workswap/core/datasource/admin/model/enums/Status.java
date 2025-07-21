package org.workswap.core.datasource.admin.model.enums;

public enum Status {
    NEW("Новая"),
    IN_PROGRESS("В процессе"),
    COMPLETED("Завершена"),
    CANCELED("Отменена");

    private final String displayName;

    Status(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}