# Mekano API — Makefile
# Atalhos para comandos Maven multi-módulo + Docker

.PHONY: dev test test-domain test-app test-infra test-rest compile clean build native up down reset-db logs swagger

# ─────────────── Docker ───────────────

up:
	docker-compose up -d

down:
	docker-compose down

reset-db:
	docker-compose down -v
	docker-compose up -d

logs:
	docker-compose logs -f postgres

# ─────────────── Dev ───────────────

dev: up
	./mvnw quarkus:dev -pl mekano-rest -am

dev-no-tests: up
	./mvnw quarkus:dev -pl mekano-rest -am -Dquarkus.test.continuous-testing=disabled

# ─────────────── Build ───────────────

compile:
	./mvnw compile -pl mekano-rest -am

clean:
	./mvnw clean

build:
	./mvnw package -pl mekano-rest -am -DskipTests

native:
	./mvnw package -Dnative -pl mekano-rest -am -DskipTests

# ─────────────── Testes ───────────────

test:
	./mvnw clean verify -pl mekano-rest -am

test-domain:
	./mvnw test -pl mekano-domain

test-app:
	./mvnw test -pl mekano-application -am

test-infra:
	./mvnw test -pl mekano-infrastructure -am

test-rest:
	./mvnw test -pl mekano-rest -am

# Rodar um teste específico: make test-one TEST=ServicoResourceTest
test-one:
	./mvnw test -pl mekano-rest -am -Dtest=$(TEST) -Dsurefire.failIfNoSpecifiedTests=false

# ─────────────── Utilitários ───────────────

swagger:
	open http://127.0.0.1:8080/q/swagger-ui

health:
	curl -s http://127.0.0.1:8080/q/health | python3 -m json.tool
