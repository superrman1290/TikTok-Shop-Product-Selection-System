"use client";

import { ArrowRightOutlined, LockOutlined, MailOutlined, UserOutlined } from "@ant-design/icons";
import { useMutation } from "@tanstack/react-query";
import { Button, Input } from "antd";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";
import { AuthShell } from "@/components/auth-shell";
import { authApi, AuthApiError } from "@/lib/auth";
import { registerSchema } from "@/lib/auth-validation";
import { useAuthStore } from "@/store/auth-store";
import styles from "@/components/auth-form.module.css";

type RegisterValues = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const router = useRouter();
  const setSession = useAuthStore((state) => state.setSession);
  const { control, handleSubmit, setError, formState: { errors } } = useForm<RegisterValues>({
    defaultValues: { email: "", username: "", password: "" },
  });
  const registration = useMutation({
    mutationFn: authApi.register,
    onSuccess: (session) => {
      setSession(session);
      router.replace("/dashboard");
    },
    onError: (error) => {
      setError("root", { message: error instanceof AuthApiError ? error.message : "注册失败，请稍后重试" });
    },
  });

  const onSubmit = (values: RegisterValues) => {
    const parsed = registerSchema.safeParse(values);
    if (parsed.success) {
      registration.mutate(parsed.data);
    }
  };

  return (
    <AuthShell
      title="创建账号"
      subtitle="建立你的选品工作区身份"
      footer={<>已有账号？ <Link href="/login">返回登录</Link></>}
    >
      <form className={styles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className={styles.field}>
          <label htmlFor="email">邮箱</label>
          <Controller name="email" control={control}
            rules={{ validate: (value) => registerSchema.shape.email.safeParse(value).error?.issues[0]?.message ?? true }}
            render={({ field }) => <Input {...field} id="email" size="large" prefix={<MailOutlined />} autoComplete="email" />} />
          <span className={styles.error}>{errors.email?.message}</span>
        </div>
        <div className={styles.field}>
          <label htmlFor="username">用户名</label>
          <Controller name="username" control={control}
            rules={{ validate: (value) => registerSchema.shape.username.safeParse(value).error?.issues[0]?.message ?? true }}
            render={({ field }) => <Input {...field} id="username" size="large" prefix={<UserOutlined />} autoComplete="username" />} />
          <span className={styles.error}>{errors.username?.message}</span>
        </div>
        <div className={styles.field}>
          <label htmlFor="password">密码</label>
          <Controller name="password" control={control}
            rules={{ validate: (value) => registerSchema.shape.password.safeParse(value).error?.issues[0]?.message ?? true }}
            render={({ field }) => <Input.Password {...field} id="password" size="large" prefix={<LockOutlined />} autoComplete="new-password" />} />
          <span className={styles.error}>{errors.password?.message}</span>
        </div>
        {errors.root?.message && <p className={styles.serverError} role="alert">{errors.root.message}</p>}
        <Button className={styles.submit} type="primary" htmlType="submit" loading={registration.isPending} icon={<ArrowRightOutlined />} iconPosition="end">
          创建账号
        </Button>
      </form>
    </AuthShell>
  );
}
