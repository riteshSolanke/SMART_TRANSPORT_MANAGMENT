package com.transport.analyticsservice.repository;

import com.transport.analyticsservice.entity.ServiceReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "eureka.client.enabled=false"
})
class ServiceReportRepositoryIntegrationTest {
    @Autowired
    private ServiceReportRepository repository;

    @Test
    void persistsAndOrdersGeneratedReports() {
        ServiceReport older = repository.save(report(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)));
        ServiceReport newer = repository.save(report(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)));

        assertThat(repository.findAllByOrderByGeneratedAtDesc())
                .containsExactly(newer, older);
    }

    private ServiceReport report(LocalDate from, LocalDate to) {
        ServiceReport report = new ServiceReport();
        report.setPeriodStart(from);
        report.setPeriodEnd(to);
        report.setGeneratedBy(7L);
        report.setTotalTicketRecords(10L);
        report.setConfirmedTickets(8L);
        report.setTotalPassengers(12L);
        report.setCancelledTickets(2L);
        report.setTotalPaymentAttempts(9L);
        report.setSuccessfulPayments(8L);
        report.setFailedPayments(1L);
        report.setRefundedPayments(1L);
        report.setGrossRevenue(new BigDecimal("100.00"));
        report.setRefundedAmount(new BigDecimal("10.00"));
        report.setTotalRevenue(new BigDecimal("90.00"));
        report.setTotalAssignments(4L);
        report.setCompletedAssignments(3L);
        report.setCancelledAssignments(1L);
        report.setOnTimeAssignments(2L);
        report.setOnTimePerformance(new BigDecimal("66.67"));
        return report;
    }
}
