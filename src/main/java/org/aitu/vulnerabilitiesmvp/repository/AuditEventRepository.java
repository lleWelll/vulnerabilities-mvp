package org.aitu.vulnerabilitiesmvp.repository;

import org.aitu.vulnerabilitiesmvp.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
}
