package com.tiktokinsight.auth.api;

import com.tiktokinsight.auth.application.AccountAdministrationService;
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

@RestController
@RequestMapping("/api/v1/admin/users")
public class AccountAdministrationController {

    private final AccountAdministrationService accountAdministrationService;

    public AccountAdministrationController(AccountAdministrationService accountAdministrationService) {
        this.accountAdministrationService = accountAdministrationService;
    }

    @PutMapping("/{userId}/status")
    public ApiResponse<UserResponse> updateStatus(
            @PathVariable long userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return ApiResponse.success(
                UserResponse.from(accountAdministrationService.updateStatus(userId, request.status())),
                RequestIdFilter.currentRequestId()
        );
    }
}
