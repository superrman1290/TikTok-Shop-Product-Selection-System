package com.tiktokinsight.audit.api;

import com.tiktokinsight.audit.application.AuditService;
import com.tiktokinsight.audit.domain.AuditLog;
import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.common.api.ApiResponse;
import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.common.logging.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/audit-logs")
public class AuditLogController {
    private final AuditService service; public AuditLogController(AuditService service){this.service=service;}
    @GetMapping public ApiResponse<PageResponse<AuditLog>> list(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,@RequestParam(required=false)String actionType,@RequestParam(required=false)String targetType,@AuthenticationPrincipal AccessPrincipal p,HttpServletRequest h){return ApiResponse.success(service.list(actionType,targetType,page,pageSize,p,h),RequestIdFilter.currentRequestId());}
}
