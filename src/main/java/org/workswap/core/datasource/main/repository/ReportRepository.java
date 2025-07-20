package org.workswap.core.datasource.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.workswap.core.datasource.main.model.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {
    
}
