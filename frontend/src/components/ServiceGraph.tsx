import { ServiceNode } from '../api/client';

const statusColor: Record<string, string> = {
  healthy: 'border-green-500/50 bg-green-500/10',
  degraded: 'border-yellow-500/50 bg-yellow-500/10',
  critical: 'border-red-500/50 bg-red-500/10',
};

export default function ServiceGraph({ nodes }: { nodes: ServiceNode[] }) {
  return (
    <div className="card">
      <h3 className="text-sm font-semibold text-slate-300 mb-4">Service Dependencies</h3>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {nodes.map((n) => (
          <div key={n.id} className={`border rounded-lg p-3 ${statusColor[n.status] || statusColor.healthy}`}>
            <p className="font-medium text-white">{n.label}</p>
            <p className="text-xs text-slate-400 mt-1 capitalize">{n.status}</p>
            {n.dependencies.length > 0 && (
              <p className="text-xs text-slate-500 mt-2">→ {n.dependencies.join(', ')}</p>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
