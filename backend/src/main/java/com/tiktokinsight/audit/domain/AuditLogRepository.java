package com.tiktokinsight.audit.domain;

import com.tiktokinsight.common.api.PageResponse;
import java.time.Instant;

public interface AuditLogRepository {
    void append(Long operatorUserId, String operatorEmail, String actionType, String targetType, String targetId,
                String requestId, String requestIp, String result, String detail, Instant createdAt);
    PageResponse<AuditLog> find(String actionType, String targetType, int page, int pageSize);
}
