package org.workswap.core.services.impl;

import java.util.Comparator;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.workswap.datasource.admin.model.Task;
import org.workswap.datasource.admin.repository.TaskRepository;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.repository.UserRepository;
import org.workswap.common.enums.SearchModelParamType;
import org.workswap.common.enums.TaskStatus;
import org.workswap.common.enums.TaskType;
import org.workswap.core.services.TaksService;
import org.workswap.core.services.components.ServiceUtils;

import lombok.RequiredArgsConstructor;

@Service
@Profile("backoffice")
@RequiredArgsConstructor
public class TaskServiceImpl implements TaksService {

    private final TaskRepository taskRepository;
    private final ServiceUtils serviceUtils;
    private final UserRepository userRepository;

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

        for (Task task : tasks) {
            User executor = null;

            if (task.getExecutorId() != null) {
                executor = userRepository.findById(task.getExecutorId()).orElse(null);
            }

            User author = null;

            if (task.getExecutorId() != null) {
                author = userRepository.findById(task.getAuthorId()).orElse(null);
            }

            task.setExecutor(executor);
            task.setAuthor(author);
        }

        Comparator<Task> comparator;

        switch (sort) {
            case "created":
                comparator = Comparator.comparing(Task::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()));
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
}
