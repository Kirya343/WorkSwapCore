package org.workswap.core.datasource.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.workswap.core.datasource.admin.model.Task;
import org.workswap.core.datasource.admin.model.Task.Status;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(Status status);
    Task findByName(String name);

    List<Task> findByExecutorId(Long executorId);
}
