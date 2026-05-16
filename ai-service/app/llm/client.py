"""
LLM client for InSeeDent — supports offline (Ollama), cloud (OpenAI), or rule-based mock.

Set LLM_MODE:
  - mock   (default) — deterministic RCA, no network, no GPU
  - ollama — local models via Ollama (llama3.2, mistral, etc.)
  - openai — OpenAI API (requires OPENAI_API_KEY)
"""

from __future__ import annotations

import json
import os
import re
from typing import Any

import httpx


def get_llm_mode() -> str:
    return os.getenv("LLM_MODE", "mock").lower().strip()


def llm_status() -> dict[str, Any]:
    mode = get_llm_mode()
    status: dict[str, Any] = {"mode": mode, "available": mode == "mock"}
    if mode == "ollama":
        base = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
        model = os.getenv("OLLAMA_MODEL", "llama3.2")
        status["model"] = model
        status["base_url"] = base
        try:
            r = httpx.get(f"{base}/api/tags", timeout=3.0)
            status["available"] = r.status_code == 200
            if r.status_code == 200:
                names = [m.get("name", "") for m in r.json().get("models", [])]
                status["models_installed"] = names[:10]
                status["model_ready"] = any(model in n for n in names)
        except Exception as e:
            status["available"] = False
            status["error"] = str(e)
    elif mode == "openai":
        status["available"] = bool(os.getenv("OPENAI_API_KEY"))
        status["model"] = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
    return status


def invoke_llm(system_prompt: str, user_prompt: str, max_tokens: int = 1024) -> str | None:
    """Call configured LLM. Returns None if unavailable (caller should use mock fallback)."""
    mode = get_llm_mode()
    if mode == "mock":
        return None
    if mode == "ollama":
        return _invoke_ollama(system_prompt, user_prompt, max_tokens)
    if mode == "openai":
        return _invoke_openai(system_prompt, user_prompt, max_tokens)
    return None


def _invoke_ollama(system_prompt: str, user_prompt: str, max_tokens: int) -> str | None:
    base = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434").rstrip("/")
    model = os.getenv("OLLAMA_MODEL", "llama3.2")
    try:
        from langchain_ollama import ChatOllama
        from langchain_core.messages import HumanMessage, SystemMessage

        llm = ChatOllama(
            base_url=base,
            model=model,
            temperature=0.2,
            num_predict=max_tokens,
        )
        messages = [SystemMessage(content=system_prompt), HumanMessage(content=user_prompt)]
        response = llm.invoke(messages)
        return response.content if response else None
    except ImportError:
        # Fallback: raw Ollama HTTP API (no langchain-ollama installed)
        try:
            payload = {
                "model": model,
                "messages": [
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt},
                ],
                "stream": False,
                "options": {"num_predict": max_tokens, "temperature": 0.2},
            }
            r = httpx.post(f"{base}/api/chat", json=payload, timeout=120.0)
            r.raise_for_status()
            return r.json().get("message", {}).get("content")
        except Exception:
            return None
    except Exception:
        return None


def _invoke_openai(system_prompt: str, user_prompt: str, max_tokens: int) -> str | None:
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        return None
    try:
        from langchain_openai import ChatOpenAI
        from langchain_core.messages import HumanMessage, SystemMessage

        llm = ChatOpenAI(
            model=os.getenv("OPENAI_MODEL", "gpt-4o-mini"),
            api_key=api_key,
            temperature=0.2,
            max_tokens=max_tokens,
        )
        messages = [SystemMessage(content=system_prompt), HumanMessage(content=user_prompt)]
        response = llm.invoke(messages)
        return response.content if response else None
    except Exception:
        return None


def parse_rca_json(text: str) -> dict[str, Any] | None:
    """Extract JSON object from LLM response."""
    if not text:
        return None
    match = re.search(r"\{[\s\S]*\}", text)
    if not match:
        return None
    try:
        return json.loads(match.group())
    except json.JSONDecodeError:
        return None
