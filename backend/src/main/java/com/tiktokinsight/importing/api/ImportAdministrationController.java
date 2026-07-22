package com.tiktokinsight.importing.api;

import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.common.api.ApiResponse;
import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.common.logging.RequestIdFilter;
import com.tiktokinsight.datasource.ImportRowError;
import com.tiktokinsight.importing.application.ImportApplicationService;
import com.tiktokinsight.importing.domain.ImportJob;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/imports")
public class ImportAdministrationController {

    private final ImportApplicationService service;

    public ImportAdministrationController(ImportApplicationService service) {
        this.service = service;
    }

    @PostMapping(value = "/products", consumes = "multipart/form-data")
    public ApiResponse<ImportJob> importProducts(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AccessPrincipal principal
    ) {
        return ApiResponse.success(
                service.importProducts(file, principal.userId()), RequestIdFilter.currentRequestId()
        );
    }

    @PostMapping(value = "/product-stats", consumes = "multipart/form-data")
    public ApiResponse<ImportJob> importProductStats(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AccessPrincipal principal
    ) {
        return ApiResponse.success(
                service.importProductStats(file, principal.userId()), RequestIdFilter.currentRequestId()
        );
    }

    @GetMapping
    public ApiResponse<PageResponse<ImportJob>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(service.list(page, pageSize), RequestIdFilter.currentRequestId());
    }

    @GetMapping("/{importId}")
    public ApiResponse<ImportJob> get(@PathVariable long importId) {
        return ApiResponse.success(service.get(importId), RequestIdFilter.currentRequestId());
    }

    @GetMapping("/{importId}/errors")
    public ApiResponse<PageResponse<ImportRowError>> errors(
            @PathVariable long importId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(service.errors(importId, page, pageSize), RequestIdFilter.currentRequestId());
    }
}
