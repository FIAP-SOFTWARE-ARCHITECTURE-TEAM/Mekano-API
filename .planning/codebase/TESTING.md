# Testing Patterns

**Analysis Date:** 2025-01-31

> **Note:** This project is a Quarkus 3.36.0 scaffold. Two test files exist representing the standard Quarkus test split: unit/integration with `@QuarkusTest` and native/packaged integration with `@QuarkusIntegrationTest`.

## Test Framework

**Runner:**
- JUnit 5 (Jupiter) — via `quarkus-junit` dependency in `pom.xml`
- Quarkus Test framework — `@QuarkusTest` starts the full app in test mode
- Config: Maven Surefire Plugin 3.5.4 (`pom.xml` lines 74–83)

**HTTP Testing Library:**
- REST-assured (`io.rest-assured:rest-assured`, test scope)

**Run Commands:**
```bash
./mvnw test                        # Run all unit tests (@QuarkusTest)
./mvnw verify -DskipITs=false      # Run unit + integration tests
./mvnw verify -Pnative             # Run native integration tests (@QuarkusIntegrationTest)
./mvnw test -Dtest=GreetingResourceTest   # Run a specific test class
```

## Test File Organization

**Location:**
- Test sources: `src/test/java/`
- Mirrors main source package structure: `src/test/java/com/fiap/`

**Naming:**
- Unit/integration tests (JVM mode): `*Test.java` — picked up by Surefire
- Native/packaged integration tests: `*IT.java` — picked up by Failsafe

**Current test files:**
- `src/test/java/com/fiap/GreetingResourceTest.java` — `@QuarkusTest`, JVM mode
- `src/test/java/com/fiap/GreetingResourceIT.java` — `@QuarkusIntegrationTest`, packaged mode

## Test Structure

**Suite Organization:**
```java
package com.fiap;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest                          // Boots full Quarkus app
class GreetingResourceTest {          // Package-private class (no public modifier)

    @Test
    void testHelloEndpoint() {        // camelCase, void return, no public modifier
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(is("Hello from Quarkus REST"));
    }
}
```

**Key structural rules observed:**
- Test class is package-private (no `public` modifier)
- Test methods are package-private (no `public` modifier) — JUnit 5 style
- Method names use camelCase with `test` prefix: `testHelloEndpoint()`
- No `@BeforeEach` / `@AfterEach` setup in existing tests (scaffold only)

## Test Types

**@QuarkusTest (JVM Integration Tests):**
- Boots the full Quarkus application context
- Tests run against a real HTTP server (random or fixed port)
- Use REST-assured to make real HTTP requests
- File pattern: `*Test.java`
- Example: `src/test/java/com/fiap/GreetingResourceTest.java`

**@QuarkusIntegrationTest (Native/Packaged Integration Tests):**
- Runs against the packaged JAR or native binary
- Extends the `@QuarkusTest` class to reuse all test methods
- Only runs when `-Pnative` profile is active (`skipITs=false`)
- File pattern: `*IT.java`
- Example: `src/test/java/com/fiap/GreetingResourceIT.java`

```java
@QuarkusIntegrationTest
class GreetingResourceIT extends GreetingResourceTest {
    // Execute the same tests but in packaged mode.
}
```

## HTTP Testing with REST-Assured

**Pattern — full request/response chain:**
```java
given()
    .contentType(ContentType.JSON)      // request content type
    .body(requestPayload)               // request body (POJO or String)
.when()
    .post("/endpoint")                  // HTTP method + path
.then()
    .statusCode(201)                    // assert status
    .body("field", equalTo("value"));   // assert response body with Hamcrest
```

**Common matchers (Hamcrest):**
```java
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
```

**Static imports used:**
```java
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
```

## Mocking

**Framework:** Not yet configured. No Mockito or QuarkusMock usage in current tests.

**Quarkus recommended mock pattern using `@InjectMock`:**
```java
import io.quarkus.test.InjectMock;
import org.mockito.Mockito;

@QuarkusTest
class MyResourceTest {

    @InjectMock
    MyService myService;

    @Test
    void testWithMock() {
        Mockito.when(myService.doSomething()).thenReturn("mocked");
        given().when().get("/my-endpoint").then().statusCode(200);
    }
}
```

**To add Mockito support, add to `pom.xml`:**
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit5-mockito</artifactId>
    <scope>test</scope>
</dependency>
```

## Fixtures and Factories

**Test Data:** Not yet defined (scaffold only).

**Recommended pattern for request bodies:**
```java
// Inline record/POJO as test fixture
record GreetingRequest(String name) {}

given()
    .contentType(ContentType.JSON)
    .body(new GreetingRequest("World"))
.when().post("/hello")
.then().statusCode(200);
```

**Location for shared test utilities:**
- `src/test/java/com/fiap/util/` — helper classes, builders, factories

## Coverage

**Requirements:** No coverage enforcement configured (no JaCoCo plugin in `pom.xml`).

**To add JaCoCo coverage reporting, add to `pom.xml`:**
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

**View coverage (after JaCoCo added):**
```bash
./mvnw test
# Report at: target/site/jacoco/index.html
```

## CI/CD

**No CI/CD pipeline detected** (no `.github/workflows/`, `.gitlab-ci.yml`, or similar files found).

## Common Patterns

**Async Testing (Quarkus reactive):**
```java
@Test
void testAsync() {
    given()
        .when().get("/reactive-endpoint")
        .then()
        .statusCode(200);
    // REST-assured handles async transparently for HTTP
}
```

**Error/Failure Testing:**
```java
@Test
void testNotFound() {
    given()
        .when().get("/nonexistent")
        .then()
        .statusCode(404);
}
```

**JSON body assertion:**
```java
@Test
void testJsonBody() {
    given()
        .when().get("/items/1")
        .then()
        .statusCode(200)
        .body("id", equalTo(1))
        .body("name", notNullValue());
}
```

## Test Execution Notes

- Surefire runs `*Test.java` during `mvn test` phase
- Failsafe runs `*IT.java` during `mvn verify` phase
- `skipITs=true` is the default (see `pom.xml` line 17) — IT tests are opt-in
- The `@{argLine}` placeholder in Surefire config reserves space for JaCoCo agent when added

---

*Testing analysis: 2025-01-31*
