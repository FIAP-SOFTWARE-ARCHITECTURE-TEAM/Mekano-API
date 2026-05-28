# Domain Pitfalls

**Domain:** Clean Architecture multi-module Maven API — Quarkus 3.36.0 + Java 17
**Researched:** 2025-05-27
**Confidence:** HIGH (all critical findings verified against official Quarkus docs, MapStruct reference, and SmallRye JWT guide)

---

## Critical Pitfalls

Mistakes that cause silent failures, build breakage, or full rewrites.

---

### Pitfall 1: `quarkus-maven-plugin` in the Wrong POM

**What goes wrong:**  
When converting from single-module to multi-module Maven, developers copy `quarkus-maven-plugin` (with `<extensions>true</extensions>`) into the parent POM. This causes build failures because the plugin only works correctly in the deployable module — the one that actually packages and runs the application.

**Why it happens:**  
It feels natural to centralize plugin config in the parent. But `<packaging>quarkus</packaging>` + the plugin define *what gets built*, not configuration to share.

**Consequences:**
- `mvn quarkus:dev` fails or runs from the wrong module
- Native build profile (`-Pnative`) silently applies to all modules
- The BOM import + plugin version sync breaks in sub-modules

**Prevention:**
- Parent POM: `<packaging>pom</packaging>`, **no** `quarkus-maven-plugin`
- Only the deployable module (e.g., `adapter` or `app`): `<packaging>quarkus</packaging>` + plugin with `<extensions>true</extensions>`
- `dependencyManagement` with BOM import belongs in the **parent**; the plugin definition belongs **only in the deployable module**

```xml
<!-- ✅ PARENT pom.xml -->
<packaging>pom</packaging>
<modules>
  <module>domain</module>
  <module>application</module>
  <module>infrastructure</module>
  <module>adapter</module>
</modules>
<!-- quarkus-maven-plugin NOT here -->

<!-- ✅ adapter/pom.xml (the deployable module) -->
<packaging>quarkus</packaging>
<build>
  <plugins>
    <plugin>
      <groupId>io.quarkus.platform</groupId>
      <artifactId>quarkus-maven-plugin</artifactId>
      <version>${quarkus.platform.version}</version>
      <extensions>true</extensions>
      <executions>
        <execution>
          <goals>
            <goal>build</goal>
            <goal>generate-code</goal>
            <goal>generate-code-tests</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

**Source:** https://quarkus.io/guides/maven-tooling — *"Working with multi-module projects"*

---

### Pitfall 2: CDI Beans in Sub-modules Not Discovered

**What goes wrong:**  
Classes annotated with `@ApplicationScoped`, `@Singleton`, etc. in `domain`, `application`, or `infrastructure` modules are invisible to Quarkus CDI. Injection points fail at runtime with `UnsatisfiedResolutionException` or are silently ignored.

**Why it happens:**  
Quarkus builds a single synthetic CDI bean archive using Jandex indexing. By default, only the **main application module** (the one with `quarkus-maven-plugin`) is indexed automatically. Other modules in a multi-module project are not indexed unless explicitly configured.

**Official statement (Quarkus docs):**  
> "By default, Quarkus will not discover CDI beans inside another module."

**Consequences:**
- `@Inject UserRepositoryImpl` fails with unsatisfied dependency
- No build error — failure is at startup or first injection
- Can waste hours debugging if the class "looks correct"

**Prevention — Option A (Recommended): `jandex-maven-plugin` per sub-module**

Add to each non-main module's `pom.xml`:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>io.smallrye</groupId>
      <artifactId>jandex-maven-plugin</artifactId>
      <version>3.5.3</version>
      <executions>
        <execution>
          <id>make-index</id>
          <goals>
            <goal>jandex</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

This generates `META-INF/jandex.idx` inside each module's JAR — Quarkus picks it up automatically.

**Prevention — Option B: `beans.xml` in sub-modules**

Place `src/main/resources/META-INF/beans.xml` (can be empty) in each sub-module. Quarkus treats any archive containing `beans.xml` as a bean archive. Note: Quarkus uses `annotated` discovery mode by default — classes still need a bean-defining annotation (`@ApplicationScoped`, `@Singleton`, etc.) to be discovered.

**Prevention — Option C: `quarkus.index-dependency` in application.properties**

```properties
quarkus.index-dependency.domain.group-id=com.fiap
quarkus.index-dependency.domain.artifact-id=mekano-domain
```

This forces Quarkus to index a specific dependency even if it has no `beans.xml` or Jandex index.

**Recommendation for this project:** Use `jandex-maven-plugin` in `domain`, `application`, and `infrastructure` modules. The `adapter` module (deployable, has `quarkus-maven-plugin`) is indexed automatically.

**Source:** https://quarkus.io/guides/maven-tooling + https://quarkus.io/guides/cdi-reference

---

### Pitfall 3: MapStruct + Lombok Annotation Processor — Missing Binding Artifact

**What goes wrong:**  
MapStruct-generated mapper implementations fail to see Lombok-generated getters/setters/constructors. The result: mappers produce objects with all `null` fields, or the build fails with "No property named 'X' exists in source/target".

**Why it happens:**  
Lombok modifies the AST of compiled classes (adds getters/setters). MapStruct reads the compiled class structure to generate mapping code. Since **Lombok 1.18.16**, a breaking change requires a coordination artifact (`lombok-mapstruct-binding`) to ensure Lombok runs before MapStruct reads the class structure.

**Consequences:**
- Silent `null`s in mapped objects (worst case — passes compilation, fails at runtime)
- Build errors that look like MapStruct can't find properties
- Mappers that work in one module but not another depending on compilation order

**Prevention:**

In any module that uses both Lombok AND MapStruct, configure `maven-compiler-plugin` with **all three** annotation processor paths:

```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.15.0</version>
  <configuration>
    <parameters>true</parameters>
    <annotationProcessorPaths>
      <!-- 1. MapStruct processor -->
      <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>${mapstruct.version}</version>
      </path>
      <!-- 2. Lombok -->
      <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
      </path>
      <!-- 3. REQUIRED for Lombok 1.18.16+ — coordinates execution order -->
      <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok-mapstruct-binding</artifactId>
        <version>0.2.0</version>
      </path>
    </annotationProcessorPaths>
  </configuration>
