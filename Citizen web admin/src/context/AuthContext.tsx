import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi, getToken, setToken, clearToken } from '../services/apiService';
import { UserProfile, AdminLevel } from '../types';

interface AuthContextType {
  currentUser: UserProfile | null;
  adminProfile: UserProfile | null;
  adminLevel: AdminLevel | null;
  isSuperAdmin: boolean;
  department: string | null;
  loading: boolean;
  token: string | null;
  loginAdmin: (email: string, pass: string) => Promise<{ success: boolean; error?: string }>;
  logoutAdmin: () => void;
}

const AuthContext = createContext<AuthContextType>({} as AuthContextType);

export const useAuth = () => useContext(AuthContext);

const mapUser = (u: any): UserProfile => ({
  id: u.id,
  name: u.name,
  email: u.email,
  phone: u.phone,
  role: u.role,
  adminLevel: u.adminLevel || undefined,
  departmentId: u.departmentId || undefined,
  department: u.department || undefined,
  workerId: u.workerId || undefined,
  avatarUrl: u.avatarUrl || undefined,
  status: u.accountStatus || 'ACTIVE',
  tasksCompleted: u.tasksCompleted ?? 0,
  tasksInProgress: u.tasksInProgress ?? 0,
  avgCompletionTimeHours: u.avgCompletionTimeHours ?? 0,
  totalReports: u.totalReports ?? 0,
  resolvedReports: u.resolvedReports ?? 0,
  pendingReports: u.pendingReports ?? 0,
  createdAt: u.createdAt ? new Date(u.createdAt).getTime() : Date.now(),
});

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [adminProfile, setAdminProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [token, setTokenState] = useState<string | null>(getToken());

  // On mount — try to restore session from stored token
  useEffect(() => {
    const restore = async () => {
      const storedToken = getToken();
      if (!storedToken) {
        setLoading(false);
        return;
      }
      try {
        const res = await authApi.me();
        const profile = mapUser(res.user);
        if (profile.role !== 'ADMIN' && profile.role !== 'SUPER_ADMIN') {
          clearToken();
          setLoading(false);
          return;
        }
        setAdminProfile(profile);
        setTokenState(storedToken);
      } catch {
        clearToken();
        setTokenState(null);
      } finally {
        setLoading(false);
      }
    };
    restore();
  }, []);

  const loginAdmin = useCallback(async (email: string, pass: string): Promise<{ success: boolean; error?: string }> => {
    try {
      const res = await authApi.login(email, pass);
      const profile = mapUser(res.user);

      if (profile.role !== 'ADMIN' && profile.role !== 'SUPER_ADMIN') {
        return {
          success: false,
          error: 'Access Denied: Citizens and Workers cannot log into the Admin Web Portal.',
        };
      }

      setToken(res.token);
      setTokenState(res.token);
      setAdminProfile(profile);
      return { success: true };
    } catch (err: any) {
      const status = err?.status;
      let errorMsg = 'Invalid email or password.';
      if (status === 401) errorMsg = 'Invalid email or password.';
      if (status === 403) errorMsg = 'Account is suspended or inactive.';
      if (status === 429) errorMsg = 'Too many login attempts. Please wait 15 minutes.';
      if (err?.data?.message) errorMsg = err.data.message;
      return { success: false, error: errorMsg };
    }
  }, []);

  const logoutAdmin = useCallback(() => {
    clearToken();
    setTokenState(null);
    setAdminProfile(null);
  }, []);

  const adminLevel = (adminProfile?.adminLevel as AdminLevel) || 'SUPER_ADMIN';
  const isSuperAdmin = adminProfile?.role === 'ADMIN' || adminProfile?.role === 'SUPER_ADMIN' || adminLevel === 'SUPER_ADMIN';
  const department = adminProfile?.department || null;

  return (
    <AuthContext.Provider
      value={{
        currentUser: adminProfile,
        adminProfile,
        adminLevel,
        isSuperAdmin,
        department,
        loading,
        token,
        loginAdmin,
        logoutAdmin,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
