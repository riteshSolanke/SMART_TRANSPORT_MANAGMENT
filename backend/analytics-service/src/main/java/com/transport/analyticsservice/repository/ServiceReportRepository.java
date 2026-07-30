package com.transport.analyticsservice.repository;

import com.transport.analyticsservice.entity.ServiceReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceReportRepository
        extends JpaRepository<ServiceReport, Long> {
    List<ServiceReport> findAllByOrderByGeneratedAtDesc();
}