</plugin>
```

**Key rules:**
1. `lombok-mapstruct-binding` **must be present** if Lombok ≥ 1.18.16 (current Lombok is 1.18.x series)
2. The `lombok-mapstruct-binding` artifact handles ordering — it ensures Lombok enhances classes before MapStruct reads them
3. Lombok dependency in `<dependencies>` must be `<scope>provided</scope>` (not `compile`) to avoid classpath pollution
4. `mapstruct` (the API JAR, not processor) goes in `<dependencies>` as a compile dependency

**Architecture note for this project:**  
- `infrastructure` module: needs Lombok + MapStruct (JPA entity ↔ domain entity mapping)
- `adapter` module: needs Lombok + MapStruct (domain entity ↔ DTO mapping)
- `domain` module: needs only Lombok (pure POJOs/Value Objects)
- `application` module: no MapStruct needed (orchestration only)

**Source:** https://mapstruct.org/documentation/stable/reference/html/ — *Section 14.2 Lombok*  
https://mapstruct.org/faq/ — *"How to use MapStruct together with Project Lombok?"*

---

### Pitfall 4: Jackson Incompatibility Between `quarkus-rest` and `quarkus-resteasy`

**What goes wrong:**  
Mixing RESTEasy Classic and Quarkus REST (RESTEasy Reactive) extensions causes classpath conflicts. Annotations from `org.jboss.resteasy.annotations.*` silently fail or throw at runtime. `@Context HttpServletRequest` injection compiles but throws at runtime.

**Why it happens:**  
`quarkus-rest` (reactive, this project's choice) and `quarkus-resteasy` (classic, blocking) are **mutually exclusive**. Each has its own Jackson extension. Common mistake: adding `quarkus-resteasy-jackson` when already using `quarkus-rest`.

**Correct pairings:**

| Stack | JSON Extension | Notes |
|-------|---------------|-------|
| `quarkus-rest` (reactive) | `quarkus-rest-jackson` | This project |
| `quarkus-resteasy` (classic) | `quarkus-resteasy-jackson` | Do NOT mix |

**Consequences of mixing:**
- Duplicate provider registration
- Serialization silently picks wrong provider
- `ClassCastException` or `MessageBodyWriter not found` at runtime

**RESTEasy annotations NOT supported in `quarkus-rest`:**
- `org.jboss.resteasy.annotations.jaxrs.*` → use `org.jboss.resteasy.reactive.*` equivalents
- `@Context HttpServletRequest` → won't work (not servlet-based); inject via `@Context UriInfo`, `@Context SecurityContext`, etc.
- `@Context ServletContext` → not available

**Custom `ObjectMapper` in `quarkus-rest`:**  
Do NOT replace the `ObjectMapper` bean directly — implement `ObjectMapperCustomizer`:

```java
@Singleton
public class CustomObjectMapperConfig implements ObjectMapperCustomizer {
    @Override
    public void customize(ObjectMapper mapper) {
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new JavaTimeModule());
    }
}
```

**Service Loader providers**: `quarkus-rest` does NOT support Java's ServiceLoader for provider discovery. Providers in dependency JARs must be explicitly registered via build items — for application code, use standard annotations (`@Provider`, `@ApplicationScoped`).

**Source:** https://quarkus.io/guides/rest-migration + https://quarkus.io/guides/rest

---

## Moderate Pitfalls

---

### Pitfall 5: Flyway Migration Naming and Location Config

**What goes wrong:**  
Migrations are not executed (silently skipped) or Flyway throws `FlywayException: Found non-empty schema(s) ... without schema history table`.

**Common mistakes:**

**A. Wrong file naming convention**
```
❌ v1_create_users.sql        (lowercase 'v')
❌ V1_create_users.sql        (single underscore)  
❌ V1.create_users.sql        (dot instead of double underscore)
✅ V1__create_users.sql       (capital V + double underscore)
✅ V1.0__create_users.sql     (versioned with dot notation)
```

The Flyway naming pattern is: `{prefix}{version}__{description}{suffix}`  
Default prefix: `V`, default separator: `__` (double underscore), default suffix: `.sql`

**B. Default location not matching module structure**  
Quarkus default location: `classpath:db/migration`  
Translates to: `src/main/resources/db/migration/`

If migrations are in the `infrastructure` module (recommended for this project), the JAR's classpath includes that path. But if Flyway config is missing, it defaults to looking only in `db/migration` at the classpath root.

```properties
# ✅ In application.properties (adapter/main module)
quarkus.flyway.migrate-at-start=true
quarkus.flyway.locations=db/migration

