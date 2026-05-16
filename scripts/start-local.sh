#!/usr/bin/env bash
# Start InSeeDent locally (3 terminals worth of work in one guide)
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "=== 1. Postgres (Docker) ==="
if command -v docker &>/dev/null && docker info &>/dev/null 2>&1; then
  docker compose -f "$ROOT/docker-compose.db.yml" up -d
  echo "Postgres on localhost:5432"
else
  echo "Docker not running. Start Postgres yourself on port 5432 (db: inseedent, user/pass: inseedent)"
fi

echo ""
echo "=== 2. Backend (new terminal) ==="
echo "  cd $ROOT/backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo ""
echo "=== 3. AI service (new terminal) ==="
echo "  cd $ROOT/ai-service && chmod +x run.sh && ./run.sh"
echo ""
echo "=== 4. Frontend (new terminal) ==="
echo "  cd $ROOT/frontend && npm run dev"
echo ""
echo "Login: admin / admin123  →  http://localhost:5173"
