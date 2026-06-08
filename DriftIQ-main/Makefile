.PHONY: all setup keys up down logs test clean

# DriftIQ — Docker deployment helpers
# Usage: make setup && make up

COMPOSE = docker compose
BACKEND = ./backend

## Generate RSA keys for JWT (run once before first deploy)
keys:
	@echo "Generating RSA keys in ./keys/..."
	@mkdir -p keys
	@openssl genrsa -out keys/private.pem 2048
	@openssl rsa -in keys/private.pem -pubout -out keys/public.pem
	@echo "Keys generated:"
	@ls -la keys/

## Copy example env file (edit before running)
env:
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo ".env created from .env.example — edit it before deploying!"; \
	else \
		echo ".env already exists"; \
	fi

## Full setup: env + keys
setup: env keys
	@echo ""
	@echo "✓ Setup complete. Edit .env with your credentials, then run: make up"

## Start all services (builds, migrates, starts)
up:
	$(COMPOSE) up --build -d
	@echo ""
	@echo "✓ DriftIQ started. API: http://localhost:8000"
	@echo "  Logs: make logs"

## Start without rebuilding
start:
	$(COMPOSE) up -d

## Stop all services
down:
	$(COMPOSE) down

## Stop and remove volumes (WARNING: deletes all data)
destroy:
	$(COMPOSE) down -v --remove-orphans
	@echo "All services stopped and volumes removed."

## View logs (all services)
logs:
	$(COMPOSE) logs -f --tail=100

## View logs for specific service (e.g. make logs-api)
logs-%:
	$(COMPOSE) logs -f --tail=200 $*

## Run database migrations manually
migrate:
	$(COMPOSE) run --rm migrate

## Run tests (unit only — no Docker required)
test:
	cd $(BACKEND) && python -m pytest tests/unit/ -v --tb=short

## Run all tests including integration
test-all:
	cd $(BACKEND) && python -m pytest tests/ -v --tb=short

## Check service health
health:
	@echo "Checking service health..."
	@curl -sf http://localhost:8000/health | python -m json.tool || echo "API not responding"
	@$(COMPOSE) ps

## Rebuild and restart specific service (e.g. make rebuild-api)
rebuild-%:
	$(COMPOSE) up --build --force-recreate -d $*

## Shell into API container
shell:
	$(COMPOSE) exec api bash

## Clean Python artifacts
clean:
	find $(BACKEND) -type f -name "*.pyc" -delete
	find $(BACKEND) -type d -name "__pycache__" -exec rm -rf {} +
	find $(BACKEND) -type d -name ".pytest_cache" -exec rm -rf {} +
	rm -f $(BACKEND)/test.db
