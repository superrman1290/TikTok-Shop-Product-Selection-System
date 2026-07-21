package com.tiktokinsight.common.api.health;

import com.tiktokinsight.common.api.ApiResponse;
import com.tiktokinsight.common.logging.RequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health")
public class HealthController {

    private final SystemHealthService systemHealthService;

    public HealthController(SystemHealthService systemHealthService) {
        this.systemHealthService = systemHealthService;
    }

    @GetMapping
    @Operation(summary = "Check application dependencies")
    public ResponseEntity<ApiResponse<SystemHealth>> health() {
        SystemHealth health = systemHealthService.check();
        HttpStatus status = health.allComponentsUp() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(ApiResponse.success(
                health,
                RequestIdFilter.currentRequestId()
        ));
    }
}
