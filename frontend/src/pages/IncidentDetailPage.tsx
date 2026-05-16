import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import Layout from '../components/Layout';
import SeverityBadge from '../components/SeverityBadge';
import IncidentTimeline from '../components/IncidentTimeline';
import ChatSidebar from '../components/ChatSidebar';
import { api, Incident, TimelineEvent, AnalysisResult, SimilarIncident } from '../api/client';
import { ArrowLeft, Play, Database } from 'lucide-react';

export default function IncidentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const incidentId = Number(id);
  const [incident, setIncident] = useState<Incident | null>(null);
  const [timeline, setTimeline] = useState<TimelineEvent[]>([]);
  const [analysis, setAnalysis] = useState<AnalysisResult | null>(null);
  const [similar, setSimilar] = useState<SimilarIncident[]>([]);
  const [busy, setBusy] = useState('');

  const load = () => {
    api.getIncident(incidentId).then(setIncident).catch(console.error);
    api.getTimeline(incidentId).then(setTimeline).catch(console.error);
    api.getLatestAnalysis(incidentId).then(setAnalysis).catch(() => setAnalysis(null));
    api.searchSimilar(incidentId).then(setSimilar).catch(() => setSimilar([]));
  };

  useEffect(() => {
    load();
  }, [incidentId]);

  const runIngest = async () => {
    setBusy('ingest');
    await api.ingestMock(incidentId);
    load();
    setBusy('');
  };

  const runAnalysis = async () => {
    setBusy('analysis');
    await api.triggerAnalysis(incidentId);
    setTimeout(load, 2000);
    setBusy('');
  };

  if (!incident) {
    return (
      <Layout>
        <p className="text-slate-400">Loading incident...</p>
      </Layout>
    );
  }

  return (
    <Layout>
      <Link to="/" className="inline-flex items-center gap-1 text-slate-400 hover:text-white text-sm mb-4">
        <ArrowLeft className="w-4 h-4" /> Back to dashboard
      </Link>

      <div className="flex flex-wrap items-start justify-between gap-4 mb-6">
        <div>
          <div className="flex items-center gap-3 mb-2">
            <h1 className="text-xl font-bold">{incident.title}</h1>
            <SeverityBadge severity={incident.severity} />
          </div>
          <p className="text-slate-400 text-sm max-w-2xl">{incident.description}</p>
          <p className="text-slate-500 text-xs mt-2">
            Services: {incident.affectedServices?.join(', ')} · Status: {incident.status}
          </p>
        </div>
        <div className="flex gap-2">
          <button onClick={runIngest} disabled={!!busy} className="btn-primary flex items-center gap-2 text-sm">
            <Database className="w-4 h-4" /> Ingest Mock Telemetry
          </button>
          <button onClick={runAnalysis} disabled={!!busy} className="btn-primary flex items-center gap-2 text-sm">
            <Play className="w-4 h-4" /> Run AI RCA
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-4">
          <div className="card">
            <h3 className="text-sm font-semibold text-slate-300 mb-3">AI Root Cause Analysis</h3>
            {analysis?.rootCause ? (
              <>
                <div className="flex items-center gap-4 mb-3">
                  <div className="text-3xl font-bold text-accent">
                    {Math.round((analysis.confidenceScore || 0) * 100)}%
                  </div>
                  <p className="text-slate-400 text-sm">confidence</p>
                </div>
                <p className="text-white font-medium mb-2">{analysis.rootCause}</p>
                <p className="text-slate-400 text-sm mb-4">{analysis.summary}</p>
                <h4 className="text-sm text-slate-300 mb-2">Remediation</h4>
                <pre className="text-sm text-slate-400 whitespace-pre-wrap bg-surface rounded p-3">
                  {analysis.remediationSuggestions}
                </pre>
              </>
            ) : (
              <p className="text-slate-500 text-sm">
                No analysis yet. Ingest telemetry and run AI RCA, or create incident to trigger Kafka workflow.
              </p>
            )}
          </div>
          <IncidentTimeline events={timeline} />
          {similar.length > 0 && (
            <div className="card">
              <h3 className="text-sm font-semibold text-slate-300 mb-3">Similar Historical Incidents</h3>
              <ul className="space-y-2">
                {similar.map((s) => (
                  <li key={s.incidentId} className="text-sm border-b border-surface-border pb-2">
                    <span className="text-accent">{Math.round(s.similarityScore * 100)}% match</span> — {s.title}
                    {s.rootCause && <p className="text-slate-500 text-xs">{s.rootCause}</p>}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
        <ChatSidebar incidentId={incidentId} />
      </div>
    </Layout>
  );
}
