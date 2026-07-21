"use client";

import { ArrowRightOutlined, LockOutlined, MailOutlined } from "@ant-design/icons";
import { useMutation } from "@tanstack/react-query";
import { Button, Input } from "antd";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";
import { AuthShell } from "@/components/auth-shell";
import { authApi, AuthApiError } from "@/lib/auth";
import { loginSchema } from "@/lib/auth-validation";
import { useAuthStore } from "@/store/auth-store";
import styles from "@/components/auth-form.module.css";

type LoginValues = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const router = useRouter();
  const setSession = useAuthStore((state) => state.setSession);
  const { control, handleSubmit, setError, formState: { errors } } = useForm<LoginValues>({
    defaultValues: { email: "", password: "" },
  });
  const login = useMutation({
    mutationFn: authApi.login,
    onSuccess: (session) => {
      setSession(session);
      router.replace(session.user.role === "ADMIN" ? "/admin" : "/dashboard");
    },
    onError: (error) => {
      setError("root", { message: error instanceof AuthApiError ? error.message : "登录失败，请稍后重试" });
    },
  });

  const onSubmit = (values: LoginValues) => {
    const parsed = loginSchema.safeParse(values);
    if (parsed.success) {
      login.mutate(parsed.data);
    }
  };

  return (
    <AuthShell
      title="登录工作台"
      subtitle="使用你的账号继续"
      footer={<>还没有账号？ <Link href="/register">创建账号</Link></>}
    >
      <form className={styles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className={styles.field}>
          <label htmlFor="email">邮箱</label>
          <Controller
            name="email"
            control={control}
            rules={{ validate: (value) => loginSchema.shape.email.safeParse(value).error?.issues[0]?.message ?? true }}
            render={({ field }) => <Input {...field} id="email" size="large" prefix={<MailOutlined />} autoComplete="email" />}
          />
          <span className={styles.error}>{errors.email?.message}</span>
        </div>
        <div className={styles.field}>
          <label htmlFor="password">密码</label>
          <Controller
            name="password"
            control={control}
            rules={{ validate: (value) => loginSchema.shape.password.safeParse(value).error?.issues[0]?.message ?? true }}
            render={({ field }) => <Input.Password {...field} id="password" size="large" prefix={<LockOutlined />} autoComplete="current-password" />}
          />
          <span className={styles.error}>{errors.password?.message}</span>
        </div>
        {errors.root?.message && <p className={styles.serverError} role="alert">{errors.root.message}</p>}
        <Button className={styles.submit} type="primary" htmlType="submit" loading={login.isPending} icon={<ArrowRightOutlined />} iconPosition="end">
          登录
        </Button>
      </form>
    </AuthShell>
  );
}
