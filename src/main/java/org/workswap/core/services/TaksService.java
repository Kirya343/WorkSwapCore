package org.workswap.core.services;

import org.workswap.core.datasource.admin.model.Task;

public interface TaksService {

    Task findTask(String param);

    void createTask();
    void deleteTask();
    void updateTask();
}
