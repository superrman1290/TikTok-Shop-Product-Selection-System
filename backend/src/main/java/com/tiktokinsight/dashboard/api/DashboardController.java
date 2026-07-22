package com.tiktokinsight.dashboard.api;

import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.common.api.ApiResponse;
import com.tiktokinsight.common.logging.RequestIdFilter;
import com.tiktokinsight.dashboard.application.DashboardService;
import com.tiktokinsight.dashboard.domain.UserDashboard;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }
    @GetMapping public ApiResponse<UserDashboard> get(@AuthenticationPrincipal AccessPrincipal principal, @RequestParam(required = false) String market) { return ApiResponse.success(service.get(principal.userId(), market), RequestIdFilter.currentRequestId()); }
}
