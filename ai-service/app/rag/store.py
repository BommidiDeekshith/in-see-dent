"""In-memory RAG store for similar incident retrieval (demo)."""

from __future__ import annotations

import hashlib
from typing import Any


# Seeded historical incidents for hackathon demo
_HISTORICAL = [
    {
        "incident_id": 101,
        "title": "Checkout DB pool exhaustion — Black Friday 2025",
        "root_cause": "HikariCP pool saturation under traffic spike after deployment",
        "content": "checkout-service connection pool timeout HikariPool deployment spike 5xx",
    },
    {
        "incident_id": 102,
        "title": "Payment gateway timeout cascade",
        "root_cause": "payment-gateway p99 latency caused checkout circuit breaker trips",
        "content": "payment-gateway timeout checkout circuit breaker upstream latency",
    },
    {
        "incident_id": 103,
        "title": "Auth JWT validation failures during peak",
        "root_cause": "Redis cache eviction increased auth DB load",
        "content": "auth-service JWT redis cache eviction validation errors",
    },
]


def _tokenize(text: str) -> set[str]:
    return set(text.lower().replace("-", " ").split())


def search_similar(title: str, description: str = "", top_k: int = 3) -> list[dict[str, Any]]:
    query_tokens = _tokenize(f"{title} {description}")
    scored = []
    for doc in _HISTORICAL:
        doc_tokens = _tokenize(doc["content"] + " " + doc["title"])
        overlap = len(query_tokens & doc_tokens)
        score = overlap / max(len(query_tokens), 1)
        scored.append({**doc, "score": round(min(score + 0.3, 0.95), 2)})

    scored.sort(key=lambda x: x["score"], reverse=True)
    return [
        {
            "incident_id": s["incident_id"],
            "title": s["title"],
            "root_cause": s["root_cause"],
            "score": s["score"],
        }
        for s in scored[:top_k]
    ]


def embed_text(text: str) -> str:
    return hashlib.sha256(text.encode()).hexdigest()[:16]
