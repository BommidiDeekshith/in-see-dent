const API_BASE = import.meta.env.VITE_API_URL || '';

export interface ApiResponse<T> {
  status: string;
  data: T;
  message?: string;
}

function authHeaders(): HeadersInit {
  const token = localStorage.getItem('token');
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

async function parseJsonResponse<T>(res: Response): Promise<ApiResponse<T>> {
  const text = await res.text();
  if (!text) {
    throw new Error(
      res.ok
        ? 'Empty response from server'
        : `Server error (${res.status}). Is the backend running on port 8080?`
    );
  }
  try {
    return JSON.parse(text) as ApiResponse<T>;
  } catch {
    const preview = text.slice(0, 80).replace(/\s+/g, ' ');
    throw new Error(
      `API returned non-JSON (HTTP ${res.status}). ` +
        `Start the backend: cd backend && mvn spring-boot:run. ` +
        (preview.startsWith('<') ? 'Got HTML — wrong URL or proxy.' : `Response: ${preview}`)
    );
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers: { ...authHeaders(), ...options.headers },
    });
  } catch {
    throw new Error(
      'Cannot reach the API. Start the backend (port 8080) and use npm run dev for the frontend, or run docker compose up.'
    );
  }

  const json = await parseJsonResponse<T>(res);
  if (!res.ok || json.status !== 'success') {
    throw new Error(json.message || `Request failed (${res.status})`);
  }
  if (json.data === undefined || json.data === null) {
    throw new Error(json.message || 'No data in API response');
  }
  return json.data;
}

export async function checkApiHealth(): Promise<boolean> {
  try {
    const res = await fetch(`${API_BASE}/api/auth/health`);
    return res.ok;
  } catch {
    return false;
  }
}

export const api = {
  login: (username: string, password: string) =>
    request<{ token: string; username: string }>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),

  getIncidents: (page = 0, status?: string, severity?: string) => {
    const params = new URLSearchParams({ page: String(page), size: '20' });
    if (status) params.set('status', status);
    if (severity) params.set('severity', severity);
    return request<{ content: Incident[]; totalElements: number }>(`/api/v1/incidents?${params}`);
  },

  getIncident: (id: number) => request<Incident>(`/api/v1/incidents/${id}`),

  getTimeline: (id: number) => request<TimelineEvent[]>(`/api/v1/incidents/${id}/timeline`),

  triggerAnalysis: (id: number) =>
    request<AnalysisResult>(`/api/v1/incidents/${id}/analysis`, { method: 'POST' }),

  getLatestAnalysis: (id: number) =>
    request<AnalysisResult>(`/api/v1/incidents/${id}/analysis/latest`),

  ingestMock: (id: number) =>
    request<Record<string, number>>(`/api/v1/observability/incidents/${id}/ingest-mock`, {
      method: 'POST',
    }),

  chat: (incidentId: number | null, message: string, sessionId?: string) =>
    request<ChatResult>('/api/v1/chat', {
      method: 'POST',
      body: JSON.stringify({ incidentId, message, sessionId }),
    }),

  getDependencies: () => request<ServiceNode[]>('/api/v1/services/dependencies'),

  searchSimilar: (id: number) =>
    request<SimilarIncident[]>(`/api/v1/search/incidents/${id}/similar`),
};

export interface Incident {
  id: number;
  title: string;
  description?: string;
  severity: string;
  status: string;
  affectedServices?: string[];
  startTime: string;
  endTime?: string;
  detectedBy?: string;
}

export interface TimelineEvent {
  id: number;
  eventType: string;
  eventSource?: string;
  description: string;
  severity?: string;
  eventTime: string;
}

export interface AnalysisResult {
  id?: number;
  incidentId?: number;
  status: string;
  rootCause?: string;
  confidenceScore?: number;
  affectedServices?: string[];
  summary?: string;
  remediationSuggestions?: string;
}

export interface ChatResult {
  reply: string;
  sessionId: string;
  suggestedPrompts?: string[];
  similarIncidents?: SimilarIncident[];
}

export interface SimilarIncident {
  incidentId: number;
  title: string;
  similarityScore: number;
  rootCause?: string;
}

export interface ServiceNode {
  id: string;
  label: string;
  status: string;
  dependencies: string[];
}
