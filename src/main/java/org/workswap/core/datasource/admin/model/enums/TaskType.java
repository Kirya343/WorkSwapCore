package org.workswap.core.datasource.admin.model.enums;

public enum TaskType {
    DEVELOPMENT("Разработка"),
    CONTENT_UPDATE("Дополнение контента"),
    MODERATION("Модерация"),
    DESIGN("Дизайн"),
    TESTING("Тестирование"),
    MAINTENANCE("Обслуживание"),
    SUPPORT("Поддержка");

    private final String displayName;

    TaskType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
