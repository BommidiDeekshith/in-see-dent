import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { checkApiHealth } from '../api/client';
import { Activity } from 'lucide-react';

export default function LoginPage() {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [apiUp, setApiUp] = useState<boolean | null>(null);
  const { login } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    checkApiHealth().then(setApiUp);
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await login(username, password);
      navigate('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="card w-full max-w-md">
        <div className="flex items-center gap-3 mb-6">
          <Activity className="w-8 h-8 text-accent" />
          <div>
            <h1 className="text-xl font-bold text-white">InSeeDent</h1>
            <p className="text-slate-400 text-sm">AI Incident Intelligence</p>
          </div>
        </div>
        {apiUp === false && (
          <p className="text-amber-400 text-sm mb-4 p-3 rounded-lg bg-amber-500/10 border border-amber-500/30">
            Backend not reachable. Run (no Docker needed):{' '}
            <code className="text-xs">cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local</code>
          </p>
        )}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-sm text-slate-400">Username</label>
            <input
              className="w-full mt-1 bg-surface border border-surface-border rounded-lg px-3 py-2 text-white"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>
          <div>
            <label className="text-sm text-slate-400">Password</label>
            <input
              type="password"
              className="w-full mt-1 bg-surface border border-surface-border rounded-lg px-3 py-2 text-white"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          {error && <p className="text-red-400 text-sm">{error}</p>}
          <button type="submit" disabled={loading} className="btn-primary w-full">
            {loading ? 'Signing in...' : 'Sign in'}
          </button>
        </form>
        <p className="text-slate-500 text-xs mt-4 text-center">Demo: admin / admin123</p>
      </div>
    </div>
  );
}
