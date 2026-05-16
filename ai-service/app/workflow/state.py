"""LangGraph workflow state definition for multi-agent RCA."""

from typing import Any, TypedDict, Optional


class IncidentState(TypedDict, total=False):
    incident_id: int
    title: str
    description: str
    severity: str
    affected_services: list[str]
    alerts: list[Any]
    logs: list[Any]
    metrics: list[Any]
    traces: list[Any]
    deployments: list[Any]
    # Agent outputs
    logs_findings: str
    metrics_findings: str
    traces_findings: str
    deployment_findings: str
    correlation_findings: str
    # Final RCA
    root_cause: str
    confidence_score: float
    summary: str
    remediation_suggestions: str
    artifacts: list[dict]
    retry_count: int
    error: Optional[str]
