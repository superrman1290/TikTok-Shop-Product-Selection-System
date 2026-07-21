import { z } from "zod";

export const loginSchema = z.object({
  email: z.string().trim().min(1, "请输入邮箱").email("请输入有效邮箱"),
  password: z.string().min(1, "请输入密码"),
});

export const registerSchema = z.object({
  email: z.string().trim().min(1, "请输入邮箱").email("请输入有效邮箱"),
  username: z.string().trim().min(2, "用户名至少2位").max(40, "用户名不能超过40位"),
  password: z.string()
    .min(8, "密码至少8位")
    .max(64, "密码不能超过64位")
    .regex(/[A-Za-z]/, "密码必须包含字母")
    .regex(/\d/, "密码必须包含数字"),
});

export const passwordSchema = z.object({
  currentPassword: z.string().min(1, "请输入当前密码"),
  newPassword: z.string().min(8, "密码至少8位").max(64, "密码不能超过64位")
    .regex(/[A-Za-z]/, "密码必须包含字母").regex(/\d/, "密码必须包含数字"),
});
