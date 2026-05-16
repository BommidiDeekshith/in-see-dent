"""
LangGraph Multi-Agent RCA Workflow for InSeeDent
=================================================

Flow:
  log_agent → metrics_agent → trace_agent → deployment_agent
        → correlation_agent → summarizer_agent → END

If langgraph is not installed, the same pipeline runs as a plain Python chain.
"""

from __future__ import annotations

import os
from typing import Any, Callable

import yaml

from app.workflow.state import IncidentState

try:
    from langgraph.graph import END, StateGraph

    HAS_LANGGRAPH = True
except ImportError:
    HAS_LANGGRAPH = False
    END = "__end__"

PROMPTS_PATH = os.path.join(os.path.dirname(__file__), "..", "prompts", "sample_prompts.yaml")


def _load_prompts() -> dict:
    with open(PROMPTS_PATH) as f:
        return yaml.safe_load(f)


def _mock_analyze(state: IncidentState, agent: str, findings: str, confidence: float) -> dict:
    return {
        **state,
        f"{agent}_findings": findings,
        "artifacts": state.get("artifacts", [])
        + [{"type": f"{agent}_analysis", "agent": agent, "findings": findings, "confidence": confidence}],
    }


def log_agent(state: IncidentState) -> IncidentState:
    logs = state.get("logs", [])
    errors = [l for l in logs if isinstance(l, dict) and str(l.get("logLevel", l.get("log_level", ""))).upper() == "ERROR"]
    findings = (
        f"Detected {len(errors)} ERROR log entries. "
        "Patterns: connection pool exhaustion (HikariPool), upstream payment-gateway timeouts, "
        "circuit breaker OPEN on checkout-service."
        if logs
        else "No logs provided; infer from incident description."
    )
    return _mock_analyze(state, "logs", findings, 0.78)


def metrics_agent(state: IncidentState) -> IncidentState:
    metrics = state.get("metrics", [])
    findings = (
        f"Analyzed {len(metrics)} metric samples. p99 latency spike +340%, 5xx rate > 5% on affected services."
        if metrics
        else "Metric spike on http_request_duration_seconds and elevated http_requests_total error ratio."
    )
    return _mock_analyze(state, "metrics", findings, 0.81)


def trace_agent(state: IncidentState) -> IncidentState:
    traces = state.get("traces", [])
    findings = (
        f"Analyzed {len(traces)} traces. Critical path: checkout → payment-gateway timeout (4500ms span). "
        "Root span status ERROR: Upstream dependency timeout."
        if traces
        else "Trace failures expected on checkout-service POST /api/checkout spans."
    )
    return _mock_analyze(state, "traces", findings, 0.85)


def deployment_agent(state: IncidentState) -> IncidentState:
    deployments = state.get("deployments", [])
    if deployments:
        d = deployments[0] if isinstance(deployments[0], dict) else {}
        ver = d.get("version", "v2.4.1")
        svc = d.get("serviceName", d.get("service_name", "checkout-service"))
        findings = f"Deployment {svc} {ver} occurred ~12 min before incident. High correlation with error onset."
    else:
        findings = "Deployment v2.4.1 to checkout-service likely preceded incident (mock)."
    return _mock_analyze(state, "deployment", findings, 0.88)


def correlation_agent(state: IncidentState) -> IncidentState:
    narrative = (
        f"CORRELATED TIMELINE for '{state.get('title', 'incident')}':\n"
        f"1) Deployment: {state.get('deployment_findings', 'N/A')}\n"
        f"2) Metrics: {state.get('metrics_findings', 'N/A')}\n"
        f"3) Logs: {state.get('logs_findings', 'N/A')}\n"
        f"4) Traces: {state.get('traces_findings', 'N/A')}\n"
        "Causal chain: deploy → pool saturation → upstream timeouts → 5xx spike."
    )
    artifacts = state.get("artifacts", [])
    artifacts.append({"type": "correlation", "agent": "correlation", "findings": narrative, "confidence": 0.86})
    return {**state, "correlation_findings": narrative, "artifacts": artifacts}