# For multiple locations across modules:
quarkus.flyway.locations=classpath:db/migration,filesystem:./extra-migrations
```

**C. Forgetting `migrate-at-start`**  
Flyway does NOT auto-migrate by default in Quarkus. Must set:
```properties
quarkus.flyway.migrate-at-start=true
```

Without it, migrations only run if you inject and call `flyway.migrate()` manually.

**D. Schema validation failures after changing applied migrations**  
Once a migration is applied and its checksum is stored in `flyway_schema_history`, **never modify that file**. If you do, Flyway throws `FlywayValidateException`. Fix with:
```properties
quarkus.flyway.validate-migration-naming=true   # validate naming at startup
quarkus.flyway.out-of-order=false               # default, strict ordering
```

**Source:** https://quarkus.io/guides/flyway

---

### Pitfall 6: SmallRye JWT — Common Configuration Mistakes

**What goes wrong:**  
JWT verification fails with HTTP 401 Unauthorized. The error is often unhelpful — it won't tell you whether the issuer didn't match or the key was wrong.

**Common mistakes:**

**A. Wrong public key property name**
```properties
❌ quarkus.smallrye-jwt.public-key.location=publicKey.pem   # wrong namespace
✅ mp.jwt.verify.publickey.location=publicKey.pem            # correct (MicroProfile spec)
```

**B. Key file not on classpath**  
Place the key in `src/main/resources/` (root, not subdirectory) or reference the subdirectory explicitly:
```properties
mp.jwt.verify.publickey.location=META-INF/resources/publicKey.pem
# or simply:
mp.jwt.verify.publickey.location=publicKey.pem   # looks in classpath root
```

**C. Wrong key format**  
Quarkus/SmallRye JWT expects PKCS#8 PEM format:
```
-----BEGIN PUBLIC KEY-----
...base64...
-----END PUBLIC KEY-----
```
NOT the legacy RSA format (`-----BEGIN RSA PUBLIC KEY-----`). Generate with:
```bash
openssl genrsa -out privateKey.pem 2048
openssl pkcs8 -topk8 -nocrypt -inform pem -in privateKey.pem -outform pem -out privateKey_pkcs8.pem
openssl rsa -pubout -in privateKey.pem -out publicKey.pem
```

**D. Issuer mismatch**  
The `iss` claim in the JWT token must **exactly match** `mp.jwt.verify.issuer`. Trailing slashes, HTTP vs HTTPS, or dev vs prod URLs cause silent 401s.
```properties
# Token must have: "iss": "https://example.com/issuer"
mp.jwt.verify.issuer=https://example.com/issuer
```

**E. Native build: key file not included**  
In native builds, classpath resources are not automatically included. Must add:
```properties
quarkus.native.resources.includes=publicKey.pem
```

**F. Blocking authentication issue**  
SmallRye JWT authentication is non-blocking by default in Quarkus REST (reactive). If your security code blocks (e.g., database calls in a `@RolesAllowed` check), set:
```properties
quarkus.smallrye-jwt.blocking-authentication=true
```

**Source:** https://quarkus.io/guides/security-jwt

---

### Pitfall 7: Panache Entity Inheritance and Module Separation

**What goes wrong:**  
Domain entities extended by JPA entities (or `PanacheEntity`/`PanacheEntityBase`) create unwanted coupling between the `domain` module and JPA/Hibernate dependencies.

**What NOT to do:**
```java
// ❌ WRONG: domain entity inheriting from PanacheEntity
// This puts JPA annotations in the domain layer
package com.fiap.domain.entity;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
@Entity  // ← JPA in domain = layering violation
public class User extends PanacheEntity { ... }
```

**Correct pattern for Clean Architecture + Panache:**
```java
// ✅ domain module: pure POJO, no JPA
package com.fiap.domain.entity;
@Value // Lombok
public class User {
    Long id;
    String name;
    String email;
}

