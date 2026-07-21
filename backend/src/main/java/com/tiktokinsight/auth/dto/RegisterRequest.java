package com.tiktokinsight.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式无效")
        @Size(max = 254, message = "邮箱长度不能超过254位")
        String email,

        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 40, message = "用户名长度必须为2至40位")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度必须为8至64位")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须至少包含一个字母和一个数字")
        String password
) {
}
