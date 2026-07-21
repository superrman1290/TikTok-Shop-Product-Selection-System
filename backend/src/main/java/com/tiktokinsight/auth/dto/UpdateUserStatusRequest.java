package com.tiktokinsight.auth.dto;

import com.tiktokinsight.auth.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull(message = "账号状态不能为空") UserStatus status) {
}
