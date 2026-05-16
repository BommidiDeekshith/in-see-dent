import { useState } from 'react';
import { api, ChatResult } from '../api/client';
import { MessageSquare, Send } from 'lucide-react';

const PROMPTS = [
  'Why did checkout-service fail?',
  'Show similar incidents',
  'What changed before the outage?',
];

export default function ChatSidebar({ incidentId }: { incidentId: number }) {
  const [messages, setMessages] = useState<{ role: 'user' | 'assistant'; text: string }[]>([]);
  const [input, setInput] = useState('');
  const [sessionId, setSessionId] = useState<string>();
  const [loading, setLoading] = useState(false);

  const send = async (text: string) => {
    if (!text.trim()) return;
    setMessages((m) => [...m, { role: 'user', text }]);
    setInput('');
    setLoading(true);
    try {
      const res: ChatResult = await api.chat(incidentId, text, sessionId);
      setSessionId(res.sessionId);
      setMessages((m) => [...m, { role: 'assistant', text: res.reply }]);
    } catch {
      setMessages((m) => [...m, { role: 'assistant', text: 'Unable to reach assistant.' }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card flex flex-col h-[500px]">
      <div className="flex items-center gap-2 border-b border-surface-border pb-3 mb-3">
        <MessageSquare className="w-5 h-5 text-accent" />
        <h3 className="font-semibold">Investigation Assistant</h3>
      </div>
      <div className="flex-1 overflow-y-auto space-y-3 text-sm">
        {messages.length === 0 && (
          <p className="text-slate-500">Ask about root cause, deployments, or similar incidents.</p>
        )}
        {messages.map((m, i) => (
          <div
            key={i}
            className={`p-2 rounded-lg ${m.role === 'user' ? 'bg-accent/20 ml-4' : 'bg-surface mr-4'}`}
          >
            {m.text}
          </div>
        ))}
      </div>
      <div className="flex flex-wrap gap-1 mb-2">
        {PROMPTS.map((p) => (
          <button
            key={p}
            type="button"
            onClick={() => send(p)}
            className="text-xs px-2 py-1 rounded bg-surface-border hover:bg-accent/20"
          >
            {p}
          </button>
        ))}
      </div>
      <form
        className="flex gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          send(input);
        }}
      >
        <input
          className="flex-1 bg-surface border border-surface-border rounded-lg px-3 py-2 text-sm"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask about this incident..."
        />
        <button type="submit" disabled={loading} className="btn-primary p-2">
          <Send className="w-4 h-4" />
        </button>
      </form>
    </div>
  );
}
