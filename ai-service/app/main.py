"""InSeeDent AI Orchestration Service — FastAPI + LangGraph."""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Any, Optional

import json

from app.llm.client import get_llm_mode, invoke_llm, llm_status
from app.rag.store import search_similar
from app.workflow.graph import _load_prompts, run_analysis

app = FastAPI(
    title="InSeeDent AI Service",
    description="LangGraph multi-agent root cause analysis orchestration",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


class AnalyzeRequest(BaseModel):
    incident_id: Optional[int] = None
    title: str = ""
    description: str = ""
    severity: str = "high"
    affected_services: list[str] = []
    alerts: list[Any] = []
    logs: list[Any] = []
    metrics: list[Any] = []
    traces: list[Any] = []
    deployments: list[Any] = []


class ChatRequest(BaseModel):
    message: str
    session_id: Optional[str] = None
    incident_id: Optional[int] = None
    incident_title: Optional[str] = None
    affected_services: list[str] = []
    latest_analysis: Optional[str] = None


class SearchRequest(BaseModel):
    incident_id: Optional[int] = None
    title: str = ""
    description: str = ""


@app.get("/health")
def health():
    return {"status": "ok", "service": "inseedent-ai", "llm": llm_status()}


@app.post("/api/v1/analyze")
def analyze(req: AnalyzeRequest):
  """Trigger LangGraph multi-agent RCA workflow."""
  result = run_analysis(req.model_dump())
  return result


def _chat_with_llm(req: ChatRequest) -> str | None:
    if get_llm_mode() == "mock":
        return None
    prompts = _load_prompts()
    context = {
        "incident_id": req.incident_id,
        "incident_title": req.incident_title,
        "affected_services": req.affected_services,
        "latest_analysis": req.latest_analysis,
        "user_message": req.message,
    }
    user_prompt = f"Context:\n{json.dumps(context, indent=2)}\n\nUser question: {req.message}"
    return invoke_llm(prompts.get("chat_agent", "You are an SRE investigation assistant."), user_prompt, max_tokens=512)


@app.post("/api/v1/chat")
def chat(req: ChatRequest):
    llm_reply = _chat_with_llm(req)
    if llm_reply:
        similar = search_similar(req.incident_title or "", req.message)
        return {
            "reply": llm_reply,
            "session_id": req.session_id or "demo-session",
            "similar_incidents": similar,
            "llm_mode": get_llm_mode(),
        }

    msg = req.message.lower()
    service = req.affected_services[0] if req.affected_services else "checkout-service"

    if "similar" in msg:
        similar = search_similar(req.incident_title or service, msg)
        reply = f"Found {len(similar)} similar historical incidents. Top match: {similar[0]['title']} (score {similar[0]['score']})."
    elif "why" in msg and "fail" in msg:
        reply = (
            f"{service} likely failed due to: (1) recent deployment increasing pool usage, "
            f"(2) payment-gateway timeouts causing circuit breaker OPEN, "
            f"(3) connection pool exhaustion visible in ERROR logs."
        )
    elif "chang" in msg or "deploy" in msg:
        reply = (
            "Before the outage: checkout-service v2.4.1 was deployed ~12 minutes prior. "
            "This correlates with the first metric spike and trace errors."
        )
    elif "remediat" in msg or "fix" in msg:
        reply = (
            "Recommended: 1) Roll back v2.4.1  2) Scale replicas  3) Increase DB pool size  "
            "4) Enable payment-gateway circuit breaker  5) Review runbook for pool exhaustion."
        )
    else:
        ctx = req.latest_analysis or "Analysis in progress."
        reply = f"Regarding incident '{req.incident_title or 'current'}': {ctx}"

    similar = search_similar(req.incident_title or service, req.message)
    return {
        "reply": reply,
        "session_id": req.session_id or "demo-session",
        "similar_incidents": similar,
        "llm_mode": get_llm_mode(),
    }


@app.post("/api/v1/search/similar")
def similar_search(req: SearchRequest):
    results = search_similar(req.title, req.description)
    return {"results": results}
