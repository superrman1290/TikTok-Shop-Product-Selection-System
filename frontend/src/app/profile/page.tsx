"use client";

import { LockOutlined, SaveOutlined } from "@ant-design/icons";
import { useMutation } from "@tanstack/react-query";
import { Button, Input, message } from "antd";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";
import { AppHeader } from "@/components/app-header";
import formStyles from "@/components/auth-form.module.css";
import { ProtectedRoute } from "@/components/protected-route";
import { authApi, AuthApiError } from "@/lib/auth";
import { passwordSchema } from "@/lib/auth-validation";
import { useAuthStore } from "@/store/auth-store";
import styles from "../workspace.module.css";

type PasswordValues = z.infer<typeof passwordSchema>;

export default function ProfilePage() {
  const [messageApi, contextHolder] = message.useMessage();
  const accessToken = useAuthStore((state) => state.accessToken);
  const clearSession = useAuthStore((state) => state.clearSession);
  const { control, handleSubmit, reset, setError, formState: { errors } } = useForm<PasswordValues>({
    defaultValues: { currentPassword: "", newPassword: "" },
  });
  const changePassword = useMutation({
    mutationFn: (values: PasswordValues) => authApi.changePassword(accessToken ?? "", values),
    onSuccess: () => {
      reset();
      clearSession();
      messageApi.success("密码已更新，请重新登录");
      window.location.assign("/login");
    },
    onError: (error) => setError("root", {
      message: error instanceof AuthApiError ? error.message : "密码更新失败",
    }),
  });

  const onSubmit = (values: PasswordValues) => {
    const parsed = passwordSchema.safeParse(values);
    if (parsed.success) {
      changePassword.mutate(parsed.data);
    }
  };

  return (
    <ProtectedRoute>
      <div className={styles.page}>
        {contextHolder}
        <AppHeader />
        <main className={styles.main}>
          <section className={styles.heading}>
            <div>
              <p className={styles.eyebrow}>Account security</p>
              <h1>修改密码</h1>
              <p>更新后，所有 Refresh Token 将立即撤销。</p>
            </div>
          </section>
          <form className={`${styles.profileForm} ${formStyles.form}`} onSubmit={handleSubmit(onSubmit)} noValidate>
            <div className={formStyles.field}>
              <label htmlFor="currentPassword">当前密码</label>
              <Controller name="currentPassword" control={control}
                rules={{ required: "请输入当前密码" }}
                render={({ field }) => <Input.Password {...field} id="currentPassword" size="large" prefix={<LockOutlined />} autoComplete="current-password" />} />
              <span className={formStyles.error}>{errors.currentPassword?.message}</span>
            </div>
            <div className={formStyles.field}>
              <label htmlFor="newPassword">新密码</label>
              <Controller name="newPassword" control={control}
                rules={{ validate: (value) => passwordSchema.shape.newPassword.safeParse(value).error?.issues[0]?.message ?? true }}
                render={({ field }) => <Input.Password {...field} id="newPassword" size="large" prefix={<LockOutlined />} autoComplete="new-password" />} />
              <span className={formStyles.error}>{errors.newPassword?.message}</span>
            </div>
            {errors.root?.message && <p className={formStyles.serverError} role="alert">{errors.root.message}</p>}
            <Button className={formStyles.submit} type="primary" htmlType="submit" loading={changePassword.isPending} icon={<SaveOutlined />}>
              保存密码
            </Button>
          </form>
        </main>
      </div>
    </ProtectedRoute>
  );
}
