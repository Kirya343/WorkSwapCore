package org.workswap.core.services.impl;

import java.util.Comparator;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.workswap.datasource.admin.model.Task;
import org.workswap.datasource.admin.model.TaskComment;
import org.workswap.datasource.admin.repository.TaskRepository;
import org.workswap.common.dto.task.TaskCommentDTO;
import org.workswap.common.dto.task.TaskDTO;
import org.workswap.common.dto.user.UserDTO;
import org.workswap.common.enums.SearchModelParamType;
import org.workswap.common.enums.TaskStatus;
import org.workswap.common.enums.TaskType;
import org.workswap.core.services.TaskService;
import org.workswap.core.services.components.ServiceUtils;
import org.workswap.core.services.mapping.UserMappingService;
import org.workswap.core.services.query.UserQueryService;

import lombok.RequiredArgsConstructor;

@Service
@Profile("backoffice")
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ServiceUtils serviceUtils;
    private final UserMappingService userMappingService;
    private final UserQueryService userQueryService;

    private Task findTaskFromRepostirory(String param, SearchModelParamType paramType) {
        switch (paramType) {
            case ID:
                return taskRepository.findById(Long.parseLong(param)).orElse(null);
            case NAME:
                return taskRepository.findByName(param); // если есть такой метод
            default:
                throw new IllegalArgumentException("Unknown param type: " + paramType);
        }
    }

    @Override
    public Task findTask(String param) {
        SearchModelParamType paramType = serviceUtils.detectParamType(param);
        return findTaskFromRepostirory(param, paramType);
    }

    @Override
    public List<Task> getSortedTasks(String sort, String type, String status) {

        List<Task> tasks = taskRepository.findAll();

        if (type != null) {
            tasks.removeIf(task -> task.getTaskType() != TaskType.valueOf(type));
        }

        if (status != null) {
            tasks.removeIf(task -> task.getStatus() != TaskStatus.valueOf(status));
        }

        Comparator<Task> comparator;

        switch (sort) {
            case "created":
                comparator = Comparator.comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
                break;
            case "deadline":
                comparator = Comparator.comparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "name":
                comparator = Comparator.comparing(Task::getName, String.CASE_INSENSITIVE_ORDER);
                break;
            case "status":
                comparator = Comparator.comparing(Task::getStatus);
                break;
            default:
                comparator = Comparator.comparing(Task::getId); // сортировка по id по умолчанию
        }

        tasks.sort(comparator);

        return tasks;
    }

    @Override
    public void createTask() {

    }

    @Override
    public void deleteTask() {

    }

    @Override
    public void updateTask() {

    }

    @Override
    public TaskDTO convertToDto(Task task) {

        Long executorId = task.getExecutorId();
        Long authorId = task.getAuthorId();

        UserDTO executor = null;
        if (executorId != null) {
            executor = userMappingService.toDto(userQueryService.findUser(executorId.toString()));
        }
        UserDTO author = userMappingService.toDto(userQueryService.findUser(authorId.toString()));

        return new TaskDTO(
            task.getId(),
            task.getName(),
            task.getDescription(),
            task.getStatus().getDisplayName(),
            task.getTaskType().getDisplayName(),
            executorId,
            authorId,
            author,
            executor,
            task.getCreatedAt(),
            task.getDeadline(),
            task.getCompleted()
        );
    }

    @Override
    public TaskDTO convertToShortDto(Task task) {

        Long executorId = task.getExecutorId();
        Long authorId = task.getAuthorId();

        return new TaskDTO(
            task.getId(),
            task.getName(),
            task.getDescription(),
            task.getStatus().getDisplayName(),
            task.getTaskType().getDisplayName(),
            executorId,
            authorId,
            null,
            null,
            task.getCreatedAt(),
            task.getDeadline(),
            task.getCompleted()
        );
    }

    @Override
    public TaskCommentDTO convertCommentToDto(TaskComment comment) {

        Long authorId = comment.getAuthorId();

        UserDTO user = userMappingService.toDto(userQueryService.findUser(authorId.toString()));

        return new TaskCommentDTO(
            comment.getId(),
            comment.getContent(),
            authorId,
            comment.getTask().getId(),
            comment.getCreatedAt(),
            user
        );
    }
}
