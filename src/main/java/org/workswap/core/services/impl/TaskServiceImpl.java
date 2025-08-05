package org.workswap.core.services.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.workswap.datasource.admin.model.Task;
import org.workswap.datasource.admin.repository.TaskRepository;
import org.workswap.common.enums.SearchModelParamType;
import org.workswap.core.services.TaksService;
import org.workswap.core.services.components.ServiceUtils;

import lombok.RequiredArgsConstructor;

@Service
@Profile("backoffice")
@RequiredArgsConstructor
public class TaskServiceImpl implements TaksService {

    private final TaskRepository taskRepository;
    private final ServiceUtils serviceUtils;

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
    public void createTask() {

    }

    @Override
    public void deleteTask() {

    }

    @Override
    public void updateTask() {

    }
}
