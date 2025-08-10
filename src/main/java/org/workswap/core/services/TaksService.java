package org.workswap.core.services;

import java.util.List;

import org.workswap.datasource.admin.model.Task;

public interface TaksService {

    Task findTask(String param);

    List<Task> getSortedTasks(String sort, String type, String status);

    void createTask();
    void deleteTask();
    void updateTask();
}
