import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Activity, LogOut } from 'lucide-react';

export default function Layout({ children }: { children: React.ReactNode }) {
  const { username, logout } = useAuth();

  return (
    <div className="min-h-screen flex flex-col">
      <header className="border-b border-surface-border bg-surface-card/80 backdrop-blur sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2">
            <Activity className="w-6 h-6 text-accent" />
            <span className="font-bold text-lg">InSeeDent</span>
          </Link>
          <div className="flex items-center gap-4 text-sm text-slate-400">
            <span>{username}</span>
            <button onClick={logout} className="flex items-center gap-1 hover:text-white">
              <LogOut className="w-4 h-4" /> Logout
            </button>
          </div>
        </div>
      </header>
      <main className="flex-1 max-w-7xl mx-auto w-full px-4 py-6">{children}</main>
    </div>
  );
}
