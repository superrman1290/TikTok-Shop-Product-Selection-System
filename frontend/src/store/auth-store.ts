import { create } from "zustand";
import type { AuthSession, AuthUser } from "@/lib/auth";

interface AuthState {
  accessToken: string | null;
  user: AuthUser | null;
  initialized: boolean;
  setSession: (session: AuthSession) => void;
  setUser: (user: AuthUser) => void;
  clearSession: () => void;
  markInitialized: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  user: null,
  initialized: false,
  setSession: (session) => set({
    accessToken: session.accessToken,
    user: session.user,
    initialized: true,
  }),
  setUser: (user) => set({ user }),
  clearSession: () => set({ accessToken: null, user: null, initialized: true }),
  markInitialized: () => set({ initialized: true }),
}));
