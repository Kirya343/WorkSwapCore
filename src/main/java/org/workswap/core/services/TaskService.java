package org.workswap.core.services;

import java.util.List;

import org.workswap.common.dto.TaskCommentDTO;
import org.workswap.common.dto.TaskDTO;
import org.workswap.datasource.admin.model.Task;
import org.workswap.datasource.admin.model.TaskComment;

public interface TaskService {

    Task findTask(String param);

    List<Task> getSortedTasks(String sort, String type, String status);

    void createTask();
    void deleteTask();
    void updateTask();

    TaskDTO convertToDto(Task task);
    TaskDTO convertToShortDto(Task task);
    TaskCommentDTO convertCommentToDto(TaskComment comment);
}
