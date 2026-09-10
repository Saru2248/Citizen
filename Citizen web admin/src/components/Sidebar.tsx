import React from 'react';
import {
  LayoutDashboard,
  FileText,
  MapPin,
  Building2,
  Users,
  UserCheck,
  BarChart3,
  FileSpreadsheet,
  Bell,
  Settings,
  LogOut,
  ChevronLeft,
  ChevronRight,
  ShieldAlert,
  Building
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export type TabType =
  | 'dashboard'
  | 'complaints'
  | 'map'
  | 'departments'
  | 'workers'
  | 'users'
  | 'analytics'
  | 'reports'
  | 'notifications'
  | 'settings';

interface SidebarProps {
  activeTab: TabType;
  setActiveTab: (tab: TabType) => void;
  collapsed: boolean;
  setCollapsed: (collapsed: boolean) => void;
  unreadNotifsCount: number;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  setActiveTab,
  collapsed,
  setCollapsed,
  unreadNotifsCount
}) => {
  const { adminProfile, isSuperAdmin, logoutAdmin } = useAuth();

  const navItems = [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { id: 'complaints', label: 'Complaints', icon: FileText },
    { id: 'map', label: 'Live Map', icon: MapPin },
    { id: 'departments', label: 'Departments', icon: Building2 },
    { id: 'workers', label: 'Workers', icon: Users },
    { id: 'users', label: 'Users', icon: UserCheck },
    { id: 'analytics', label: 'Analytics', icon: BarChart3 },
    { id: 'reports', label: 'Reports', icon: FileSpreadsheet },
    { id: 'notifications', label: 'Notifications', icon: Bell, badge: unreadNotifsCount },
    { id: 'settings', label: 'Settings', icon: Settings },
  ];

  return (
    <aside
      className={`fixed top-0 left-0 bottom-0 z-40 bg-slate-900 text-slate-300 transition-all duration-300 flex flex-col border-r border-slate-800 ${
        collapsed ? 'w-20' : 'w-64'
      }`}
    >
      {/* Brand Header */}
      <div className="h-16 flex items-center justify-between px-4 border-b border-slate-800/80 bg-slate-950/40">
        <div className="flex items-center gap-3 overflow-hidden">
          <img
            src="/logo.png"
            alt="Citizen AI Logo"
            className="w-10 h-10 rounded-xl object-contain bg-white p-0.5 shadow-md shrink-0 border border-slate-700/50"
          />
          {!collapsed && (
            <div className="flex flex-col">
              <span className="font-bold text-white tracking-wide text-base leading-tight">
                CITIZEN <span className="text-emerald-400 font-extrabold">AI</span>
              </span>
              <span className="text-[10px] text-slate-400 uppercase tracking-widest font-semibold">
                Municipal Admin
              </span>
            </div>
          )}
        </div>
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
          title={collapsed ? 'Expand Sidebar' : 'Collapse Sidebar'}
        >
          {collapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
        </button>
      </div>

      {/* Authority Level Tag */}
      {!collapsed && (
        <div className="mx-3 my-3 px-3 py-2 rounded-lg bg-slate-800/60 border border-slate-700/50 flex items-center gap-2">
          <ShieldAlert size={16} className={isSuperAdmin ? "text-emerald-400" : "text-amber-400"} />
          <div className="truncate">
            <p className="text-xs font-semibold text-white truncate">
              {isSuperAdmin ? 'SUPER ADMIN' : 'DEPARTMENT ADMIN'}
            </p>
            <p className="text-[10px] text-slate-400 truncate">
              {adminProfile?.department || 'Full System Scope'}
            </p>
          </div>
        </div>
      )}

      {/* Navigation List */}
      <nav className="flex-1 px-3 py-2 space-y-1 overflow-y-auto">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id as TabType)}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all group ${
                isActive
                  ? 'bg-emerald-600 text-white shadow-md shadow-emerald-600/25 font-semibold'
                  : 'text-slate-300 hover:bg-slate-800 hover:text-white'
              }`}

            >
              <Icon
                size={20}
                className={`shrink-0 transition-transform group-hover:scale-110 ${
                  isActive ? 'text-white' : 'text-slate-400 group-hover:text-emerald-400'
                }`}
              />
              {!collapsed && <span className="truncate">{item.label}</span>}
              {!collapsed && item.badge !== undefined && item.badge > 0 && (
                <span className="ml-auto px-2 py-0.5 text-xs font-bold bg-emerald-500 text-white rounded-full">
                  {item.badge}
                </span>
              )}
            </button>
          );
        })}
      </nav>

      {/* User Profile & Logout */}
      <div className="p-3 border-t border-slate-800 bg-slate-950/30">
        <div className={`flex items-center ${collapsed ? 'justify-center' : 'justify-between'} gap-2`}>
          {!collapsed && (
            <div className="flex items-center gap-2.5 overflow-hidden">
              <div className="w-8 h-8 rounded-full bg-slate-700 text-emerald-400 flex items-center justify-center font-bold text-xs shrink-0 border border-slate-600">
                {adminProfile?.name?.charAt(0) || 'A'}
              </div>
              <div className="truncate">
                <p className="text-xs font-semibold text-white truncate">{adminProfile?.name || 'Administrator'}</p>
                <p className="text-[10px] text-slate-400 truncate">{adminProfile?.email || 'admin@citizen.gov'}</p>
              </div>
            </div>
          )}
          <button
            onClick={logoutAdmin}
            className="p-2 rounded-lg text-slate-400 hover:text-red-400 hover:bg-slate-800 transition shrink-0"
            title="Log Out"
          >
            <LogOut size={18} />
          </button>
        </div>
      </div>
    </aside>
  );
};
