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
    {
        "incident_id": 104,
        "title": "Elasticsearch yellow cluster — search degradation",
        "root_cause": "Stuck shard relocation on search-service cluster",
        "content": "search elasticsearch shard latency query catalog",
    },
    {
        "incident_id": 105,
        "title": "Recommendation model OOM under load",
        "root_cause": "Memory limit too low for new embedding model version",
        "content": "recommendation model-server OOM kubernetes memory embedding",
    },
    {
        "incident_id": 106,
        "title": "Order idempotency regression — duplicate charges",
        "root_cause": "Missing idempotency key on retry path in order-service",
        "content": "order-service payment duplicate charge idempotency postgres",
    },
    {
        "incident_id": 107,
        "title": "CDN origin 502 burst",
        "root_cause": "Origin timeout caused edge PoP errors in EU region",
        "content": "cdn edge 502 origin timeout api-gateway static assets",
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
