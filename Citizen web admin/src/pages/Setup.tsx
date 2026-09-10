import React, { useState } from 'react';
import { createUserWithEmailAndPassword } from 'firebase/auth';
import { doc, setDoc } from 'firebase/firestore';
import { auth, db } from '../config/firebase';
import { CheckCircle, AlertCircle, Loader } from 'lucide-react';

const ADMINS = [
  {
    email: 'admin@citizen.gov',
    password: 'admin123',
    name: 'Super Administrator',
    role: 'ADMIN',
    adminLevel: 'SUPER_ADMIN',
    label: 'Super Admin',
  },
  {
    email: 'pwd.admin@citizen.gov',
    password: 'admin123',
    name: 'PWD Department Admin',
    role: 'ADMIN',
    adminLevel: 'DEPT_ADMIN',
    department: 'Public Works',
    label: 'Department Admin',
  },
];

type Status = 'idle' | 'loading' | 'success' | 'error';

export const Setup: React.FC = () => {
  const [results, setResults] = useState<{ label: string; status: Status; message: string }[]>([]);
  const [running, setRunning] = useState(false);
  const [done, setDone] = useState(false);

  const runSetup = async () => {
    setRunning(true);
    const res: { label: string; status: Status; message: string }[] = [];

    for (const admin of ADMINS) {
      try {
        const cred = await createUserWithEmailAndPassword(auth, admin.email, admin.password);
        const uid = cred.user.uid;
        await setDoc(doc(db, 'users', uid), {
          id: uid,
          name: admin.name,
          email: admin.email,
          role: admin.role,
          adminLevel: admin.adminLevel,
          department: admin.department || null,
          createdAt: Date.now(),
        });
        res.push({ label: admin.label, status: 'success', message: `✅ Created: ${admin.email}` });
      } catch (err: any) {
        if (err.code === 'auth/email-already-in-use') {
          res.push({ label: admin.label, status: 'success', message: `ℹ️ Already exists: ${admin.email}` });
        } else {
          res.push({ label: admin.label, status: 'error', message: `❌ Failed: ${err.message}` });
        }
      }
    }

    setResults(res);
    setRunning(false);
    setDone(true);
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-slate-800 border border-slate-700 rounded-3xl p-8 shadow-2xl space-y-6">
        <div className="text-center space-y-2">
          <div className="w-14 h-14 rounded-2xl bg-emerald-500 flex items-center justify-center text-white font-black text-xl mx-auto">C</div>
          <h1 className="text-xl font-bold text-white">CITIZEN <span className="text-emerald-400">AI</span></h1>
          <p className="text-xs text-slate-400 uppercase tracking-widest">One-Time Admin Setup</p>
        </div>

        <div className="bg-slate-900/60 rounded-2xl p-4 space-y-2 text-sm">
          <p className="text-slate-300 font-semibold mb-3">Accounts to create:</p>
          {ADMINS.map(a => (
            <div key={a.email} className="flex justify-between text-xs text-slate-400">
              <span className="font-medium text-slate-300">{a.label}</span>
              <span>{a.email} / {a.password}</span>
            </div>
          ))}
        </div>

        {results.length > 0 && (
          <div className="space-y-2">
            {results.map((r, i) => (
              <div key={i} className={`flex items-center gap-2 p-3 rounded-xl text-sm ${r.status === 'success' ? 'bg-emerald-500/10 border border-emerald-500/30 text-emerald-300' : 'bg-red-500/10 border border-red-500/30 text-red-300'}`}>
                {r.status === 'success' ? <CheckCircle size={16} /> : <AlertCircle size={16} />}
                <span>{r.message}</span>
              </div>
            ))}
          </div>
        )}

        {done ? (
          <div className="text-center space-y-3">
            <p className="text-emerald-400 font-semibold text-sm">Setup complete!</p>
            <a
              href="/"
              className="block w-full py-3 bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-sm rounded-xl text-center transition"
            >
              Go to Login →
            </a>
          </div>
        ) : (
          <button
            onClick={runSetup}
            disabled={running}
            className="w-full py-3 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white font-bold text-sm rounded-xl flex items-center justify-center gap-2 transition"
          >
            {running ? <><Loader size={16} className="animate-spin" /> Creating Accounts...</> : 'Create Admin Accounts'}
          </button>
        )}
      </div>
    </div>
  );
};
