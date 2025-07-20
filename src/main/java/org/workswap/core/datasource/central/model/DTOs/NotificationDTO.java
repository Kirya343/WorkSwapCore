package org.workswap.core.datasource.central.model.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationDTO {
    private String title;
    private String message;
    private String link;
}