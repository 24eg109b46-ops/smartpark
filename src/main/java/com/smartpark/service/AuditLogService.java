package com.smartpark.service;

import com.smartpark.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    AuditLog log(Long userId, String userEmail, String action, String details, String ipAddress);

    Page<AuditLog> getAuditLogs(Pageable pageable);

    Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable);
}
