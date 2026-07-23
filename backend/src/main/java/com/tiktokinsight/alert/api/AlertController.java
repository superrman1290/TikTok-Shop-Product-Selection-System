package com.tiktokinsight.alert.api;

import com.tiktokinsight.alert.application.AlertService;
import com.tiktokinsight.alert.domain.AlertEvent;
import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.common.api.ApiResponse;
import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.common.logging.RequestIdFilter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {
    private final AlertService service;
    public AlertController(AlertService service) { this.service = service; }
    @GetMapping public ApiResponse<PageResponse<AlertEvent>> list(@AuthenticationPrincipal AccessPrincipal principal, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize, @RequestParam(required = false) String market, @RequestParam(required = false) Boolean readStatus) { return ApiResponse.success(service.list(principal.userId(), market, readStatus, page, pageSize), RequestIdFilter.currentRequestId()); }
    @PostMapping("/{alertId}/read") public ApiResponse<Void> read(@AuthenticationPrincipal AccessPrincipal principal, @PathVariable long alertId) { service.markRead(principal.userId(), alertId); return ApiResponse.success(null, RequestIdFilter.currentRequestId()); }
    @PostMapping("/read-all") public ApiResponse<Integer> readAll(@AuthenticationPrincipal AccessPrincipal principal) { return ApiResponse.success(service.markAllRead(principal.userId()), RequestIdFilter.currentRequestId()); }
}
