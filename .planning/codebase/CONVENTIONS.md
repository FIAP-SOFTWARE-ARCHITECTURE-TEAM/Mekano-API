# Coding Conventions

**Analysis Date:** 2025-01-31

> **Note:** This project is a Quarkus 3.36.0 scaffold with a single resource class. Conventions are based on the existing code and Quarkus/JAX-RS standards in use.

## Naming Patterns

**Packages:**
- Root package: `com.fiap`
- Pattern: lowercase, domain-reversed style
- Example: `com.fiap` (expand with sub-packages like `com.fiap.resource`, `com.fiap.service`, etc.)

**Classes:**
- PascalCase for all class names
- REST resource classes suffixed with `Resource`: `GreetingResource`
- Test classes suffixed with `Test`: `GreetingResourceTest`
- Integration test classes suffixed with `IT`: `GreetingResourceIT`

**Methods:**
- camelCase for all method names
- Handler/endpoint methods named descriptively after action: `hello()`
- Test methods prefixed with `test` + noun: `testHelloEndpoint()`

**Files:**
- One top-level class per file
- Filename matches class name exactly: `GreetingResource.java`

## Code Style

**Formatting:**
- No formatter config detected (no `.editorconfig`, Checkstyle, or Spotless)
- Standard Java indentation (4 spaces based on existing source)
- Opening braces on same line as declaration (K&R style)
- Single blank line between methods

**Linting:**
- No static analysis tool configured (no Checkstyle, PMD, or SpotBugs in `pom.xml`)

## Annotations

**JAX-RS / Jakarta REST (used on resource classes):**
```java
@Path("/hello")          // class-level URI mapping
public class GreetingResource {

    @GET                                    // HTTP method
    @Produces(MediaType.TEXT_PLAIN)         // response content type
    public String hello() { ... }
}
```

**Quarkus CDI:**
- `@ApplicationScoped`, `@RequestScoped`, `@Singleton` — use for injectable beans (not yet present but expected pattern)
- `@Inject` — field injection for dependencies

**Testing annotations** (see TESTING.md for full details):
- `@QuarkusTest` — full application context test
- `@QuarkusIntegrationTest` — packaged/native binary test

**OpenAPI:**
- `quarkus-smallrye-openapi` dependency is present; use `@Operation`, `@APIResponse`, `@Schema` from `org.eclipse.microprofile.openapi.annotations` to document endpoints

## Import Organization

**Order (observed pattern):**
1. `jakarta.*` — Jakarta EE / JAX-RS APIs
2. `io.quarkus.*` — Quarkus framework classes
3. `org.junit.*` — Test framework
4. `static` imports last (e.g., `static io.restassured.RestAssured.given`)

**Example from `GreetingResource.java`:**
```java
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
```

## REST Resource Design

**Pattern:**
- Plain Java class (no explicit `@ApplicationScoped` required — Quarkus infers it)
- `@Path` at class level defines base URI
- HTTP verb annotations (`@GET`, `@POST`, `@PUT`, `@DELETE`) at method level
- `@Produces` / `@Consumes` declare media types

**Current example (`src/main/java/com/fiap/GreetingResource.java`):**
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

**Recommended expansion pattern:**
- Add `@Consumes(MediaType.APPLICATION_JSON)` and `@Produces(MediaType.APPLICATION_JSON)` for JSON endpoints
- Return `Response` objects for full HTTP control, or typed POJOs (Quarkus serializes automatically with Jackson/JSON-B)

## Error Handling

**Current state:** No error handling implemented (scaffold only).

**Quarkus recommended pattern:**
- Use `jakarta.ws.rs.WebApplicationException` for HTTP-level errors
- Create `@Provider` + `ExceptionMapper<T>` classes for custom error responses:
```java
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
    @Override
    public Response toResponse(NotFoundException e) {
        return Response.status(Response.Status.NOT_FOUND)
                       .entity(new ErrorDto(e.getMessage()))
                       .build();
    }
}
```
- Place mappers in `src/main/java/com/fiap/exception/` (suggested)

## Logging

**Framework:** JBoss LogManager (configured in `pom.xml` surefire/failsafe via `java.util.logging.manager=org.jboss.logmanager.LogManager`)

**Quarkus pattern:**
```java
import org.jboss.logging.Logger;

public class MyResource {
    private static final Logger LOG = Logger.getLogger(MyResource.class);
    // ...
    LOG.info("Processing request");
}
```

## Configuration

**Config file:** `src/main/resources/application.properties` (currently empty — scaffold default)

**Pattern:** Use MicroProfile Config via `@ConfigProperty`:
```java
@ConfigProperty(name = "greeting.message", defaultValue = "Hello")
String message;
```

## Module Design

**Exports:** No module-info.java (plain Maven project, not JPMS modular)

**Suggested package structure as project grows:**
```
com.fiap/
├── resource/     # JAX-RS resources (REST layer)
├── service/      # Business logic
├── repository/   # Data access
├── domain/       # Domain entities / POJOs
├── dto/          # Data Transfer Objects
└── exception/    # Exception mappers
```

---

*Convention analysis: 2025-01-31*
