# Mekano API — Makefile
# Atalhos para Maven multi-módulo + Docker (Windows & Linux)

# OS helpers — $(OS) é "Windows_NT" no Windows, indefinido no Linux/macOS
# O shell `uname -s` retorna "MINGW64_NT..." no Git Bash, então
# usamos a env-var `OS` que o cmd.exe sempre define.

ifeq ($(OS),Windows_NT)
  OPEN_CMD     := start
  CURL         := curl.exe
  PYTHON       := python
  MAVEN        := mvnw
  DOCKER_COMP  := docker-compose
else
  OPEN_CMD     := xdg-open
  CURL         := curl
  PYTHON       := python3
  MAVEN        := ./mvnw
  DOCKER_COMP  := docker-compose
endif

# ─────────────── Targets ───────────────

.PHONY: dev test test-domain test-app test-infra test-rest test-one
.PHONY: compile clean build native
.PHONY: up down reset-db logs swagger health

# ── Docker ──

up:
	$(DOCKER_COMP) up -d

down:
	$(DOCKER_COMP) down

reset-db:
	$(DOCKER_COMP) down -v
	$(DOCKER_COMP) up -d

logs:
	$(DOCKER_COMP) logs -f postgres

# ── Dev ──

dev: up
	$(MAVEN) quarkus:dev -pl mekano-rest -am

dev-no-tests: up
	$(MAVEN) quarkus:dev -pl mekano-rest -am -Dquarkus.test.continuous-testing=disabled

# ── Build ──

compile:
	$(MAVEN) compile -pl mekano-rest -am

clean:
	$(MAVEN) clean

build:
	$(MAVEN) -B -ntp verify -pl mekano-rest -am

native:
	$(MAVEN) package -Dnative -pl mekano-rest -am -DskipTests

# ── Testes ──

test:
	$(MAVEN) clean verify -pl mekano-rest -am

test-domain:
	$(MAVEN) test -pl mekano-domain

test-app:
	$(MAVEN) test -pl mekano-application -am

test-infra:
	$(MAVEN) test -pl mekano-infrastructure -am

test-rest:
	$(MAVEN) test -pl mekano-rest -am

test-one:
	$(MAVEN) test -pl mekano-rest -am -Dtest=$(TEST) -Dsurefire.failIfNoSpecifiedTests=false

# ── Utilitários ──

swagger:
	$(OPEN_CMD) http://127.0.0.1:8080/q/swagger-ui

health:
	$(CURL) -s http://127.0.0.1:8080/q/health | $(PYTHON) -m json.tool
