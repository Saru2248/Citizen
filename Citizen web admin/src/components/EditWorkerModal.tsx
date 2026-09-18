import React, { useState, useEffect } from 'react';
import { X, Edit3, Shield, Building, Phone, Mail, Lock, Eye, EyeOff, AlertCircle, CheckCircle2 } from 'lucide-react';
import { Department, UserProfile } from '../types';
import { adminApi } from '../services/apiService';

interface EditWorkerModalProps {
  isOpen: boolean;
  onClose: () => void;
  worker: UserProfile | null;
  departments: Department[];
  onSuccess?: () => void;
}

export const EditWorkerModal: React.FC<EditWorkerModalProps> = ({
  isOpen,
  onClose,
  worker,
  departments,
  onSuccess,
}) => {
  if (!isOpen || !worker) return null;

  const [name, setName] = useState(worker.name || '');
  const [workerId, setWorkerId] = useState(worker.workerId || '');
  const [mobileNumber, setMobileNumber] = useState(worker.mobileNumber || worker.phone || '');
  const [department, setDepartment] = useState(worker.department || departments[0]?.name || 'Public Works');
  const [accountStatus, setAccountStatus] = useState<'ACTIVE' | 'INACTIVE' | 'SUSPENDED'>(
    (worker.accountStatus as any) || (worker.status as any) || 'ACTIVE'
  );
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (worker) {
      setName(worker.name || '');
      setWorkerId(worker.workerId || '');
      setMobileNumber(worker.mobileNumber || worker.phone || '');
      setDepartment(worker.department || departments[0]?.name || 'Public Works');
      setAccountStatus(((worker.accountStatus as any) || (worker.status as any) || 'ACTIVE'));
      setPassword('');
      setConfirmPassword('');
      setError(null);
    }
  }, [worker]);

  const validate = (): string | null => {
    if (!name.trim()) return 'Worker full name is required.';
    if (!mobileNumber.trim()) return 'Mobile number is required.';
    if (workerId.trim() && workerId.trim().length < 3) {
      return 'Worker ID must be at least 3 characters.';
    }
    if (password) {
      if (password.length < 8) return 'New password must be at least 8 characters long.';
      if (!/[a-zA-Z]/.test(password) || !/\d/.test(password)) {
        return 'New password must contain at least one letter and one number.';
      }
      if (password !== confirmPassword) {
        return 'New password and confirmation do not match.';
      }
    }
    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    setError(null);
    setIsSubmitting(true);
    try {
      const payload: any = {
        name: name.trim(),
        workerId: workerId.trim() || undefined,
        mobileNumber: mobileNumber.trim(),
        phone: mobileNumber.trim(),
        department,
        accountStatus,
        isActive: accountStatus === 'ACTIVE',
      };
      if (password) {
        payload.password = password;
      }

      await adminApi.updateWorker(worker.id, payload);
      if (onSuccess) onSuccess();
      onClose();
    } catch (err: any) {
      console.error('Failed to update worker:', err);
      setError(err.response?.data?.message || err.message || 'Failed to update worker profile.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in overflow-y-auto">
      <div className="bg-white w-full max-w-lg rounded-2xl shadow-2xl overflow-hidden border border-slate-200 my-8">
        <div className="px-6 py-4 bg-slate-900 text-white flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Edit3 size={18} className="text-indigo-400" />
            <h3 className="font-bold text-sm">Edit Worker Profile & Credentials</h3>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4 max-h-[80vh] overflow-y-auto">
          {error && (
            <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-xl flex items-center gap-2">
              <AlertCircle size={16} className="shrink-0 text-rose-500" />
              <span>{error}</span>
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-semibold text-slate-700 block mb-1">Full Name *</label>
              <input
                type="text"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>

            <div>
              <label className="text-xs font-semibold text-slate-700 block mb-1">Worker ID</label>
              <input
                type="text"
                value={workerId}
                onChange={(e) => setWorkerId(e.target.value)}
                className="w-full px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-semibold text-slate-700 block mb-1">Mobile Number *</label>
              <input
                type="tel"
                required
                value={mobileNumber}
                onChange={(e) => setMobileNumber(e.target.value)}
                className="w-full px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>

            <div>
              <label className="text-xs font-semibold text-slate-700 block mb-1">Account Status</label>
              <select
                value={accountStatus}
                onChange={(e) => setAccountStatus(e.target.value as any)}
                className="w-full px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 font-medium"
              >
                <option value="ACTIVE">ACTIVE (Authorized to login)</option>
                <option value="INACTIVE">INACTIVE (Login blocked)</option>
                <option value="SUSPENDED">SUSPENDED</option>
              </select>
            </div>
          </div>

          <div>
            <label className="text-xs font-semibold text-slate-700 block mb-1">Assigned Department</label>
            <select
              value={department}
              onChange={(e) => setDepartment(e.target.value)}
              className="w-full px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 font-medium"
            >
              {departments.map((d) => (
                <option key={d.id} value={d.name}>
                  {d.name} ({d.code})
                </option>
              ))}
            </select>
          </div>

          <div className="border-t border-slate-100 pt-3">
            <h4 className="text-xs font-bold text-slate-800 mb-1 flex items-center gap-1.5">
              <Lock size={14} className="text-indigo-600" /> Change / Reset Password
            </h4>
            <p className="text-[11px] text-slate-400 mb-2">
              Leave blank to keep current password unchanged.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="text-xs font-semibold text-slate-700 block mb-1">New Password (Optional)</label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    placeholder="Min 8 chars, letter + digit"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full px-3 py-2 pr-9 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                  >
                    {showPassword ? <EyeOff size={14} /> : <Eye size={14} />}
                  </button>
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-slate-700 block mb-1">Confirm New Password</label>
                <input
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Repeat new password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="w-full px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            </div>
          </div>

          <div className="pt-3 flex gap-3">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2.5 text-xs font-semibold bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex-1 py-2.5 text-xs font-semibold bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl shadow-md transition disabled:opacity-50"
            >
              {isSubmitting ? 'Saving Changes...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

