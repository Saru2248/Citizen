import React, { useState } from 'react';
import { Search, Bell, RefreshCw, CheckCircle2, Shield, AlertTriangle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { NotificationItem } from '../types';

interface NavbarProps {
  collapsed: boolean;
  activeTabTitle: string;
  notifications: NotificationItem[];
  onSelectNotification?: (complaintId?: string) => void;
  onRefresh?: () => void;
  searchQuery: string;
  setSearchQuery: (query: string) => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  collapsed,
  activeTabTitle,
  notifications,
  onSelectNotification,
  onRefresh,
  searchQuery,
  setSearchQuery
}) => {
  const { adminProfile, isSuperAdmin } = useAuth();
  const [showNotifMenu, setShowNotifMenu] = useState(false);

  const unreadNotifs = notifications.filter((n) => !n.read);

  return (
    <header
      className={`fixed top-0 right-0 z-30 h-16 bg-white border-b border-slate-200 transition-all duration-300 flex items-center justify-between px-6 shadow-sm ${
        collapsed ? 'left-20' : 'left-64'
      }`}
    >
      {/* Active Page Title & Search */}
      <div className="flex items-center gap-6 flex-1">
        <h1 className="text-xl font-bold text-slate-900 tracking-tight capitalize">
          {activeTabTitle}
        </h1>

        {/* Global Search Bar */}
        <div className="relative max-w-md w-full hidden md:block">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
          <input
            type="text"
            placeholder="Search complaint ID, category, worker, citizen..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 text-sm bg-slate-100/80 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:bg-white transition"
          />
        </div>
      </div>

      {/* Right Header Status & Controls */}
      <div className="flex items-center gap-4">
        {/* Firestore Live Indicator */}
        <div className="hidden lg:flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-50 border border-emerald-200 text-xs font-semibold text-emerald-700">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
          <span>Live Sync Active</span>
        </div>

        {/* Admin Role Tag */}
        <div className="hidden sm:flex items-center gap-1.5 px-3 py-1 bg-slate-100 border border-slate-200 rounded-lg text-xs font-semibold text-slate-700">
          <Shield size={14} className="text-emerald-600" />
          <span>{isSuperAdmin ? 'Super Admin' : `${adminProfile?.department || 'Dept Admin'}`}</span>
        </div>

        {/* Refresh Button */}
        {onRefresh && (
          <button
            onClick={onRefresh}
            className="p-2 text-slate-500 hover:text-emerald-600 hover:bg-slate-100 rounded-xl transition"
            title="Refresh Live Data"
          >
            <RefreshCw size={18} />
          </button>
        )}

        {/* Notification Bell Dropdown */}
        <div className="relative">
          <button
            onClick={() => setShowNotifMenu(!showNotifMenu)}
            className="relative p-2 text-slate-600 hover:text-emerald-600 hover:bg-slate-100 rounded-xl transition"
            title="Notifications"
          >
            <Bell size={20} />
            {unreadNotifs.length > 0 && (
              <span className="absolute top-1 right-1 w-5 h-5 bg-emerald-600 text-white font-bold text-[10px] flex items-center justify-center rounded-full border-2 border-white">
                {unreadNotifs.length}
              </span>
            )}
          </button>

          {/* Notifications Popover */}
          {showNotifMenu && (
            <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white border border-slate-200 rounded-2xl shadow-xl py-2 z-50 animate-in fade-in slide-in-from-top-2">
              <div className="px-4 py-2 border-b border-slate-100 flex items-center justify-between">
                <span className="font-bold text-sm text-slate-900">Notifications</span>
                <span className="text-xs font-semibold text-emerald-600">{unreadNotifs.length} unread</span>
              </div>
              <div className="max-h-80 overflow-y-auto divide-y divide-slate-100">
                {notifications.length === 0 ? (
                  <p className="p-4 text-center text-xs text-slate-400">No notifications yet</p>
                ) : (
                  notifications.slice(0, 8).map((n) => (
                    <div
                      key={n.id}
                      onClick={() => {
                        if (n.complaintId && onSelectNotification) {
                          onSelectNotification(n.complaintId);
                        }
                        setShowNotifMenu(false);
                      }}
                      className={`p-3 text-xs hover:bg-slate-50 cursor-pointer transition flex items-start gap-3 ${
                        !n.read ? 'bg-emerald-50/50 font-medium' : ''
                      }`}
                    >
                      {n.type === 'HIGH_PRIORITY' ? (
                        <AlertTriangle className="text-red-500 shrink-0 mt-0.5" size={16} />
                      ) : (
                        <CheckCircle2 className="text-emerald-600 shrink-0 mt-0.5" size={16} />
                      )}
                      <div className="flex-1">
                        <p className="font-semibold text-slate-900">{n.title}</p>
                        <p className="text-slate-600 mt-0.5">{n.message}</p>
                        <p className="text-[10px] text-slate-400 mt-1">
                          {new Date(n.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </p>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
