import React from 'react';
import { Bell, CheckCircle2, AlertTriangle, Clock } from 'lucide-react';
import { NotificationItem } from '../types';
import { notificationApi } from '../services/apiService';

interface NotificationsViewProps {
  notifications: NotificationItem[];
  onOpenComplaintById?: (id: string) => void;
}

export const NotificationsView: React.FC<NotificationsViewProps> = ({
  notifications,
  onOpenComplaintById
}) => {
  return (
    <div className="space-y-6 animate-in fade-in">
      {/* Header */}
      <div className="p-6 bg-white border border-slate-200 rounded-2xl shadow-sm">
        <h2 className="text-lg font-bold text-slate-900">Admin Notification Stream</h2>
        <p className="text-xs text-slate-500">Real-time alerts for new complaints, worker assignments & status changes</p>
      </div>

      {/* Notifications List */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm divide-y divide-slate-100 overflow-hidden">
        {notifications.length === 0 ? (
          <div className="p-12 text-center text-slate-400 text-xs">No active notifications</div>
        ) : (
          notifications.map((n) => (
            <div
              key={n.id}
              onClick={() => {
                notificationApi.markRead(n.id).catch(console.error);
                if (n.complaintId && onOpenComplaintById) {
                  onOpenComplaintById(n.complaintId);
                }
              }}
              className={`p-4 text-xs flex items-start gap-4 hover:bg-slate-50 cursor-pointer transition ${
                !n.read ? 'bg-emerald-50/40 font-medium' : ''
              }`}
            >
              {n.type === 'HIGH_PRIORITY' ? (
                <div className="p-2 bg-red-100 text-red-600 rounded-xl shrink-0">
                  <AlertTriangle size={18} />
                </div>
              ) : (
                <div className="p-2 bg-emerald-100 text-emerald-600 rounded-xl shrink-0">
                  <CheckCircle2 size={18} />
                </div>
              )}

              <div className="flex-1 space-y-1">
                <div className="flex items-center justify-between">
                  <h4 className="font-bold text-slate-900 text-sm">{n.title}</h4>
                  <span className="text-[10px] text-slate-400">
                    {new Date(n.timestamp).toLocaleString()}
                  </span>
                </div>
                <p className="text-slate-600 leading-relaxed">{n.message}</p>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
