package com.tiktokinsight.auth.api;

import com.tiktokinsight.auth.application.AccountAdministrationService;
import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.audit.application.AuditService;
import com.tiktokinsight.auth.dto.UpdateUserStatusRequest;
import com.tiktokinsight.auth.dto.UserResponse;
import com.tiktokinsight.common.api.ApiResponse;
import com.tiktokinsight.common.logging.RequestIdFilter;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AccountAdministrationController {

    private final AccountAdministrationService accountAdministrationService;
    private final AuditService auditService;

    public AccountAdministrationController(AccountAdministrationService accountAdministrationService, AuditService auditService) {
        this.accountAdministrationService = accountAdministrationService;
        this.auditService = auditService;
    }

    @PutMapping("/{userId}/status")
    public ApiResponse<UserResponse> updateStatus(
            @PathVariable long userId,
            @Valid @RequestBody UpdateUserStatusRequest request
            , @AuthenticationPrincipal AccessPrincipal principal, HttpServletRequest servletRequest
    ) {
        var user = UserResponse.from(accountAdministrationService.updateStatus(userId, request.status()));
        auditService.record(principal, request.status().name().equals("DISABLED") ? "USER_DISABLED" : "USER_ENABLED", "USER", String.valueOf(userId), "SUCCESS", "{\"status\":\"" + request.status() + "\"}", servletRequest);
        return ApiResponse.success(
                user,
                RequestIdFilter.currentRequestId()
        );
    }
}
