package com.tiktokinsight.datasource.administration;

import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.common.api.ApiResponse;
import com.tiktokinsight.common.logging.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/data-sources")
public class DataSourceAdministrationController {
    private final DataSourceAdministrationService service; public DataSourceAdministrationController(DataSourceAdministrationService service){this.service=service;}
    @GetMapping public ApiResponse<List<DataSourceRecord>> list(){return ApiResponse.success(service.list(),RequestIdFilter.currentRequestId());}
    @PostMapping public ApiResponse<DataSourceRecord> create(@Valid @RequestBody DataSourceRequest r,@AuthenticationPrincipal AccessPrincipal p,HttpServletRequest h){return ApiResponse.success(service.create(r,p,h),RequestIdFilter.currentRequestId());}
    @PutMapping("/{id}") public ApiResponse<DataSourceRecord> update(@PathVariable long id,@Valid @RequestBody DataSourceRequest r,@AuthenticationPrincipal AccessPrincipal p,HttpServletRequest h){return ApiResponse.success(service.update(id,r,p,h),RequestIdFilter.currentRequestId());}
    @PutMapping("/{id}/status") public ApiResponse<DataSourceRecord> status(@PathVariable long id,@RequestParam boolean enabled,@AuthenticationPrincipal AccessPrincipal p,HttpServletRequest h){return ApiResponse.success(service.updateStatus(id,enabled,p,h),RequestIdFilter.currentRequestId());}
}
