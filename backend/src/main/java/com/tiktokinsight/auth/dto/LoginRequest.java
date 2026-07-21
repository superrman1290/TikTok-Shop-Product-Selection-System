package com.tiktokinsight.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式无效") String email,
        @NotBlank(message = "密码不能为空") String password
) {
}
