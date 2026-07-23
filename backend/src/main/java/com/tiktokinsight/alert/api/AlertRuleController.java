package com.tiktokinsight.alert.api;

import com.tiktokinsight.alert.application.AlertRuleService;
import com.tiktokinsight.alert.domain.AlertRule;
import com.tiktokinsight.alert.dto.AlertRuleRequest;
import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.common.api.ApiResponse;
import com.tiktokinsight.common.logging.RequestIdFilter;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/alert-rules")
public class AlertRuleController {
    private final AlertRuleService service;
    public AlertRuleController(AlertRuleService service) { this.service = service; }
    @GetMapping public ApiResponse<List<AlertRule>> list(@AuthenticationPrincipal AccessPrincipal principal) { return ApiResponse.success(service.list(principal.userId()), RequestIdFilter.currentRequestId()); }
    @PostMapping public ApiResponse<AlertRule> create(@AuthenticationPrincipal AccessPrincipal principal, @Valid @RequestBody AlertRuleRequest request) { return ApiResponse.success(service.create(principal.userId(), request), RequestIdFilter.currentRequestId()); }
    @PutMapping("/{ruleId}") public ApiResponse<AlertRule> update(@AuthenticationPrincipal AccessPrincipal principal, @PathVariable long ruleId, @Valid @RequestBody AlertRuleRequest request) { return ApiResponse.success(service.update(principal.userId(), ruleId, request), RequestIdFilter.currentRequestId()); }
    @DeleteMapping("/{ruleId}") public ApiResponse<Void> delete(@AuthenticationPrincipal AccessPrincipal principal, @PathVariable long ruleId) { service.delete(principal.userId(), ruleId); return ApiResponse.success(null, RequestIdFilter.currentRequestId()); }
}
