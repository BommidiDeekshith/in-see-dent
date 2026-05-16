-- V001__initial_schema.sql
-- InSeeDent Database Schema

-- Users Table
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(255) UNIQUE NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Incidents Table
CREATE TABLE incidents (
  id BIGSERIAL PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  severity VARCHAR(50) NOT NULL, -- critical, high, medium, low
  status VARCHAR(50) NOT NULL DEFAULT 'open', -- open, investigating, resolved, closed
  affected_services TEXT[], -- JSON array of service names
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP,
  detected_by VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT REFERENCES users(id)
);

CREATE INDEX idx_incidents_status ON incidents(status);
CREATE INDEX idx_incidents_severity ON incidents(severity);
CREATE INDEX idx_incidents_start_time ON incidents(start_time DESC);

-- Alerts Table
CREATE TABLE alerts (
  id BIGSERIAL PRIMARY KEY,
  incident_id BIGINT REFERENCES incidents(id) ON DELETE CASCADE,
  alert_source VARCHAR(100) NOT NULL, -- Prometheus, Grafana, etc.
  alert_name VARCHAR(255) NOT NULL,
  severity VARCHAR(50),
  description TEXT,
  rule_name VARCHAR(255),
  alert_data JSONB,
  triggered_at TIMESTAMP NOT NULL,
  resolved_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alerts_incident ON alerts(incident_id);
CREATE INDEX idx_alerts_triggered_at ON alerts(triggered_at DESC);

-- Metrics Table
CREATE TABLE metrics (
  id BIGSERIAL PRIMARY KEY,
  incident_id BIGINT REFERENCES incidents(id) ON DELETE CASCADE,
  metric_name VARCHAR(255) NOT NULL,
  service_name VARCHAR(255),
  metric_type VARCHAR(50), -- gauge, counter, histogram, summary
  metric_value DOUBLE PRECISION,
  timestamp TIMESTAMP NOT NULL,
  labels JSONB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_metrics_incident ON metrics(incident_id);
CREATE INDEX idx_metrics_timestamp ON metrics(timestamp DESC);

-- Logs Table
CREATE TABLE logs (
  id BIGSERIAL PRIMARY KEY,
  incident_id BIGINT REFERENCES incidents(id) ON DELETE CASCADE,
  service_name VARCHAR(255),
  log_level VARCHAR(20),
  message TEXT,
  log_source VARCHAR(100), -- Loki, etc.
  trace_id VARCHAR(255),
  span_id VARCHAR(255),
  timestamp TIMESTAMP NOT NULL,
  log_data JSONB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_logs_incident ON logs(incident_id);
CREATE INDEX idx_logs_timestamp ON logs(timestamp DESC);
CREATE INDEX idx_logs_trace_id ON logs(trace_id);

-- Traces Table
CREATE TABLE traces (
  id BIGSERIAL PRIMARY KEY,
  incident_id BIGINT REFERENCES incidents(id) ON DELETE CASCADE,
  trace_id VARCHAR(255) UNIQUE NOT NULL,
  service_name VARCHAR(255),
  span_name VARCHAR(255),
  duration_ms BIGINT,
  status VARCHAR(50), -- OK, ERROR
  error_message TEXT,
  timestamp TIMESTAMP NOT NULL,
  trace_data JSONB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_traces_incident ON traces(incident_id);
CREATE INDEX idx_traces_trace_id ON traces(trace_id);
CREATE INDEX idx_traces_timestamp ON traces(timestamp DESC);

-- Deployments Table
CREATE TABLE deployments (
  id BIGSERIAL PRIMARY KEY,
  incident_id BIGINT REFERENCES incidents(id) ON DELETE CASCADE,
  service_name VARCHAR(255) NOT NULL,
  version VARCHAR(100),
  deployed_by VARCHAR(255),
  deployment_time TIMESTAMP NOT NULL,
  previous_version VARCHAR(100),
  rollback_available BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_deployments_incident ON deployments(incident_id);
CREATE INDEX idx_deployments_deployment_time ON deployments(deployment_time DESC);

-- AI Analyses Table
CREATE TABLE ai_analyses (
  id BIGSERIAL PRIMARY KEY,
  incident_id BIGINT REFERENCES incidents(id) ON DELETE CASCADE,
  analysis_status VARCHAR(50) NOT NULL DEFAULT 'pending', -- pending, running, completed, failed
  root_cause VARCHAR(500),
  confidence_score DOUBLE PRECISION,
  affected_services TEXT[],
  summary TEXT,
  findings JSONB,
  remediation_suggestions TEXT,
  analysis_data JSONB,
  started_at TIMESTAMP,
  completed_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_analyses_incident ON ai_analyses(incident_id);
CREATE INDEX idx_analyses_status ON ai_analyses(analysis_status);

-- Analysis Artifacts Table (for intermediate workflow results)
CREATE TABLE analysis_artifacts (
  id BIGSERIAL PRIMARY KEY,
  analysis_id BIGINT REFERENCES ai_analyses(id) ON DELETE CASCADE,
  artifact_type VARCHAR(100), -- logs_analysis, metrics_analysis, traces_analysis, deployment_analysis, correlation
  agent_name VARCHAR(100),
  findings TEXT,
  confidence DOUBLE PRECISION,
  raw_data JSONB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_artifacts_analysis ON analysis_artifacts(analysis_id);
CREATE INDEX idx_artifacts_type ON analysis_artifacts(artifact_type);

-- Runbooks Table
CREATE TABLE runbooks (
  id BIGSERIAL PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  content TEXT NOT NULL,
  service_name VARCHAR(255),
  tags TEXT[],
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT REFERENCES users(id)
);

CREATE INDEX idx_runbooks_service ON runbooks(service_name);

-- Embeddings Table (pgvector)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE embeddings (
  id BIGSERIAL PRIMARY KEY,
  entity_type VARCHAR(50) NOT NULL, -- incident, runbook, postmortem
  entity_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  embedding vector(1536),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_embeddings_entity ON embeddings(entity_type, entity_id);
CREATE INDEX idx_embeddings_vector ON embeddings USING ivfflat (embedding vector_cosine_ops);

-- Incident Timeline Table
CREATE TABLE incident_timelines (
  id BIGSERIAL PRIMARY KEY,
  incident_id BIGINT REFERENCES incidents(id) ON DELETE CASCADE,
  event_type VARCHAR(100), -- alert, deployment, metric_spike, trace_error, log_error
  event_source VARCHAR(100),
  description TEXT,
  severity VARCHAR(50),
  event_time TIMESTAMP NOT NULL,
  event_data JSONB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_timelines_incident ON incident_timelines(incident_id);
CREATE INDEX idx_timelines_event_time ON incident_timelines(event_time DESC);