// ✅ infrastructure module: JPA entity, separate class
@Entity
@Table(name = "users")
public class UserEntity extends PanacheEntity {
    public String name;
    public String email;
}

// ✅ infrastructure module: MapStruct mapper
@Mapper(componentModel = "cdi")
public interface UserEntityMapper {
    UserEntity toEntity(User user);
    User toDomain(UserEntity entity);
}
```

**Pitfall with `@MappedSuperclass`:**  
If you attempt to put common fields in a shared base class across modules, the base class must be in the same module as the JPA entities (infrastructure). Putting `@MappedSuperclass` classes in the `domain` module pulls in JPA API as a domain dependency.

**Inheritance and Jandex:**  
If a `PanacheEntity` subclass is in a module without a Jandex index, Hibernate ORM will not discover the entity and will not create the table. Symptom: no table created, no error — just missing functionality.

**Source:** https://quarkus.io/guides/hibernate-orm-panache

---

## Minor Pitfalls

---

### Pitfall 8: Native Compilation — Reflection and Resource Registration

**What goes wrong:**  
Application works in JVM mode but throws `ClassNotFoundException`, `NullPointerException`, or `JsonProcessingException` in native mode.

**Root cause:**  
GraalVM native image uses closed-world assumption — classes accessed via reflection, dynamic proxies, or classpath resources must be explicitly registered.

**MapStruct in native:**  
MapStruct is compile-time code generation — it produces plain Java method calls, **no reflection**. MapStruct mappers work natively without any special configuration. ✅

**Panache in native:**  
Panache entities use build-time bytecode enhancement in Quarkus — compatible with native. ✅  
Exception: **projection classes** (used with `.project(MyDto.class)`) ARE accessed via reflection and need `@RegisterForReflection`:

```java
@RegisterForReflection  // ← Required for native
public class UserSummary {
    public final String name;
    public UserSummary(String name) { this.name = name; }
}
// Usage: Person.find("status", Status.Active).project(UserSummary.class)
```

**Jackson DTOs in native:**  
Jackson uses reflection for serialization. Quarkus auto-registers return types of JAX-RS endpoints, but nested types or types only referenced via generics may be missed. If a DTO has `null` fields in native mode, add:
```java
@RegisterForReflection
public class UserResponse { ... }
```

**Classpath resources in native:**  
Files in `src/main/resources/` are not automatically embedded in the native binary. Register them explicitly:
```properties
# Single file
quarkus.native.resources.includes=publicKey.pem

