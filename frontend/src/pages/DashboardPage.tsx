import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Area, AreaChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import Layout from '../components/Layout';
import SeverityBadge from '../components/SeverityBadge';
import ServiceGraph from '../components/ServiceGraph';
import { api, Incident, ServiceNode } from '../api/client';
import { AlertTriangle, Brain, Zap } from 'lucide-react';

const chartData = [
  { t: '10:00', errors: 12, latency: 120 },
  { t: '10:15', errors: 45, latency: 280 },
  { t: '10:30', errors: 180, latency: 890 },
  { t: '10:45', errors: 95, latency: 420 },
  { t: '11:00', errors: 60, latency: 310 },
];

export default function DashboardPage() {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [services, setServices] = useState<ServiceNode[]>([]);
  const [filter, setFilter] = useState({ status: '', severity: '' });

  useEffect(() => {
    api.getIncidents(0, filter.status || undefined, filter.severity || undefined)
      .then((p) => setIncidents(p.content))
      .catch(console.error);
    api.getDependencies().then(setServices).catch(console.error);
  }, [filter]);

  const stats = {
    active: incidents.filter((i) => ['open', 'investigating'].includes(i.status)).length,
    critical: incidents.filter((i) => i.severity === 'critical').length,
    resolved: incidents.filter((i) => i.status === 'resolved').length,
  };

  return (
    <Layout>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold">Incident Command Center</h1>
          <p className="text-slate-400 text-sm">Multi-agent AI root cause analysis for production incidents</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="card flex items-center gap-3">
          <AlertTriangle className="w-8 h-8 text-severity-critical" />
          <div>
            <p className="text-2xl font-bold">{stats.active}</p>
            <p className="text-slate-400 text-sm">Active Incidents</p>
          </div>
        </div>
        <div className="card flex items-center gap-3">
          <Zap className="w-8 h-8 text-severity-high" />
          <div>
            <p className="text-2xl font-bold">{stats.critical}</p>
            <p className="text-slate-400 text-sm">Critical Severity</p>
          </div>
        </div>
        <div className="card flex items-center gap-3">
          <Brain className="w-8 h-8 text-accent" />
          <div>
            <p className="text-2xl font-bold">{stats.resolved}</p>
            <p className="text-slate-400 text-sm">Resolved Today</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-4">
          <div className="card">
            <div className="flex gap-2 mb-4">
              <select
                className="bg-surface border border-surface-border rounded px-2 py-1 text-sm"
                value={filter.status}
                onChange={(e) => setFilter({ ...filter, status: e.target.value })}
              >
                <option value="">All statuses</option>
                <option value="open">Open</option>
                <option value="investigating">Investigating</option>
                <option value="resolved">Resolved</option>
              </select>
              <select
                className="bg-surface border border-surface-border rounded px-2 py-1 text-sm"
                value={filter.severity}
                onChange={(e) => setFilter({ ...filter, severity: e.target.value })}
              >
                <option value="">All severities</option>
                <option value="critical">Critical</option>
                <option value="high">High</option>
                <option value="medium">Medium</option>
              </select>
            </div>
            <div className="space-y-2">
              {incidents.map((inc) => (
                <Link
                  key={inc.id}
                  to={`/incidents/${inc.id}`}
                  className="block border border-surface-border rounded-lg p-4 hover:border-accent/50 transition-colors"
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <h3 className="font-medium text-white">{inc.title}</h3>
                      <p className="text-slate-400 text-sm mt-1">
                        {inc.affectedServices?.join(', ') || '—'} · {new Date(inc.startTime).toLocaleString()}
                      </p>
                    </div>
                    <SeverityBadge severity={inc.severity} />
                  </div>
                  <span className="text-xs text-slate-500 capitalize mt-2 inline-block">{inc.status}</span>
                </Link>
              ))}
            </div>
          </div>
        </div>

        <div className="space-y-4">
          <div className="card h-48">
            <h3 className="text-sm font-semibold text-slate-300 mb-2">Error Rate (mock)</h3>
            <ResponsiveContainer width="100%" height="85%">
              <AreaChart data={chartData}>
                <XAxis dataKey="t" stroke="#64748b" fontSize={10} />
                <YAxis stroke="#64748b" fontSize={10} />
                <Tooltip contentStyle={{ background: '#1a2332', border: '1px solid #2d3a4f' }} />
                <Area type="monotone" dataKey="errors" stroke="#ef4444" fill="#ef444433" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
          <ServiceGraph nodes={services} />
        </div>
      </div>
    </Layout>
  );
}