def summarizer_agent(state: IncidentState) -> IncidentState:
    services = state.get("affected_services") or ["checkout-service", "payment-gateway"]
    if isinstance(services, str):
        services = [services]

    root_cause = (
        f"Cascading failure in {services[0]}: deployment v2.4.1 increased connection pool pressure, "
        "causing payment-gateway timeouts and elevated 5xx error rate."
    )
    confidence = 0.84
    retry = state.get("retry_count", 0)

    if confidence < 0.7 and retry < 1:
        return {**state, "retry_count": retry + 1}

    return {
        **state,
        "root_cause": root_cause,
        "confidence_score": confidence,
        "summary": state.get("correlation_findings", root_cause)[:500],
        "remediation_suggestions": (
            "1. Roll back checkout-service to v2.4.0\n"
            "2. Scale checkout-service replicas (+3)\n"
            "3. Increase HikariCP maximumPoolSize from 10 to 25\n"
            "4. Enable circuit breaker on payment-gateway client\n"
            "5. Add pool exhaustion alert in Prometheus"
        ),
        "artifacts": state.get("artifacts", []),
    }


_PIPELINE: list[Callable[[IncidentState], IncidentState]] = [
    log_agent,
    metrics_agent,
    trace_agent,
    deployment_agent,
    correlation_agent,
    summarizer_agent,
]


def _run_pipeline(initial: IncidentState) -> IncidentState:
    """Fallback: run agents sequentially without LangGraph."""
    state = dict(initial)
    for step in _PIPELINE:
        state = step(state)
    return state


def build_rca_graph():
    if not HAS_LANGGRAPH:
        return None
    graph = StateGraph(IncidentState)
    graph.add_node("log_agent", log_agent)
    graph.add_node("metrics_agent", metrics_agent)
    graph.add_node("trace_agent", trace_agent)
    graph.add_node("deployment_agent", deployment_agent)
    graph.add_node("correlation_agent", correlation_agent)
    graph.add_node("summarizer_agent", summarizer_agent)
    graph.set_entry_point("log_agent")
    graph.add_edge("log_agent", "metrics_agent")
    graph.add_edge("metrics_agent", "trace_agent")
    graph.add_edge("trace_agent", "deployment_agent")
    graph.add_edge("deployment_agent", "correlation_agent")
    graph.add_edge("correlation_agent", "summarizer_agent")
    graph.add_edge("summarizer_agent", END)
    return graph.compile()


def run_analysis(payload: dict[str, Any]) -> dict[str, Any]:
    initial: IncidentState = {
        "incident_id": payload.get("incident_id", 0),
        "title": payload.get("title", "Unknown incident"),
        "description": payload.get("description", ""),
        "severity": payload.get("severity", "high"),
        "affected_services": payload.get("affected_services") or ["checkout-service"],
        "alerts": _serialize(payload.get("alerts", [])),
        "logs": _serialize(payload.get("logs", [])),
        "metrics": _serialize(payload.get("metrics", [])),
        "traces": _serialize(payload.get("traces", [])),
        "deployments": _serialize(payload.get("deployments", [])),
        "artifacts": [],
        "retry_count": 0,
    }

    if HAS_LANGGRAPH:
        app = build_rca_graph()
        result = app.invoke(initial)
    else:
        result = _run_pipeline(initial)

    return {
        "root_cause": result.get("root_cause", ""),
        "confidence_score": result.get("confidence_score", 0.0),
        "affected_services": result.get("affected_services", []),
        "summary": result.get("summary", ""),
        "remediation_suggestions": result.get("remediation_suggestions", ""),
        "findings": {
            "logs": result.get("logs_findings"),
            "metrics": result.get("metrics_findings"),
            "traces": result.get("traces_findings"),
            "deployments": result.get("deployment_findings"),
            "correlation": result.get("correlation_findings"),
        },
        "artifacts": result.get("artifacts", []),
    }


def _serialize(items: list) -> list:
    out = []
    for item in items:
        if hasattr(item, "__dict__"):
            out.append({k: str(v) for k, v in item.__dict__.items() if not k.startswith("_")})
        elif isinstance(item, dict):
            out.append(item)
        else:
            out.append({"raw": str(item)})
    return out