# Pattern
quarkus.native.resources.includes=db/migration/*.sql
```

**Source:** https://quarkus.io/guides/writing-native-applications-tips  
https://quarkus.io/guides/security-jwt

---

### Pitfall 9: `@Context` and Servlet API in Quarkus REST (Reactive)

**What goes wrong:**  
Code compiles fine but fails at runtime with `NullPointerException` or `IllegalStateException` when using `@Context HttpServletRequest` in a resource class.

**Why:**  
`quarkus-rest` is NOT servlet-based. It runs on Vert.x. There is no `HttpServletRequest` object. If `quarkus-undertow` is on the classpath, the interface resolves at compile time but the injection returns `null` at runtime.

**Fix:**
```java
// ❌ Won't work with quarkus-rest
@Context HttpServletRequest request;

// ✅ Use JAX-RS standard
@Context UriInfo uriInfo;
@Context HttpHeaders headers;
@Context SecurityContext securityContext;
```

**Source:** https://quarkus.io/guides/rest-migration

---

### Pitfall 10: `ExceptionMapper` Not Registered in Multi-Module Setup

**What goes wrong:**  
Custom `ExceptionMapper<T>` classes in the `adapter` module are not picked up, so domain exceptions propagate as raw 500 errors.

**Why:**  
In `quarkus-rest`, providers are discovered via Jandex index and CDI. If the adapter module doesn't have a proper Jandex index (or the class isn't annotated with `@Provider`), the mapper is invisible.

**Prevention:**
- Annotate with both `@Provider` and `@ApplicationScoped`
- Ensure the adapter module either has `quarkus-maven-plugin` (it does, as the deployable) OR has `jandex-maven-plugin`
- Check registered mappers at `http://localhost:8080/q/dev-ui/quarkus-rest/exception-mappers` in dev mode

```java
@Provider
@ApplicationScoped
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {
    @Override
    public Response toResponse(DomainException e) {
        return Response.status(Response.Status.UNPROCESSABLE_ENTITY)
                       .entity(new ErrorResponse(e.getMessage()))
                       .build();
    }
}
```

**Source:** https://quarkus.io/guides/rest-migration — *"Jakarta REST providers"*

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| Multi-module pom.xml setup | `quarkus-maven-plugin` in parent POM | Keep plugin only in deployable adapter module |
| Adding domain/application/infrastructure modules | CDI beans not discovered | Add `jandex-maven-plugin` to each sub-module |
| Adding MapStruct to infrastructure + adapter | Missing `lombok-mapstruct-binding` | Add all 3 paths: mapstruct-processor + lombok + binding |
| Flyway setup | `migrate-at-start=false` (default), migrations never run | Explicitly set `quarkus.flyway.migrate-at-start=true` |
| Flyway setup | Wrong file naming (single underscore, lowercase v) | Always use `V{n}__{Description}.sql` pattern |
| SmallRye JWT wiring | Wrong property prefix (`quarkus.smallrye-jwt.*` vs `mp.jwt.*`) | Use `mp.jwt.verify.publickey.location` and `mp.jwt.verify.issuer` |
| Panache entity design | JPA annotations leaking into domain module | Keep `@Entity`/`PanacheEntity` exclusively in infrastructure |
| Native build | Public key PEM not embedded | Add `quarkus.native.resources.includes=publicKey.pem` |
| Native build | Projection DTOs missing `@RegisterForReflection` | Annotate all classes used in `.project(Dto.class)` |
| REST ExceptionMappers | Custom mappers silently ignored | `@Provider` + `@ApplicationScoped` + verify in dev-ui |

---

## Sources

| Source | URL | Confidence |
|--------|-----|------------|
| Quarkus Maven Tooling Guide | https://quarkus.io/guides/maven-tooling | HIGH — official |
| Quarkus CDI Reference | https://quarkus.io/guides/cdi-reference | HIGH — official |
| Quarkus Flyway Guide | https://quarkus.io/guides/flyway | HIGH — official |
| Quarkus Security JWT Guide | https://quarkus.io/guides/security-jwt | HIGH — official |
| Quarkus REST Migration Guide | https://quarkus.io/guides/rest-migration | HIGH — official |
| Quarkus Panache Guide | https://quarkus.io/guides/hibernate-orm-panache | HIGH — official |
| Quarkus Native Application Tips | https://quarkus.io/guides/writing-native-applications-tips | HIGH — official |
| MapStruct Reference — Lombok | https://mapstruct.org/documentation/stable/reference/html/ | HIGH — official |
| MapStruct FAQ — Lombok | https://mapstruct.org/faq/ | HIGH — official |
