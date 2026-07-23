package com.tiktokinsight.audit.domain;

import java.time.Instant;

public record AuditLog(long id, Long operatorUserId, String operatorEmail, String actionType,
                       String targetType, String targetId, String requestId, String requestIp,
                       String result, String detail, Instant createdAt) { }
