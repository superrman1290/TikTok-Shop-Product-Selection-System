"use client";

import { useEffect, useRef } from "react";
import { authApi } from "@/lib/auth";
import { useAuthStore } from "@/store/auth-store";

export function AuthBootstrap() {
  const started = useRef(false);
  const setSession = useAuthStore((state) => state.setSession);
  const clearSession = useAuthStore((state) => state.clearSession);

  useEffect(() => {
    if (started.current) {
      return;
    }
    started.current = true;
    authApi.refresh().then(setSession).catch(() => {
      if (!useAuthStore.getState().accessToken) {
        clearSession();
      }
    });
  }, [clearSession, setSession]);

  return null;
}
