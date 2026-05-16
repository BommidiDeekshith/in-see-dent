package com.inseedent.repository;

import com.inseedent.domain.Runbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RunbookRepository extends JpaRepository<Runbook, Long> {
    List<Runbook> findByServiceName(String serviceName);
    
    List<Runbook> findByTitleContainingIgnoreCase(String keyword);

    boolean existsByTitle(String title);
}
