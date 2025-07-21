package org.workswap.core.services.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.workswap.core.datasource.admin.model.Task;
import org.workswap.core.datasource.admin.repository.TaskRepository;
import org.workswap.core.datasource.central.model.enums.SearchModelParamType;
import org.workswap.core.services.TaksService;

import lombok.RequiredArgsConstructor;

@Service
@Profile("Backoffice")
@RequiredArgsConstructor
public class TaskServiceImpl implements TaksService {

    private final TaskRepository taskRepository;
    
    @Override 
    public Task findTask(String param, String paramType) {
        SearchModelParamType searchParamType = SearchModelParamType.valueOf(paramType);
        switch (searchParamType) {
            case ID:
                return taskRepository.findById(Long.parseLong(param)).orElse(null);
            case NAME:
                return taskRepository.findByName(param);
            default:
                throw new IllegalArgumentException("Unknown param type: " + paramType);
        }
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
