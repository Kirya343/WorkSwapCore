package org.workswap.core.datasource.central.model.enums;

public enum ReportReason {
    SPAM,                    // Спам
    OFFENSIVE_LANGUAGE,      // Оскорбления / токсичное поведение
    INAPPROPRIATE_CONTENT,   // Неподобающий контент
    SCAM,                    // Мошенничество
    HARASSMENT,              // Преследование / домогательства
    HATE_SPEECH,             // Речь ненависти
    IMPERSONATION,           // Самозванство (выдаёт себя за другого)
    FALSE_INFORMATION,       // Ложная информация
    COPYRIGHT_VIOLATION,     // Нарушение авторских прав
    OTHER                    // Другая причина
}