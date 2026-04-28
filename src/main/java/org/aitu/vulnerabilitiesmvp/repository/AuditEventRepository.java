package org.aitu.vulnerabilitiesmvp.repository;

import java.util.Optional;
import org.aitu.vulnerabilitiesmvp.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    Optional<AuditEvent> findTopByOrderByIdDesc();
}
