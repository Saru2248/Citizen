import React, { useState, useId } from 'react';
import { Eye, EyeOff, AlertCircle, Loader2, Mail, Lock, ShieldCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

// ── Friendly error messages for HTTP error codes ────────────────────────────
function friendlyError(err: any): string {
  if (!err) return 'An unexpected error occurred. Please try again.';

  // Network / connectivity
  if (err instanceof TypeError && err.message.includes('fetch')) {
    return 'Unable to connect to the server. Please check your connection and try again.';
  }

  const status: number | undefined = err?.status;

  switch (status) {
    case 400: return 'Invalid request. Please check your credentials and try again.';
    case 401: return 'Invalid email or password. Please try again.';
    case 403: return 'Your account is suspended or inactive. Contact your system administrator.';
    case 404: return 'Authentication service not found. Please contact support.';
    case 408: return 'Request timed out. The server took too long to respond. Please try again.';
    case 409: return 'A session conflict was detected. Please refresh the page and try again.';
    case 429: return 'Too many failed login attempts. Please wait a few minutes before trying again.';
    case 500: return 'An internal server error occurred. Please try again later.';
    case 502: return 'The server is temporarily unavailable. Please try again shortly.';
    case 503: return 'The server is under maintenance. Please try again later.';
    default: break;
  }

  // Server-provided message (safe to show)
  if (err?.data?.message) return err.data.message;
  if (err?.message && !err.message.includes('stack')) return err.message;

  return 'An unexpected error occurred. Please try again.';
}

// ── Input field component ────────────────────────────────────────────────────
interface InputFieldProps {
  id: string;
  label: string;
  type: string;
  value: string;
  onChange: (v: string) => void;
  placeholder: string;
  autoComplete: string;
  icon: React.ReactNode;
  suffix?: React.ReactNode;
  disabled?: boolean;
  required?: boolean;
}

const InputField: React.FC<InputFieldProps> = ({
  id, label, type, value, onChange, placeholder, autoComplete,
  icon, suffix, disabled, required,
}) => (
  <div>
    <label htmlFor={id} className="block font-semibold mb-2" style={{ color: '#374151', fontSize: '0.9375rem' }}>
      {label}
    </label>
    <div
      className="flex items-center gap-3 px-4 rounded-xl transition-all"
      style={{
        background: '#f8fafc',
        border: '1.5px solid #e2e8f0',
        minHeight: '56px',
      }}
    >
      <span className="shrink-0" style={{ color: '#94a3b8' }}>{icon}</span>
      <input
        id={id}
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        autoComplete={autoComplete}
        required={required}
        disabled={disabled}
        aria-label={label}
        className="flex-1 bg-transparent outline-none py-3 disabled:opacity-50"
        style={{ color: '#1e293b', caretColor: '#2ecc71', fontSize: '1rem' }}
      />
      {suffix}
    </div>
  </div>
);

// ── Main Login component ─────────────────────────────────────────────────────
export const Login: React.FC = () => {
  const { loginAdmin } = useAuth();
  const uid = useId();

  const [email, setEmail]         = useState('');
  const [password, setPassword]   = useState('');
  const [showPass, setShowPass]   = useState(false);
  const [error, setError]         = useState<string | null>(null);
  const [loading, setLoading]     = useState(false);



  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // Client-side blank checks (HTML5 required handles empty, but still guard)
    if (!email.trim()) {
      setError('Email address is required.');
      return;
    }
    if (!password) {
      setError('Password is required.');
      return;
    }

    setLoading(true);
    try {
      const res = await loginAdmin(email.trim(), password);
      if (!res.success) {
        setError(res.error || 'Authentication failed.');
      }
      // On success, AuthContext updates the session and App.tsx redirects automatically.
    } catch (err: any) {
      setError(friendlyError(err));
    } finally {
      setLoading(false);
    }
  };

  const emailId    = `${uid}-email`;
  const passwordId = `${uid}-password`;

  return (
    <div
      className="min-h-screen w-full flex flex-col"
      style={{ fontFamily: "'Inter', 'Segoe UI', system-ui, sans-serif", background: '#f0f4f8' }}
    >
      {/* ── Top hero section (dark navy) ───────────────────────────────────── */}
      <div
        className="flex flex-col items-center justify-end px-4"
        style={{
          background: 'linear-gradient(160deg, #1a2f5a 0%, #1d3461 60%, #213a6e 100%)',
          minHeight: 'clamp(260px, 44vh, 380px)',
          paddingBottom: 'clamp(64px, 9vw, 96px)',
        }}
      >
        {/* Logo badge */}
        <div
          className="flex flex-col items-center justify-center mb-6 shadow-2xl overflow-hidden p-2"
          style={{
            width: 110,
            height: 110,
            borderRadius: 28,
            background: '#ffffff',
            boxShadow: '0 20px 40px rgba(0,0,0,0.3)',
          }}
        >
          <img
            src="/logo.png"
            alt="Citizen AI Official Logo"
            className="w-full h-full object-contain"
          />
        </div>

        <h1
          className="font-bold text-white text-center"
          style={{ fontSize: 'clamp(2rem, 5vw, 2.4rem)', letterSpacing: '-0.01em', lineHeight: 1.2 }}
        >
          Welcome Back
        </h1>
        <p className="mt-2 text-center" style={{ color: '#a8c4e0', fontSize: 'clamp(0.875rem, 2.2vw, 1.0625rem)' }}>
          Sign in to your Citizen AI Admin account
        </p>
      </div>

      {/* ── Login card (floats over the split) ─────────────────────────────── */}
      <div className="flex-1 flex flex-col items-center px-5 pb-12" style={{ background: '#f0f4f8' }}>
        <div
          className="w-full"
          style={{
            maxWidth: 520,
            marginTop: 'clamp(-72px, -9vw, -88px)',
            position: 'relative',
            zIndex: 10,
          }}
        >
          {/* Card */}
          <div
            className="w-full rounded-3xl space-y-5"
            style={{
              background: '#ffffff',
              boxShadow: '0 16px 56px rgba(26, 47, 90, 0.16)',
              padding: 'clamp(24px, 5vw, 40px)',
            }}
          >
            {/* Error alert */}
            {error && (
              <div
                role="alert"
                aria-live="assertive"
                className="flex items-start gap-2.5 p-3.5 rounded-xl text-sm font-medium"
                style={{ background: '#fff5f5', border: '1.5px solid #fca5a5', color: '#b91c1c' }}
              >
                <AlertCircle size={17} className="shrink-0 mt-px" aria-hidden="true" />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} noValidate className="space-y-4">
              {/* Email */}
              <InputField
                id={emailId}
                label="Email / Mobile / Employee ID"
                type="email"
                value={email}
                onChange={(v) => { setEmail(v); setError(null); }}
                placeholder="admin@citizen.gov"
                autoComplete="email"
                required
                disabled={loading}
                icon={<Mail size={18} />}
              />

              {/* Password */}
              <InputField
                id={passwordId}
                label="Password"
                type={showPass ? 'text' : 'password'}
                value={password}
                onChange={(v) => { setPassword(v); setError(null); }}
                placeholder="Enter your password"
                autoComplete="current-password"
                required
                disabled={loading}
                icon={<Lock size={18} />}
                suffix={
                  <button
                    type="button"
                    onClick={() => setShowPass((s) => !s)}
                    aria-label={showPass ? 'Hide password' : 'Show password'}
                    className="shrink-0 p-1 rounded-lg transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-green-500"
                    style={{ color: '#94a3b8' }}
                    tabIndex={0}
                    disabled={loading}
                  >
                    {showPass ? <EyeOff size={17} /> : <Eye size={17} />}
                  </button>
                }
              />

              {/* Forgot password link */}
              <div className="flex justify-end">
                <button
                  type="button"
                  className="text-sm font-semibold transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-green-500 rounded"
                  style={{ color: '#2ecc71' }}
                  onClick={() => {
                    // Placeholder — wire to reset flow when available
                    alert('Please contact your system administrator to reset your password.');
                  }}
                  disabled={loading}
                >
                  Forgot Password?
                </button>
              </div>

              {/* Login button */}
              <button
                id="login-submit-btn"
                type="submit"
                disabled={loading}
                aria-disabled={loading}
                aria-busy={loading}
                className="w-full flex items-center justify-center gap-2.5 rounded-2xl font-bold text-white transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-green-500 disabled:opacity-70 active:scale-[0.98]"
                style={{
                  minHeight: 56,
                  fontSize: '1.0625rem',
                  background: 'linear-gradient(135deg, #27ae60 0%, #2ecc71 100%)',
                  boxShadow: '0 6px 28px rgba(46, 204, 113, 0.40)',
                  cursor: loading ? 'not-allowed' : 'pointer',
                }}
              >
                {loading ? (
                  <>
                    <Loader2 size={20} className="animate-spin" aria-hidden="true" />
                    <span>Signing In…</span>
                  </>
                ) : (
                  <>
                    <ShieldCheck size={20} aria-hidden="true" />
                    <span>Login</span>
                  </>
                )}
              </button>
            </form>
          </div>



          {/* Footer note */}
          <p
            className="mt-6 text-center text-xs"
            style={{ color: '#94a3b8' }}
          >
            Municipal Operations &amp; Civic Technology Platform
            <br />
            Authorised personnel only. All access is monitored and logged.
          </p>
        </div>
      </div>
    </div>
  );
};
