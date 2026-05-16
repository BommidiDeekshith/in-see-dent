import { TimelineEvent } from '../api/client';
import { AlertCircle, GitBranch, Activity, FileWarning } from 'lucide-react';

const iconMap: Record<string, typeof AlertCircle> = {
  alert: AlertCircle,
  deployment: GitBranch,
  metric_spike: Activity,
  trace_error: FileWarning,
  ai_analysis: Activity,
};

export default function IncidentTimeline({ events }: { events: TimelineEvent[] }) {
  return (
    <div className="card">
      <h3 className="text-sm font-semibold text-slate-300 mb-4">Incident Timeline</h3>
      <div className="relative border-l border-surface-border ml-3 space-y-6">
        {events.map((e) => {
          const Icon = iconMap[e.eventType] || AlertCircle;
          return (
            <div key={e.id} className="relative pl-6">
              <span className="absolute -left-2 top-1 w-4 h-4 rounded-full bg-accent/30 border border-accent flex items-center justify-center">
                <Icon className="w-2.5 h-2.5 text-accent" />
              </span>
              <p className="text-xs text-slate-500">{new Date(e.eventTime).toLocaleString()}</p>
              <p className="text-sm text-white font-medium capitalize">{e.eventType.replace('_', ' ')}</p>
              <p className="text-sm text-slate-400">{e.description}</p>
              {e.eventSource && <p className="text-xs text-slate-500">{e.eventSource}</p>}
            </div>
          );
        })}
      </div>
    </div>
  );
}
