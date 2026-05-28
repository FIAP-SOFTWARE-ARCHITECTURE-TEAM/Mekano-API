# Codebase Structure

**Analysis Date:** 2025-07-15

## Directory Layout

```
mekano/                                     # Project root
├── pom.xml                                 # Maven build descriptor (Quarkus 3.36.0, Java 17)
├── mvnw / mvnw.cmd                         # Maven wrapper scripts
├── README.md                               # Project documentation
├── .mvn/                                   # Maven wrapper config
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── fiap/                   # Root application package
│   │   │           └── GreetingResource.java   # REST resource (GET /hello)
│   │   ├── resources/
│   │   │   └── application.properties      # Quarkus configuration (currently empty)
│   │   └── docker/
│   │       ├── Dockerfile.jvm              # JVM mode Docker image
│   │       ├── Dockerfile.legacy-jar       # Über-jar Docker image
│   │       ├── Dockerfile.native           # GraalVM native Docker image
│   │       └── Dockerfile.native-micro     # Minimal native Docker image
│   └── test/
│       └── java/
│           └── com/
│               └── fiap/                   # Test package (mirrors main)
│                   ├── GreetingResourceTest.java   # Unit/integration tests (@QuarkusTest)
│                   └── GreetingResourceIT.java     # Packaged-mode integration tests
├── target/                                 # Build output (git-ignored)
└── .planning/                              # Project planning documents
    └── codebase/                           # Codebase analysis docs
```

## Directory Purposes

**`src/main/java/com/fiap/`:**
- Purpose: All production Java source code
- Contains: JAX-RS resource classes, CDI beans, services, repositories (to be added)
- Key files: `GreetingResource.java`

**`src/main/resources/`:**
- Purpose: Runtime configuration and static resources
- Contains: `application.properties` (Quarkus config), future: `import.sql`, OpenAPI annotations config
- Key files: `src/main/resources/application.properties`

**`src/main/docker/`:**
- Purpose: Docker packaging strategies for different deployment modes
- Contains: Four Dockerfiles for JVM, legacy-jar, native, and native-micro packaging
- Generated: No — these are hand-curated Quarkus scaffold files

**`src/test/java/com/fiap/`:**
- Purpose: All test code, mirrors the main package structure
- Contains: `@QuarkusTest` annotated tests, integration tests using REST-Assured
- Key files: `GreetingResourceTest.java`, `GreetingResourceIT.java`

**`target/`:**
- Purpose: Maven build output
- Generated: Yes
- Committed: No (in `.gitignore`)
- JVM artifact: `target/quarkus-app/quarkus-run.jar`

## Key File Locations

**Entry Points:**
- `src/main/java/com/fiap/GreetingResource.java`: Only REST resource; `GET /hello`

**Configuration:**
- `src/main/resources/application.properties`: Quarkus application config (host, port, logging, datasource, etc.)
- `pom.xml`: Build dependencies, Quarkus version, plugin config

**Docker / Deployment:**
- `src/main/docker/Dockerfile.jvm`: Primary JVM container image definition

**Testing:**
- `src/test/java/com/fiap/GreetingResourceTest.java`: Quarkus-managed test, boots full application
- `src/test/java/com/fiap/GreetingResourceIT.java`: Runs same tests against packaged jar

## Naming Conventions

**Files:**
- Resource classes: `{Domain}Resource.java` — e.g., `GreetingResource.java`
- Test classes: `{ClassName}Test.java` for unit/Quarkus tests
- Integration test classes: `{ClassName}IT.java` (Maven Failsafe picks up `*IT.java`)

**Packages:**
- Root package: `com.fiap`
- New features should be added as sub-packages: `com.fiap.{domain}` (e.g., `com.fiap.product`, `com.fiap.order`)

**Classes (follow Java conventions):**
- PascalCase for class names
- camelCase for method and field names

## Where to Add New Code

**New REST Endpoint:**
- Implementation: `src/main/java/com/fiap/{domain}/{Domain}Resource.java`
- Test: `src/test/java/com/fiap/{domain}/{Domain}ResourceTest.java`

**New Service Layer:**
- Implementation: `src/main/java/com/fiap/{domain}/{Domain}Service.java`
- Annotate with `@ApplicationScoped` (CDI managed bean)
- Inject into resource with `@Inject`

**New Repository / Data Access:**
- Implementation: `src/main/java/com/fiap/{domain}/{Domain}Repository.java`
- Requires adding `quarkus-hibernate-orm-panache` or `quarkus-hibernate-reactive-panache` to `pom.xml`

**New DTOs / Models:**
- Location: `src/main/java/com/fiap/{domain}/dto/` or `src/main/java/com/fiap/{domain}/model/`

**Configuration Properties:**
- Add to: `src/main/resources/application.properties`
- Typed config class: `src/main/java/com/fiap/config/{Feature}Config.java` with `@ConfigMapping`

**New Dependency:**
- Add to `<dependencies>` section in `pom.xml`; use Quarkus BOM — no version needed for `io.quarkus:*` artifacts

## Special Directories

**`.mvn/`:**
- Purpose: Maven wrapper JAR and properties
- Generated: Partially (wrapper JAR downloaded on first use)
- Committed: Yes

**`.planning/`:**
- Purpose: GSD planning and codebase analysis documents
- Generated: No — manually maintained
- Committed: Yes

## Package Organization Pattern

The project currently has a **flat single-package** layout. As the project grows, adopt a **feature-based (vertical slice) package structure**:

```
com.fiap/
├── {domain}/                  # One sub-package per domain/feature
│   ├── {Domain}Resource.java  # REST layer
│   ├── {Domain}Service.java   # Business logic
│   ├── {Domain}Repository.java # Data access
│   ├── dto/                   # Request/response DTOs
│   └── model/                 # Domain entities
└── shared/                    # Cross-cutting utilities, exceptions, mappers
```

---

*Structure analysis: 2025-07-15*
