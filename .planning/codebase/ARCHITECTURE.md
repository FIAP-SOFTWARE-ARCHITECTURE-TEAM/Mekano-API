# Architecture

**Analysis Date:** 2025-07-15

## Pattern Overview

**Overall:** REST API — Quarkus JAX-RS (Jakarta REST) single-layer scaffold

**Key Characteristics:**
- Minimal bootstrap: one REST resource class, no service or repository layers yet
- CDI (Contexts and Dependency Injection) managed via `quarkus-arc`
- Standard JAX-RS annotation-driven routing (`@Path`, `@GET`, `@Produces`)
- OpenAPI/Swagger UI built in via `quarkus-smallrye-openapi`
- No persistence layer present (no ORM, no database dependency declared)

## Layers

**REST Resource Layer:**
- Purpose: Handles incoming HTTP requests, maps URLs to Java methods, produces responses
- Location: `src/main/java/com/fiap/`
- Contains: JAX-RS resource classes annotated with `@Path`
- Depends on: (future) service layer, CDI beans
- Used by: HTTP clients, REST-Assured tests

> **Note:** This project is a Quarkus scaffold. Only the resource layer exists today. Service and repository layers are not yet present and should be added as the project grows.

## Data Flow

**HTTP Request Flow:**

1. Client sends HTTP request to `http://localhost:8080/{path}`
2. Quarkus Vert.x HTTP server receives the request
3. JAX-RS routing dispatches to the matching `@Path` resource method
4. Resource method executes and returns a response body
5. Quarkus serializes response (plain text, JSON, etc.) and sends HTTP response

**Current endpoint:**
- `GET /hello` → `GreetingResource#hello()` → returns `"Hello from Quarkus REST"` as `text/plain`

**State Management:**
- Stateless: no shared mutable state; each request handled independently

## Key Abstractions

**JAX-RS Resource (`@Path`):**
- Purpose: Represents a REST endpoint bound to a URL path
- Examples: `src/main/java/com/fiap/GreetingResource.java`
- Pattern: Annotate class with `@Path("/route")`, annotate methods with HTTP verb (`@GET`, `@POST`, etc.) and `@Produces`/`@Consumes`

```java
@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }
}
```

## Entry Points

**HTTP Server:**
- Location: Managed internally by Quarkus (Vert.x embedded)
- Triggers: Application startup (`./mvnw quarkus:dev` or `java -jar target/quarkus-app/quarkus-run.jar`)
- Default port: `8080`
- Responsibilities: Accept TCP connections, route to JAX-RS resources

**Application Bootstrap:**
- Quarkus uses build-time augmentation; no explicit `main()` class needed
- Configuration loaded from `src/main/resources/application.properties`

**OpenAPI / Dev UI:**
- OpenAPI spec: `http://localhost:8080/q/openapi`
- Swagger UI: `http://localhost:8080/q/swagger-ui` (dev mode)
- Dev UI: `http://localhost:8080/q/dev/` (dev mode only)

## Error Handling

**Strategy:** Default Quarkus JAX-RS exception mapping

**Patterns:**
- Unhandled exceptions produce HTTP 500 with Quarkus default error response
- JAX-RS `WebApplicationException` can be thrown explicitly to return specific status codes
- Custom `ExceptionMapper<T>` classes can be added to `com.fiap` package to handle domain errors

## Cross-Cutting Concerns

**Logging:** JBoss LogManager (`org.jboss.logmanager.LogManager`); configured via `application.properties` or JVM system property `-Djava.util.logging.manager`

**Validation:** Not yet configured (no `quarkus-hibernate-validator` dependency)

**Authentication:** Not yet configured (no security extension in `pom.xml`)

**Serialization:** Plain text (`MediaType.TEXT_PLAIN`) currently; JSON support requires adding `quarkus-rest-jackson` or `quarkus-rest-jsonb`

## Build & Packaging

**Dev mode:** `./mvnw quarkus:dev` — live reload enabled

**JVM mode:** `./mvnw package` → `java -jar target/quarkus-app/quarkus-run.jar`

**Über-jar:** `./mvnw package -Dquarkus.package.jar.type=uber-jar`

**Native binary:** `./mvnw package -Dnative` (requires GraalVM or `-Dquarkus.native.container-build=true`)

**Docker images available:**
- `src/main/docker/Dockerfile.jvm` — JVM mode, base: `ubi9/openjdk-17-runtime:1.24`
- `src/main/docker/Dockerfile.legacy-jar` — Über-jar packaging
- `src/main/docker/Dockerfile.native` — GraalVM native binary
- `src/main/docker/Dockerfile.native-micro` — Minimal native image

---

*Architecture analysis: 2025-07-15*
