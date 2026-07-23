package com.tiktokinsight.audit.application;

import com.tiktokinsight.audit.domain.AuditLog;
import com.tiktokinsight.audit.domain.AuditLogRepository;
import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.common.exception.ApiException;
import com.tiktokinsight.common.logging.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository repository; private final Clock clock;
    public AuditService(AuditLogRepository repository, Clock clock) { this.repository = repository; this.clock = clock; }
    public void record(AccessPrincipal operator, String action, String target, String targetId, String result, String detail, HttpServletRequest request) {
        repository.append(operator == null ? null : operator.userId(), operator == null ? null : operator.email(), action, target, targetId, RequestIdFilter.currentRequestId(), request == null ? null : request.getRemoteAddr(), result, sanitize(detail), clock.instant());
    }
    public PageResponse<AuditLog> list(String action, String target, int page, int pageSize, AccessPrincipal operator, HttpServletRequest request) {
        if (page < 1 || pageSize < 1 || pageSize > 100) throw new ApiException(HttpStatus.BAD_REQUEST, com.tiktokinsight.common.api.ApiErrorCode.VALIDATION_FAILED);
        record(operator, "AUDIT_LOG_QUERY", "AUDIT_LOG", null, "SUCCESS", "{}", request);
        return repository.find(blank(action), blank(target), page, pageSize);
    }
    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String sanitize(String detail) { return detail == null || detail.isBlank() ? "{}" : detail.replaceAll("(?i)(password|token|secret|authorization)\\s*[:=]\\s*[^,}\\s]+", "$1=REDACTED"); }
}
