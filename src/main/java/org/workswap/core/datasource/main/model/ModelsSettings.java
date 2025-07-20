package org.workswap.core.datasource.main.model;

public class ModelsSettings {

    // Параметры моделей
    public enum SearchParamType { 
        // Общие параметры
        ID,
        NAME,

        ALL, // Найти все элементы

        // User
        EMAIL,
        SUB,
        OAUTH2,
    }

    public enum ObjectType {
        MESSAGE,     
        LISTING, 
        REVIEW,   
        USER,                    
        OTHER                    
    }
}
