package com.tiktokinsight.watchlist.api;

import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.common.api.ApiResponse;
import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.common.logging.RequestIdFilter;
import com.tiktokinsight.watchlist.application.WatchlistService;
import com.tiktokinsight.watchlist.domain.WatchlistItem;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/watchlist")
public class WatchlistController {
    private final WatchlistService service;
    public WatchlistController(WatchlistService service) { this.service = service; }
    @GetMapping public ApiResponse<PageResponse<WatchlistItem>> list(@AuthenticationPrincipal AccessPrincipal principal, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize, @RequestParam(required = false) String market) {
        return ApiResponse.success(service.list(principal.userId(), market, page, pageSize), RequestIdFilter.currentRequestId());
    }
    @PostMapping("/{productId}") public ApiResponse<Void> add(@AuthenticationPrincipal AccessPrincipal principal, @PathVariable long productId) {
        service.add(principal.userId(), productId); return ApiResponse.success(null, RequestIdFilter.currentRequestId());
    }
    @DeleteMapping("/{productId}") public ApiResponse<Void> remove(@AuthenticationPrincipal AccessPrincipal principal, @PathVariable long productId) {
        service.remove(principal.userId(), productId); return ApiResponse.success(null, RequestIdFilter.currentRequestId());
    }
}
