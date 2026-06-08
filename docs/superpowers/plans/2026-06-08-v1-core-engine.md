# v1 Core Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the `core` module — the dependency root of the Appium platform — providing config loading, per-thread context isolation, an SPI registry, an event bus, a thread-safe device pool, lifecycle orchestration, and TestNG base classes, fully unit-tested against a fake driver with no real device required.

**Architecture:** Maven multi-module project (parent POM + `core` module). `core` knows interfaces only and discovers implementations via `ServiceLoader`. Per-thread `DriverContext` held in a `ThreadLocal` is the isolation boundary. A copy-on-write `EventBus` decouples capture/reporting listeners from the lifecycle. A `BlockingQueue`-backed `DevicePool` hands devices to parallel workers. Driver creation is abstracted behind a `DriverProvider` SPI so the engine is testable with a fake driver.

**Tech Stack:** Java 17, Maven, TestNG 7.x, Appium java-client 9.x (compile-time type only), SnakeYAML (config), JUnit 5 + Mockito (unit tests for the engine itself), AssertJ (assertions).

---

## File Structure

```
appium-framework/                         parent (packaging=pom)
├─ pom.xml                                 parent POM: BOM, dep mgmt, Java 17, plugins
└─ core/
   ├─ pom.xml                             core module POM
   └─ src/
      ├─ main/java/io/framework/core/
      │  ├─ exception/
      │  │  ├─ FrameworkException.java          base unchecked exception
      │  │  ├─ ConfigException.java
      │  │  ├─ DriverInitException.java
      │  │  ├─ ElementNotFoundException.java     carries tried candidates
      │  │  ├─ UnsupportedActionException.java
      │  │  └─ SecretResolutionException.java
      │  ├─ config/
      │  │  ├─ Platform.java                     enum ANDROID, IOS, BOTH
      │  │  ├─ Execution.java                    record: mode, parallelBy, threads
      │  │  ├─ Capture.java                      record: screenshots, video, network, vitals
      │  │  ├─ RetryPolicy.java                  record: enabled, maxRetries, retryOn, quarantineAfter
      │  │  ├─ FrameworkConfig.java              immutable aggregate + typed getters
      │  │  ├─ ValueResolver.java                interface: resolve(String) for ${...} placeholders
      │  │  ├─ PlaceholderResolver.java          expands ${env:X}; pluggable extra resolvers
      │  │  └─ ConfigLoader.java                 cascade: defaults→yaml→-D→env
      │  ├─ spi/
      │  │  └─ ServiceRegistry.java              ServiceLoader wrapper + cache
      │  ├─ events/
      │  │  ├─ TestEvent.java                    enum of lifecycle events
      │  │  ├─ EventContext.java                 record: event + payload map
      │  │  ├─ EventListener.java                interface
      │  │  └─ EventBus.java                     copy-on-write listener dispatch
      │  ├─ parallel/
      │  │  ├─ DeviceLease.java                  record: deviceId, systemPort, providerName
      │  │  └─ DevicePool.java                   BlockingQueue acquire/release
      │  ├─ driver/
      │  │  └─ DriverProvider.java               SPI: create(Platform, Capabilities)→AppiumDriver
      │  ├─ context/
      │  │  ├─ DriverContext.java                per-thread world
      │  │  └─ ContextManager.java               ThreadLocal holder + facade
      │  ├─ lifecycle/
      │  │  └─ DriverLifecycle.java              acquire→create→context; teardown reverse
      │  └─ base/
      │     ├─ BaseTest.java                     TestNG @BeforeMethod/@AfterMethod skeleton
      │     └─ BaseScreen.java                   base for page objects (current context accessor)
      └─ test/java/io/framework/core/
         ├─ config/ConfigLoaderTest.java
         ├─ config/PlaceholderResolverTest.java
         ├─ spi/ServiceRegistryTest.java
         ├─ events/EventBusTest.java
         ├─ parallel/DevicePoolTest.java
         ├─ context/ContextManagerTest.java
         ├─ lifecycle/DriverLifecycleTest.java
         └─ support/FakeDriverProvider.java      test double implementing DriverProvider
```

**Package root:** `io.framework.core`. Group id `io.framework`, version `0.1.0-SNAPSHOT`.

---

## Task 1: Parent POM and core module skeleton

**Files:**
- Create: `pom.xml` (parent)
- Create: `core/pom.xml`

- [ ] **Step 1: Create the parent POM**

Create `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>io.framework</groupId>
  <artifactId>appium-framework</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>pom</packaging>

  <modules>
    <module>core</module>
  </modules>

  <properties>
    <maven.compiler.release>17</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <appium-java-client.version>9.2.2</appium-java-client.version>
    <testng.version>7.10.2</testng.version>
    <snakeyaml.version>2.2</snakeyaml.version>
    <junit.version>5.10.2</junit.version>
    <mockito.version>5.11.0</mockito.version>
    <assertj.version>3.25.3</assertj.version>
    <surefire.version>3.2.5</surefire.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>io.appium</groupId>
        <artifactId>java-client</artifactId>
        <version>${appium-java-client.version}</version>
      </dependency>
      <dependency>
        <groupId>org.testng</groupId>
        <artifactId>testng</artifactId>
        <version>${testng.version}</version>
      </dependency>
      <dependency>
        <groupId>org.yaml</groupId>
        <artifactId>snakeyaml</artifactId>
        <version>${snakeyaml.version}</version>
      </dependency>
      <dependency>
        <groupId>org.junit</groupId>
        <artifactId>junit-bom</artifactId>
        <version>${junit.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>${mockito.version}</version>
      </dependency>
      <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>${assertj.version}</version>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>${surefire.version}</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
```

- [ ] **Step 2: Create the core module POM**

Create `core/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>io.framework</groupId>
    <artifactId>appium-framework</artifactId>
    <version>0.1.0-SNAPSHOT</version>
  </parent>

  <artifactId>core</artifactId>

  <dependencies>
    <dependency>
      <groupId>io.appium</groupId>
      <artifactId>java-client</artifactId>
    </dependency>
    <dependency>
      <groupId>org.testng</groupId>
      <artifactId>testng</artifactId>
    </dependency>
    <dependency>
      <groupId>org.yaml</groupId>
      <artifactId>snakeyaml</artifactId>
    </dependency>

    <!-- engine self-tests use JUnit 5 + Mockito + AssertJ -->
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.mockito</groupId>
      <artifactId>mockito-core</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.assertj</groupId>
      <artifactId>assertj-core</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <!-- run JUnit 5 engine tests -->
          <includes>
            <include>**/*Test.java</include>
          </includes>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 3: Verify the project builds (empty)**

Run: `mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS` with no source files yet (both modules resolve).

- [ ] **Step 4: Commit**

```bash
git add pom.xml core/pom.xml
git commit -m "build: scaffold parent POM and core module"
```

---

## Task 2: Exception hierarchy

**Files:**
- Create: `core/src/main/java/io/framework/core/exception/FrameworkException.java`
- Create: `core/src/main/java/io/framework/core/exception/ConfigException.java`
- Create: `core/src/main/java/io/framework/core/exception/DriverInitException.java`
- Create: `core/src/main/java/io/framework/core/exception/ElementNotFoundException.java`
- Create: `core/src/main/java/io/framework/core/exception/UnsupportedActionException.java`
- Create: `core/src/main/java/io/framework/core/exception/SecretResolutionException.java`
- Test: `core/src/test/java/io/framework/core/exception/ElementNotFoundExceptionTest.java`

- [ ] **Step 1: Write the failing test**

Create `core/src/test/java/io/framework/core/exception/ElementNotFoundExceptionTest.java`:

```java
package io.framework.core.exception;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ElementNotFoundExceptionTest {

    @Test
    void messageListsTriedCandidates() {
        var ex = new ElementNotFoundException("loginButton",
                List.of("id=login", "accessibility-id=login_btn"));
        assertThat(ex).isInstanceOf(FrameworkException.class);
        assertThat(ex.elementName()).isEqualTo("loginButton");
        assertThat(ex.triedCandidates()).containsExactly("id=login", "accessibility-id=login_btn");
        assertThat(ex.getMessage()).contains("loginButton").contains("id=login");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl core test -Dtest=ElementNotFoundExceptionTest`
Expected: FAIL — compilation error, classes do not exist.

- [ ] **Step 3: Write the exception classes**

Create `core/src/main/java/io/framework/core/exception/FrameworkException.java`:

```java
package io.framework.core.exception;

/** Base unchecked exception for all framework errors. */
public class FrameworkException extends RuntimeException {
    public FrameworkException(String message) { super(message); }
    public FrameworkException(String message, Throwable cause) { super(message, cause); }
}
```

Create `core/src/main/java/io/framework/core/exception/ConfigException.java`:

```java
package io.framework.core.exception;

/** Thrown for invalid or unreadable configuration. Fail-fast at startup. */
public class ConfigException extends FrameworkException {
    public ConfigException(String message) { super(message); }
    public ConfigException(String message, Throwable cause) { super(message, cause); }
}
```

Create `core/src/main/java/io/framework/core/exception/DriverInitException.java`:

```java
package io.framework.core.exception;

/** Thrown when an Appium driver cannot be created. */
public class DriverInitException extends FrameworkException {
    public DriverInitException(String message, Throwable cause) { super(message, cause); }
}
```

Create `core/src/main/java/io/framework/core/exception/ElementNotFoundException.java`:

```java
package io.framework.core.exception;

import java.util.List;

/** Thrown when no locator candidate (and no heal) resolves an element. */
public class ElementNotFoundException extends FrameworkException {
    private final String elementName;
    private final List<String> triedCandidates;

    public ElementNotFoundException(String elementName, List<String> triedCandidates) {
        super("Element '" + elementName + "' not found. Tried candidates: " + triedCandidates);
        this.elementName = elementName;
        this.triedCandidates = List.copyOf(triedCandidates);
    }

    public String elementName() { return elementName; }
    public List<String> triedCandidates() { return triedCandidates; }
}
```

Create `core/src/main/java/io/framework/core/exception/UnsupportedActionException.java`:

```java
package io.framework.core.exception;

/** Thrown when an action is not supported on the active platform. */
public class UnsupportedActionException extends FrameworkException {
    public UnsupportedActionException(String action, String platform) {
        super("Action '" + action + "' is not supported on platform " + platform);
    }
}
```

Create `core/src/main/java/io/framework/core/exception/SecretResolutionException.java`:

```java
package io.framework.core.exception;

/** Thrown when a referenced secret cannot be resolved. Fail-fast at startup. */
public class SecretResolutionException extends FrameworkException {
    public SecretResolutionException(String key, String backend) {
        super("Could not resolve secret '" + key + "' from backend '" + backend + "'");
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl core test -Dtest=ElementNotFoundExceptionTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/java/io/framework/core/exception core/src/test/java/io/framework/core/exception
git commit -m "feat(core): add typed exception hierarchy"
```

---

## Task 3: Config value types

**Files:**
- Create: `core/src/main/java/io/framework/core/config/Platform.java`
- Create: `core/src/main/java/io/framework/core/config/Execution.java`
- Create: `core/src/main/java/io/framework/core/config/Capture.java`
- Create: `core/src/main/java/io/framework/core/config/RetryPolicy.java`
- Test: `core/src/test/java/io/framework/core/config/ConfigValueTypesTest.java`

- [ ] **Step 1: Write the failing test**

Create `core/src/test/java/io/framework/core/config/ConfigValueTypesTest.java`:

```java
package io.framework.core.config;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ConfigValueTypesTest {

    @Test
    void platformParsesCaseInsensitively() {
        assertThat(Platform.from("android")).isEqualTo(Platform.ANDROID);
        assertThat(Platform.from("IOS")).isEqualTo(Platform.IOS);
        assertThat(Platform.from("Both")).isEqualTo(Platform.BOTH);
    }

    @Test
    void executionHoldsThreadAndMode() {
        var e = new Execution(Execution.Mode.PARALLEL, Execution.ParallelBy.DEVICE, 4);
        assertThat(e.threads()).isEqualTo(4);
        assertThat(e.mode()).isEqualTo(Execution.Mode.PARALLEL);
        assertThat(e.parallelBy()).isEqualTo(Execution.ParallelBy.DEVICE);
    }

    @Test
    void retryPolicyExposesRetryOnSet() {
        var r = new RetryPolicy(true, 2, List.of("infra", "network"), 3);
        assertThat(r.shouldRetryOn("network")).isTrue();
        assertThat(r.shouldRetryOn("assertion")).isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl core test -Dtest=ConfigValueTypesTest`
Expected: FAIL — classes do not exist.

- [ ] **Step 3: Write the value types**

Create `core/src/main/java/io/framework/core/config/Platform.java`:

```java
package io.framework.core.config;

import io.framework.core.exception.ConfigException;

public enum Platform {
    ANDROID, IOS, BOTH;

    public static Platform from(String raw) {
        if (raw == null) throw new ConfigException("platform is required");
        try {
            return Platform.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ConfigException("Unknown platform: " + raw + " (expected android|ios|both)");
        }
    }
}
```

Create `core/src/main/java/io/framework/core/config/Execution.java`:

```java
package io.framework.core.config;

public record Execution(Mode mode, ParallelBy parallelBy, int threads) {
    public enum Mode { SEQUENTIAL, PARALLEL }
    public enum ParallelBy { TEST, CLASS, SUITE, PLATFORM, DEVICE }

    public Execution {
        if (threads < 1) throw new IllegalArgumentException("threads must be >= 1");
    }
}
```

Create `core/src/main/java/io/framework/core/config/Capture.java`:

```java
package io.framework.core.config;

public record Capture(When screenshots, When video, boolean network, boolean vitals) {
    public enum When { OFF, ON_ASSERTION, ON_FAILURE, ON_ACTION, ALWAYS }
}
```

Create `core/src/main/java/io/framework/core/config/RetryPolicy.java`:

```java
package io.framework.core.config;

import java.util.List;
import java.util.Set;

public record RetryPolicy(boolean enabled, int maxRetries, List<String> retryOn, int quarantineAfter) {
    public RetryPolicy {
        retryOn = List.copyOf(retryOn);
    }
    public boolean shouldRetryOn(String classification) {
        return enabled && Set.copyOf(retryOn).contains(classification);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl core test -Dtest=ConfigValueTypesTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/java/io/framework/core/config core/src/test/java/io/framework/core/config/ConfigValueTypesTest.java
git commit -m "feat(core): add config value types (Platform, Execution, Capture, RetryPolicy)"
```

---

## Task 4: PlaceholderResolver

**Files:**
- Create: `core/src/main/java/io/framework/core/config/ValueResolver.java`
- Create: `core/src/main/java/io/framework/core/config/PlaceholderResolver.java`
- Test: `core/src/test/java/io/framework/core/config/PlaceholderResolverTest.java`

- [ ] **Step 1: Write the failing test**

Create `core/src/test/java/io/framework/core/config/PlaceholderResolverTest.java`:

```java
package io.framework.core.config;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceholderResolverTest {

    @Test
    void expandsEnvPlaceholderFromProvidedSource() {
        var resolver = new PlaceholderResolver(Map.of("HOST", "example.com")::get);
        assertThat(resolver.expand("https://${env:HOST}/api")).isEqualTo("https://example.com/api");
    }

    @Test
    void leavesPlainStringsUntouched() {
        var resolver = new PlaceholderResolver(k -> null);
        assertThat(resolver.expand("plain")).isEqualTo("plain");
    }

    @Test
    void delegatesNonEnvPrefixToExtraResolver() {
        var resolver = new PlaceholderResolver(k -> null);
        resolver.register("secret", k -> "S3CR3T");
        assertThat(resolver.expand("${secret:PWD}")).isEqualTo("S3CR3T");
    }

    @Test
    void throwsWhenUnresolved() {
        var resolver = new PlaceholderResolver(k -> null);
        assertThatThrownBy(() -> resolver.expand("${env:MISSING}"))
                .hasMessageContaining("MISSING");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl core test -Dtest=PlaceholderResolverTest`
Expected: FAIL — classes do not exist.

- [ ] **Step 3: Write ValueResolver and PlaceholderResolver**

Create `core/src/main/java/io/framework/core/config/ValueResolver.java`:

```java
package io.framework.core.config;

/** Resolves a key to a value for a given placeholder prefix. Returns null if unknown. */
@FunctionalInterface
public interface ValueResolver {
    String resolve(String key);
}
```

Create `core/src/main/java/io/framework/core/config/PlaceholderResolver.java`:

```java
package io.framework.core.config;

import io.framework.core.exception.ConfigException;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands ${prefix:key} placeholders inside strings.
 * The "env" prefix is built in (backed by the source given at construction).
 * Other prefixes (e.g. "secret", "vault") are registered by other modules.
 */
public final class PlaceholderResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([a-zA-Z]+):([^}]+)}");

    private final Map<String, ValueResolver> resolvers = new HashMap<>();

    public PlaceholderResolver(ValueResolver envSource) {
        resolvers.put("env", envSource);
    }

    public void register(String prefix, ValueResolver resolver) {
        resolvers.put(prefix, resolver);
    }

    public String expand(String raw) {
        if (raw == null) return null;
        Matcher m = PLACEHOLDER.matcher(raw);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String prefix = m.group(1);
            String key = m.group(2);
            ValueResolver resolver = resolvers.get(prefix);
            if (resolver == null) {
                throw new ConfigException("No resolver registered for placeholder prefix '" + prefix + "'");
            }
            String value = resolver.resolve(key);
            if (value == null) {
                throw new ConfigException("Could not resolve placeholder ${" + prefix + ":" + key + "}");
            }
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl core test -Dtest=PlaceholderResolverTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/java/io/framework/core/config/ValueResolver.java core/src/main/java/io/framework/core/config/PlaceholderResolver.java core/src/test/java/io/framework/core/config/PlaceholderResolverTest.java
git commit -m "feat(core): add placeholder resolver with pluggable prefixes"
```

---

## Task 5: FrameworkConfig and ConfigLoader cascade

**Files:**
- Create: `core/src/main/java/io/framework/core/config/FrameworkConfig.java`
- Create: `core/src/main/java/io/framework/core/config/ConfigLoader.java`
- Create: `core/src/test/resources/config/test.yaml`
- Test: `core/src/test/java/io/framework/core/config/ConfigLoaderTest.java`

- [ ] **Step 1: Write the failing test and fixture**

Create `core/src/test/resources/config/test.yaml`:

```yaml
env: test
platform: android
execution:
  mode: parallel
  parallelBy: device
  threads: 2
retry:
  enabled: true
  maxRetries: 2
  retryOn: [infra, network]
  quarantineAfter: 3
capture:
  screenshots: onAction
  video: onFailure
  network: true
  vitals: true
baseUrl: https://${env:TEST_HOST}/api
```

Create `core/src/test/java/io/framework/core/config/ConfigLoaderTest.java`:

```java
package io.framework.core.config;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class ConfigLoaderTest {

    private ConfigLoader loaderWith(Map<String, String> cli, Map<String, String> env) {
        return new ConfigLoader(
                "config/test.yaml",        // classpath resource
                cli::get,                  // CLI override source (dotted keys)
                env::get);                 // ENV override source (UPPER_SNAKE keys)
    }

    @Test
    void loadsYamlValues() {
        var cfg = loaderWith(Map.of(), Map.of("TEST_HOST", "example.com")).load();
        assertThat(cfg.platform()).isEqualTo(Platform.ANDROID);
        assertThat(cfg.execution().threads()).isEqualTo(2);
        assertThat(cfg.execution().parallelBy()).isEqualTo(Execution.ParallelBy.DEVICE);
        assertThat(cfg.capture().screenshots()).isEqualTo(Capture.When.ON_ACTION);
        assertThat(cfg.retry().shouldRetryOn("network")).isTrue();
    }

    @Test
    void cliOverridesYaml() {
        var cfg = loaderWith(Map.of("execution.threads", "8"),
                Map.of("TEST_HOST", "example.com")).load();
        assertThat(cfg.execution().threads()).isEqualTo(8);
    }

    @Test
    void envOverridesCli() {
        var cfg = loaderWith(Map.of("execution.threads", "8"),
                Map.of("TEST_HOST", "example.com", "EXECUTION_THREADS", "16")).load();
        assertThat(cfg.execution().threads()).isEqualTo(16);
    }

    @Test
    void expandsEnvPlaceholderInValues() {
        var cfg = loaderWith(Map.of(), Map.of("TEST_HOST", "example.com")).load();
        assertThat(cfg.string("baseUrl")).isEqualTo("https://example.com/api");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl core test -Dtest=ConfigLoaderTest`
Expected: FAIL — `FrameworkConfig` / `ConfigLoader` do not exist.

- [ ] **Step 3: Write FrameworkConfig**

Create `core/src/main/java/io/framework/core/config/FrameworkConfig.java`:

```java
package io.framework.core.config;

import io.framework.core.exception.ConfigException;

import java.util.Map;
import java.util.Optional;

/**
 * Immutable, typed view over the merged configuration map.
 * Built once by ConfigLoader and injected via context. No System.getProperty elsewhere.
 */
public final class FrameworkConfig {

    private final String env;
    private final Platform platform;
    private final Execution execution;
    private final Capture capture;
    private final RetryPolicy retry;
    private final Map<String, Object> raw;   // already placeholder-expanded for string leaves

    FrameworkConfig(String env, Platform platform, Execution execution,
                    Capture capture, RetryPolicy retry, Map<String, Object> raw) {
        this.env = env;
        this.platform = platform;
        this.execution = execution;
        this.capture = capture;
        this.retry = retry;
        this.raw = Map.copyOf(raw);
    }

    public String env() { return env; }
    public Platform platform() { return platform; }
    public Execution execution() { return execution; }
    public Capture capture() { return capture; }
    public RetryPolicy retry() { return retry; }

    /** Dotted-path lookup for arbitrary leaf values, e.g. "baseUrl" or "device.target". */
    public String string(String dottedKey) {
        return optional(dottedKey)
                .map(Object::toString)
                .orElseThrow(() -> new ConfigException("Missing config key: " + dottedKey));
    }

    public Optional<Object> optional(String dottedKey) {
        Object node = raw;
        for (String part : dottedKey.split("\\.")) {
            if (!(node instanceof Map<?, ?> m)) return Optional.empty();
            node = m.get(part);
            if (node == null) return Optional.empty();
        }
        return Optional.of(node);
    }
}
```

- [ ] **Step 4: Write ConfigLoader**

Create `core/src/main/java/io/framework/core/config/ConfigLoader.java`:

```java
package io.framework.core.config;

import io.framework.core.exception.ConfigException;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds an immutable FrameworkConfig from the cascade:
 *   classpath YAML  ->  CLI (-Dkey=val, dotted)  ->  ENV vars (UPPER_SNAKE).
 * Later sources win. String leaves are placeholder-expanded via env source.
 */
public final class ConfigLoader {

    private final String classpathResource;
    private final ValueResolver cliSource;   // dotted-key -> value (e.g. "execution.threads")
    private final ValueResolver envSource;   // UPPER_SNAKE -> value (e.g. "EXECUTION_THREADS")

    public ConfigLoader(String classpathResource, ValueResolver cliSource, ValueResolver envSource) {
        this.classpathResource = classpathResource;
        this.cliSource = cliSource;
        this.envSource = envSource;
    }

    @SuppressWarnings("unchecked")
    public FrameworkConfig load() {
        Map<String, Object> merged = readYaml();
        applyOverrides(merged, "");
        expandPlaceholders(merged, new PlaceholderResolver(envSource));

        String env = str(merged, "env", "local");
        Platform platform = Platform.from(str(merged, "platform", null));

        Map<String, Object> exec = (Map<String, Object>) merged.getOrDefault("execution", Map.of());
        Execution execution = new Execution(
                Execution.Mode.valueOf(str(exec, "mode", "sequential").toUpperCase()),
                Execution.ParallelBy.valueOf(str(exec, "parallelBy", "test").toUpperCase()),
                intval(exec, "threads", 1));

        Map<String, Object> cap = (Map<String, Object>) merged.getOrDefault("capture", Map.of());
        Capture capture = new Capture(
                when(str(cap, "screenshots", "onFailure")),
                when(str(cap, "video", "off")),
                boolval(cap, "network", false),
                boolval(cap, "vitals", false));

        Map<String, Object> rt = (Map<String, Object>) merged.getOrDefault("retry", Map.of());
        RetryPolicy retry = new RetryPolicy(
                boolval(rt, "enabled", false),
                intval(rt, "maxRetries", 0),
                (java.util.List<String>) rt.getOrDefault("retryOn", java.util.List.of()),
                intval(rt, "quarantineAfter", Integer.MAX_VALUE));

        return new FrameworkConfig(env, platform, execution, capture, retry, merged);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readYaml() {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(classpathResource)) {
            if (in == null) throw new ConfigException("Config resource not found: " + classpathResource);
            Object loaded = new Yaml().load(in);
            if (loaded == null) return new LinkedHashMap<>();
            if (!(loaded instanceof Map)) throw new ConfigException("Config root must be a YAML mapping");
            return new LinkedHashMap<>((Map<String, Object>) loaded);
        } catch (ConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigException("Failed to read config: " + classpathResource, e);
        }
    }

    /** Walk the tree; for each leaf path, apply CLI then ENV override if present. */
    @SuppressWarnings("unchecked")
    private void applyOverrides(Map<String, Object> node, String prefix) {
        for (Map.Entry<String, Object> e : node.entrySet()) {
            String dotted = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            if (e.getValue() instanceof Map<?, ?> child) {
                applyOverrides((Map<String, Object>) child, dotted);
            } else {
                String cli = cliSource.resolve(dotted);
                String env = envSource.resolve(dotted.replace('.', '_').toUpperCase());
                if (env != null) e.setValue(env);
                else if (cli != null) e.setValue(cli);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void expandPlaceholders(Map<String, Object> node, PlaceholderResolver resolver) {
        for (Map.Entry<String, Object> e : node.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Map<?, ?> child) expandPlaceholders((Map<String, Object>) child, resolver);
            else if (v instanceof String s) e.setValue(resolver.expand(s));
        }
    }

    private static String str(Map<String, Object> m, String k, String def) {
        Object v = m.get(k);
        return v == null ? def : v.toString();
    }
    private static int intval(Map<String, Object> m, String k, int def) {
        Object v = m.get(k);
        return v == null ? def : Integer.parseInt(v.toString());
    }
    private static boolean boolval(Map<String, Object> m, String k, boolean def) {
        Object v = m.get(k);
        return v == null ? def : Boolean.parseBoolean(v.toString());
    }
    private static Capture.When when(String raw) {
        return switch (raw.toLowerCase()) {
            case "off" -> Capture.When.OFF;
            case "onassertion" -> Capture.When.ON_ASSERTION;
            case "onfailure" -> Capture.When.ON_FAILURE;
            case "onaction" -> Capture.When.ON_ACTION;
            case "always" -> Capture.When.ALWAYS;
            default -> throw new ConfigException("Unknown capture value: " + raw);
        };
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q -pl core test -Dtest=ConfigLoaderTest`
Expected: PASS (all four cases).

- [ ] **Step 6: Commit**

```bash
git add core/src/main/java/io/framework/core/config/FrameworkConfig.java core/src/main/java/io/framework/core/config/ConfigLoader.java core/src/test/resources/config/test.yaml core/src/test/java/io/framework/core/config/ConfigLoaderTest.java
git commit -m "feat(core): add immutable FrameworkConfig and cascade ConfigLoader"
```

---

## Task 6: ServiceRegistry

**Files:**
- Create: `core/src/main/java/io/framework/core/spi/ServiceRegistry.java`
- Test: `core/src/test/java/io/framework/core/spi/ServiceRegistryTest.java`
- Test fixture: `core/src/test/java/io/framework/core/spi/Greeter.java`
- Test fixture: `core/src/test/java/io/framework/core/spi/EnglishGreeter.java`
- Test fixture: `core/src/test/resources/META-INF/services/io.framework.core.spi.Greeter`

- [ ] **Step 1: Write the failing test and SPI fixtures**

Create `core/src/test/java/io/framework/core/spi/Greeter.java`:

```java
package io.framework.core.spi;

public interface Greeter { String greet(); }
```

Create `core/src/test/java/io/framework/core/spi/EnglishGreeter.java`:

```java
package io.framework.core.spi;

public class EnglishGreeter implements Greeter {
    public String greet() { return "hello"; }
}
```

Create `core/src/test/resources/META-INF/services/io.framework.core.spi.Greeter` with a single line:

```
io.framework.core.spi.EnglishGreeter
```

Create `core/src/test/java/io/framework/core/spi/ServiceRegistryTest.java`:

```java
package io.framework.core.spi;

import io.framework.core.exception.FrameworkException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceRegistryTest {

    @Test
    void loadsRegisteredImplementation() {
        var registry = new ServiceRegistry();
        Greeter g = registry.get(Greeter.class);
        assertThat(g.greet()).isEqualTo("hello");
    }

    @Test
    void cachesSameInstanceAcrossCalls() {
        var registry = new ServiceRegistry();
        assertThat(registry.get(Greeter.class)).isSameAs(registry.get(Greeter.class));
    }

    @Test
    void throwsWhenNoImplementation() {
        var registry = new ServiceRegistry();
        assertThatThrownBy(() -> registry.get(Runnable.class))
                .isInstanceOf(FrameworkException.class)
                .hasMessageContaining("No implementation");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl core test -Dtest=ServiceRegistryTest`
Expected: FAIL — `ServiceRegistry` does not exist.

- [ ] **Step 3: Write ServiceRegistry**

Create `core/src/main/java/io/framework/core/spi/ServiceRegistry.java`:

```java
package io.framework.core.spi;

import io.framework.core.exception.FrameworkException;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thin cached wrapper over ServiceLoader. Resolves the single (or all) implementations
 * of an SPI interface from the classpath. First scan is cached for the registry's lifetime.
 */
public final class ServiceRegistry {

    private final Map<Class<?>, Object> singletons = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> spi) {
        return (T) singletons.computeIfAbsent(spi, this::loadFirst);
    }

    public <T> List<T> all(Class<T> spi) {
        return ServiceLoader.load(spi).stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toList());
    }

    private <T> T loadFirst(Class<T> spi) {
        return ServiceLoader.load(spi).findFirst()
                .orElseThrow(() -> new FrameworkException(
                        "No implementation registered for SPI " + spi.getName()
                                + " (check META-INF/services and classpath)"));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl core test -Dtest=ServiceRegistryTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/java/io/framework/core/spi core/src/test/java/io/framework/core/spi core/src/test/resources/META-INF
git commit -m "feat(core): add cached ServiceLoader-backed ServiceRegistry"
```

---

## Task 7: EventBus

**Files:**
- Create: `core/src/main/java/io/framework/core/events/TestEvent.java`
- Create: `core/src/main/java/io/framework/core/events/EventContext.java`
- Create: `core/src/main/java/io/framework/core/events/EventListener.java`
- Create: `core/src/main/java/io/framework/core/events/EventBus.java`
- Test: `core/src/test/java/io/framework/core/events/EventBusTest.java`

- [ ] **Step 1: Write the failing test**

Create `core/src/test/java/io/framework/core/events/EventBusTest.java`:

```java
package io.framework.core.events;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class EventBusTest {

    @Test
    void deliversEventToAllListenersInRegistrationOrder() {
        var bus = new EventBus();
        List<String> calls = new ArrayList<>();
        bus.subscribe(e -> calls.add("a:" + e.event()));
        bus.subscribe(e -> calls.add("b:" + e.event()));

        bus.emit(TestEvent.TEST_START, Map.of());

        assertThat(calls).containsExactly("a:TEST_START", "b:TEST_START");
    }

    @Test
    void payloadIsAccessibleToListener() {
        var bus = new EventBus();
        var seen = new Object() { Object value; };
        bus.subscribe(e -> seen.value = e.payload().get("name"));

        bus.emit(TestEvent.AFTER_ACTION, Map.of("name", "tap"));

        assertThat(seen.value).isEqualTo("tap");
    }

    @Test
    void oneListenerFailureDoesNotStopOthers() {
        var bus = new EventBus();
        List<String> calls = new ArrayList<>();
        bus.subscribe(e -> { throw new RuntimeException("boom"); });
        bus.subscribe(e -> calls.add("reached"));

        bus.emit(TestEvent.RUN_END, Map.of());

        assertThat(calls).containsExactly("reached");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl core test -Dtest=EventBusTest`
Expected: FAIL — event classes do not exist.

- [ ] **Step 3: Write the event types and bus**

Create `core/src/main/java/io/framework/core/events/TestEvent.java`:

```java
package io.framework.core.events;

public enum TestEvent {
    RUN_START, SUITE_START, TEST_START,
    BEFORE_ACTION, AFTER_ACTION, ASSERTION,
    TEST_FAIL, TEST_PASS, TEST_END,
    SUITE_END, RUN_END
}
```

Create `core/src/main/java/io/framework/core/events/EventContext.java`:

```java
package io.framework.core.events;

import java.util.Map;

/** Immutable event payload delivered to listeners. */
public record EventContext(TestEvent event, Map<String, Object> payload) {
    public EventContext {
        payload = Map.copyOf(payload);
    }
}
```

Create `core/src/main/java/io/framework/core/events/EventListener.java`:

```java
package io.framework.core.events;

@FunctionalInterface
public interface EventListener {
    void on(EventContext ctx);
}
```

Create `core/src/main/java/io/framework/core/events/EventBus.java`:

```java
package io.framework.core.events;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Decouples lifecycle from reactions (capture/reporting/knowledge listeners).
 * Copy-on-write list makes emit safe while other threads subscribe.
 * A failing listener is isolated so it cannot break the run.
 */
public final class EventBus {

    private final CopyOnWriteArrayList<EventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(EventListener listener) {
        listeners.add(listener);
    }

    public void emit(TestEvent event, Map<String, Object> payload) {
        EventContext ctx = new EventContext(event, payload);
        for (EventListener l : listeners) {
            try {
                l.on(ctx);
            } catch (RuntimeException e) {
                // isolate listener failures; never break the run for a capture/report error
                System.err.println("[EventBus] listener failed on " + event + ": " + e.getMessage());
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl core test -Dtest=EventBusTest`
Expected: PASS (all three cases).

- [ ] **Step 5: Commit**

```bash
git add core/src/main/java/io/framework/core/events core/src/test/java/io/framework/core/events/EventBusTest.java
git commit -m "feat(core): add copy-on-write EventBus with listener isolation"
```

---

## Task 8: DevicePool

**Files:**
- Create: `core/src/main/java/io/framework/core/parallel/DeviceLease.java`
- Create: `core/src/main/java/io/framework/core/parallel/DevicePool.java`
- Test: `core/src/test/java/io/framework/core/parallel/DevicePoolTest.java`

- [ ] **Step 1: Write the failing test**

Create `core/src/test/java/io/framework/core/parallel/DevicePoolTest.java`:

```java
package io.framework.core.parallel;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

class DevicePoolTest {

    @Test
    void acquireReturnsAvailableDevice() throws Exception {
        var pool = new DevicePool(List.of(
                new DeviceLease("emulator-5554", 8200, "local")));
        DeviceLease lease = pool.acquire(1, TimeUnit.SECONDS);
        assertThat(lease.deviceId()).isEqualTo("emulator-5554");
    }

    @Test
    void acquireBlocksUntilReleaseWhenExhausted() throws Exception {
        var pool = new DevicePool(List.of(new DeviceLease("d1", 8200, "local")));
        DeviceLease first = pool.acquire(1, TimeUnit.SECONDS);

        var executor = Executors.newSingleThreadExecutor();
        Future<DeviceLease> waiting = executor.submit(() -> pool.acquire(2, TimeUnit.SECONDS));

        Thread.sleep(200);
        assertThat(waiting.isDone()).isFalse();   // still blocked, pool empty

        pool.release(first);
        assertThat(waiting.get(1, TimeUnit.SECONDS).deviceId()).isEqualTo("d1");
        executor.shutdownNow();
    }

    @Test
    void neverHandsSameDeviceToTwoThreads() throws Exception {
        int devices = 4, workers = 16;
        var leases = new java.util.ArrayList<DeviceLease>();
        for (int i = 0; i < devices; i++) leases.add(new DeviceLease("d" + i, 8200 + i, "local"));
        var pool = new DevicePool(leases);

        var concurrentHolders = new AtomicInteger();
        var maxConcurrent = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(workers);
        var tasks = new java.util.ArrayList<Callable<Void>>();
        for (int i = 0; i < workers; i++) {
            tasks.add(() -> {
                DeviceLease l = pool.acquire(5, TimeUnit.SECONDS);
                int now = concurrentHolders.incrementAndGet();
                maxConcurrent.accumulateAndGet(now, Math::max);
                Thread.sleep(20);
                concurrentHolders.decrementAndGet();
                pool.release(l);
                return null;
            });
        }
        executor.invokeAll(tasks);
        executor.shutdown();

        assertThat(maxConcurrent.get()).isLessThanOrEqualTo(devices);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl core test -Dtest=DevicePoolTest`
Expected: FAIL — `DevicePool` / `DeviceLease` do not exist.

- [ ] **Step 3: Write DeviceLease and DevicePool**

Create `core/src/main/java/io/framework/core/parallel/DeviceLease.java`:

```java
package io.framework.core.parallel;

/** A device a worker may hold for the duration of a test. systemPort is the per-session Appium port. */
public record DeviceLease(String deviceId, int systemPort, String providerName) { }
```

Create `core/src/main/java/io/framework/core/parallel/DevicePool.java`:

```java
package io.framework.core.parallel;

import io.framework.core.exception.FrameworkException;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe pool of devices. acquire() blocks when none are free; release() returns one.
 * Guarantees no two workers hold the same lease (a lease is removed from the queue while held).
 */
public final class DevicePool {

    private final LinkedBlockingQueue<DeviceLease> available;
    private final int capacity;

    public DevicePool(Collection<DeviceLease> devices) {
        if (devices.isEmpty()) throw new FrameworkException("DevicePool requires at least one device");
        this.available = new LinkedBlockingQueue<>(devices);
        this.capacity = devices.size();
    }

    public DeviceLease acquire(long timeout, TimeUnit unit) {
        try {
            DeviceLease lease = available.poll(timeout, unit);
            if (lease == null) {
                throw new FrameworkException("No device available within "
                        + timeout + " " + unit.name().toLowerCase());
            }
            return lease;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FrameworkException("Interrupted while acquiring a device", e);
        }
    }

    public void release(DeviceLease lease) {
        available.offer(lease);
    }

    public int capacity() { return capacity; }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl core test -Dtest=DevicePoolTest`
Expected: PASS (including the concurrency stress test).

- [ ] **Step 5: Commit**

```bash
git add core/src/main/java/io/framework/core/parallel core/src/test/java/io/framework/core/parallel/DevicePoolTest.java
git commit -m "feat(core): add blocking thread-safe DevicePool"
```

---

## Task 9: DriverProvider SPI and DriverContext + ContextManager

**Files:**
- Create: `core/src/main/java/io/framework/core/driver/DriverProvider.java`
- Create: `core/src/main/java/io/framework/core/context/DriverContext.java`
- Create: `core/src/main/java/io/framework/core/context/ContextManager.java`
- Test fixture: `core/src/test/java/io/framework/core/support/FakeDriverProvider.java`
- Test: `core/src/test/java/io/framework/core/context/ContextManagerTest.java`

- [ ] **Step 1: Write the failing test and fake provider**

Create `core/src/test/java/io/framework/core/support/FakeDriverProvider.java`:

```java
package io.framework.core.support;

import io.appium.java_client.AppiumDriver;
import io.framework.core.config.Platform;
import io.framework.core.driver.DriverProvider;
import org.openqa.selenium.Capabilities;
import org.mockito.Mockito;

/** Test double: returns a Mockito mock AppiumDriver instead of talking to a real device. */
public class FakeDriverProvider implements DriverProvider {
    @Override
    public AppiumDriver create(Platform platform, Capabilities caps) {
        return Mockito.mock(AppiumDriver.class);
    }
}
```

Create `core/src/test/java/io/framework/core/context/ContextManagerTest.java`:

```java
package io.framework.core.context;

import io.appium.java_client.AppiumDriver;
import io.framework.core.parallel.DeviceLease;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextManagerTest {

    private DriverContext sampleContext(String deviceId) {
        return DriverContext.builder()
                .driver(Mockito.mock(AppiumDriver.class))
                .device(new DeviceLease(deviceId, 8200, "local"))
                .build();
    }

    @Test
    void currentReturnsContextSetOnThisThread() {
        ContextManager.set(sampleContext("d1"));
        try {
            assertThat(ContextManager.current().device().deviceId()).isEqualTo("d1");
        } finally {
            ContextManager.clear();
        }
    }

    @Test
    void throwsWhenNoContextOnThread() {
        ContextManager.clear();
        assertThatThrownBy(ContextManager::current).hasMessageContaining("No DriverContext");
    }

    @Test
    void contextIsIsolatedPerThread() throws Exception {
        ContextManager.set(sampleContext("main"));
        try {
            var executor = Executors.newSingleThreadExecutor();
            Future<String> other = executor.submit(() -> {
                ContextManager.set(sampleContext("worker"));
                try {
                    return ContextManager.current().device().deviceId();
                } finally {
                    ContextManager.clear();
                }
            });
            assertThat(other.get()).isEqualTo("worker");
            assertThat(ContextManager.current().device().deviceId()).isEqualTo("main");
            executor.shutdownNow();
        } finally {
            ContextManager.clear();
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl core test -Dtest=ContextManagerTest`
Expected: FAIL — `DriverProvider`, `DriverContext`, `ContextManager` do not exist.

- [ ] **Step 3: Write DriverProvider SPI**

Create `core/src/main/java/io/framework/core/driver/DriverProvider.java`:

```java
package io.framework.core.driver;

import io.appium.java_client.AppiumDriver;
import io.framework.core.config.Platform;
import org.openqa.selenium.Capabilities;

/**
 * SPI implemented by the `drivers` module (real Android/iOS) and by test doubles.
 * Keeps `core` independent of concrete driver construction so the engine is testable.
 */
public interface DriverProvider {
    AppiumDriver create(Platform platform, Capabilities caps);
}
```

- [ ] **Step 4: Write DriverContext**

Create `core/src/main/java/io/framework/core/context/DriverContext.java`:

```java
package io.framework.core.context;

import io.appium.java_client.AppiumDriver;
import io.framework.core.config.FrameworkConfig;
import io.framework.core.parallel.DeviceLease;

import java.util.Objects;

/**
 * Per-thread world for one test on one device. The isolation boundary:
 * everything a running test reaches goes through the current context.
 * config may be null in unit tests that only exercise driver/device wiring.
 */
public final class DriverContext {

    private final AppiumDriver driver;
    private final DeviceLease device;
    private final FrameworkConfig config;

    private DriverContext(Builder b) {
        this.driver = Objects.requireNonNull(b.driver, "driver");
        this.device = Objects.requireNonNull(b.device, "device");
        this.config = b.config;
    }

    public AppiumDriver driver() { return driver; }
    public DeviceLease device() { return device; }
    public FrameworkConfig config() { return config; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private AppiumDriver driver;
        private DeviceLease device;
        private FrameworkConfig config;

        public Builder driver(AppiumDriver d) { this.driver = d; return this; }
        public Builder device(DeviceLease d) { this.device = d; return this; }
        public Builder config(FrameworkConfig c) { this.config = c; return this; }
        public DriverContext build() { return new DriverContext(this); }
    }
}
```

- [ ] **Step 5: Write ContextManager**

Create `core/src/main/java/io/framework/core/context/ContextManager.java`:

```java
package io.framework.core.context;

import io.framework.core.exception.FrameworkException;

/**
 * Holds the current thread's DriverContext. Facade everything reads via current().
 * Each parallel worker sets its own context; clear() must run in teardown to avoid leaks.
 */
public final class ContextManager {

    private static final ThreadLocal<DriverContext> CURRENT = new ThreadLocal<>();

    private ContextManager() { }

    public static void set(DriverContext context) {
        CURRENT.set(context);
    }

    public static DriverContext current() {
        DriverContext ctx = CURRENT.get();
        if (ctx == null) {
            throw new FrameworkException("No DriverContext on thread "
                    + Thread.currentThread().getName() + " (was set() called in setup?)");
        }
        return ctx;
    }

    public static boolean isSet() {
        return CURRENT.get() != null;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `mvn -q -pl core test -Dtest=ContextManagerTest`
Expected: PASS (including per-thread isolation).

- [ ] **Step 7: Commit**

```bash
git add core/src/main/java/io/framework/core/driver core/src/main/java/io/framework/core/context core/src/test/java/io/framework/core/support core/src/test/java/io/framework/core/context/ContextManagerTest.java
git commit -m "feat(core): add DriverProvider SPI, DriverContext, ThreadLocal ContextManager"
```

---

## Task 10: DriverLifecycle

**Files:**
- Create: `core/src/main/java/io/framework/core/lifecycle/DriverLifecycle.java`
- Test: `core/src/test/java/io/framework/core/lifecycle/DriverLifecycleTest.java`

- [ ] **Step 1: Write the failing test**

Create `core/src/test/java/io/framework/core/lifecycle/DriverLifecycleTest.java`:

```java
package io.framework.core.lifecycle;

import io.appium.java_client.AppiumDriver;
import io.framework.core.config.Platform;
import io.framework.core.context.ContextManager;
import io.framework.core.events.EventBus;
import io.framework.core.events.TestEvent;
import io.framework.core.parallel.DeviceLease;
import io.framework.core.parallel.DevicePool;
import io.framework.core.support.FakeDriverProvider;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DriverLifecycleTest {

    private DriverLifecycle lifecycle(List<String> events) {
        var pool = new DevicePool(List.of(new DeviceLease("d1", 8200, "local")));
        var bus = new EventBus();
        bus.subscribe(e -> events.add(e.event().name()));
        return new DriverLifecycle(pool, new FakeDriverProvider(), bus);
    }

    @Test
    void startSetsContextAndEmitsTestStart() {
        List<String> events = new ArrayList<>();
        var lc = lifecycle(events);
        Capabilities caps = new DesiredCapabilities();

        lc.start(Platform.ANDROID, caps, 2, TimeUnit.SECONDS);
        try {
            assertThat(ContextManager.isSet()).isTrue();
            assertThat(ContextManager.current().device().deviceId()).isEqualTo("d1");
            assertThat(events).contains(TestEvent.TEST_START.name());
        } finally {
            lc.stop(false);
        }
    }

    @Test
    void stopQuitsDriverReleasesDeviceClearsContextEmitsTestEnd() {
        List<String> events = new ArrayList<>();
        var lc = lifecycle(events);
        lc.start(Platform.ANDROID, new DesiredCapabilities(), 2, TimeUnit.SECONDS);
        AppiumDriver driver = ContextManager.current().driver();

        lc.stop(false);

        assertThat(ContextManager.isSet()).isFalse();
        org.mockito.Mockito.verify(driver).quit();
        assertThat(events).contains(TestEvent.TEST_END.name());
        // device returned to pool: a fresh start must succeed
        lc.start(Platform.ANDROID, new DesiredCapabilities(), 1, TimeUnit.SECONDS);
        lc.stop(false);
    }

    @Test
    void stopWithFailureEmitsTestFailBeforeTestEnd() {
        List<String> events = new ArrayList<>();
        var lc = lifecycle(events);
        lc.start(Platform.ANDROID, new DesiredCapabilities(), 2, TimeUnit.SECONDS);

        lc.stop(true);

        int failIdx = events.indexOf(TestEvent.TEST_FAIL.name());
        int endIdx = events.indexOf(TestEvent.TEST_END.name());
        assertThat(failIdx).isGreaterThanOrEqualTo(0);
        assertThat(endIdx).isGreaterThan(failIdx);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl core test -Dtest=DriverLifecycleTest`
Expected: FAIL — `DriverLifecycle` does not exist.

- [ ] **Step 3: Write DriverLifecycle**

Create `core/src/main/java/io/framework/core/lifecycle/DriverLifecycle.java`:

```java
package io.framework.core.lifecycle;

import io.appium.java_client.AppiumDriver;
import io.framework.core.config.Platform;
import io.framework.core.context.ContextManager;
import io.framework.core.context.DriverContext;
import io.framework.core.driver.DriverProvider;
import io.framework.core.events.EventBus;
import io.framework.core.events.TestEvent;
import io.framework.core.parallel.DeviceLease;
import io.framework.core.parallel.DevicePool;
import org.openqa.selenium.Capabilities;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates one test's driver lifecycle:
 *   start: acquire device -> create driver -> set ThreadLocal context -> emit TEST_START
 *   stop:  emit TEST_FAIL/TEST_PASS -> emit TEST_END -> quit driver -> release device -> clear context
 * Ordering matters: failure event fires before the driver is torn down so listeners
 * (screenshot/video/source) can still capture evidence.
 */
public final class DriverLifecycle {

    private final DevicePool pool;
    private final DriverProvider driverProvider;
    private final EventBus bus;

    public DriverLifecycle(DevicePool pool, DriverProvider driverProvider, EventBus bus) {
        this.pool = pool;
        this.driverProvider = driverProvider;
        this.bus = bus;
    }

    public void start(Platform platform, Capabilities caps, long acquireTimeout, TimeUnit unit) {
        DeviceLease lease = pool.acquire(acquireTimeout, unit);
        AppiumDriver driver = driverProvider.create(platform, caps);
        DriverContext ctx = DriverContext.builder().driver(driver).device(lease).build();
        ContextManager.set(ctx);
        bus.emit(TestEvent.TEST_START, Map.of("device", lease.deviceId()));
    }

    public void stop(boolean failed) {
        DriverContext ctx = ContextManager.current();
        try {
            bus.emit(failed ? TestEvent.TEST_FAIL : TestEvent.TEST_PASS,
                    Map.of("device", ctx.device().deviceId()));
            bus.emit(TestEvent.TEST_END, Map.of("device", ctx.device().deviceId()));
        } finally {
            try {
                ctx.driver().quit();
            } finally {
                pool.release(ctx.device());
                ContextManager.clear();
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl core test -Dtest=DriverLifecycleTest`
Expected: PASS (all three cases).

- [ ] **Step 5: Commit**

```bash
git add core/src/main/java/io/framework/core/lifecycle core/src/test/java/io/framework/core/lifecycle/DriverLifecycleTest.java
git commit -m "feat(core): add DriverLifecycle (acquire/create/context, teardown with evidence ordering)"
```

---

## Task 11: BaseScreen and BaseTest

**Files:**
- Create: `core/src/main/java/io/framework/core/base/BaseScreen.java`
- Create: `core/src/main/java/io/framework/core/base/BaseTest.java`
- Test: `core/src/test/java/io/framework/core/base/BaseScreenTest.java`

- [ ] **Step 1: Write the failing test**

Create `core/src/test/java/io/framework/core/base/BaseScreenTest.java`:

```java
package io.framework.core.base;

import io.appium.java_client.AppiumDriver;
import io.framework.core.context.ContextManager;
import io.framework.core.context.DriverContext;
import io.framework.core.parallel.DeviceLease;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.assertj.core.api.Assertions.assertThat;

class BaseScreenTest {

    static class HomeScreen extends BaseScreen {
        AppiumDriver exposedDriver() { return driver(); }
    }

    @AfterEach
    void tearDown() { ContextManager.clear(); }

    @Test
    void screenReadsDriverFromCurrentContext() {
        AppiumDriver mock = Mockito.mock(AppiumDriver.class);
        ContextManager.set(DriverContext.builder()
                .driver(mock)
                .device(new DeviceLease("d1", 8200, "local"))
                .build());

        assertThat(new HomeScreen().exposedDriver()).isSameAs(mock);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl core test -Dtest=BaseScreenTest`
Expected: FAIL — `BaseScreen` does not exist.

- [ ] **Step 3: Write BaseScreen**

Create `core/src/main/java/io/framework/core/base/BaseScreen.java`:

```java
package io.framework.core.base;

import io.appium.java_client.AppiumDriver;
import io.framework.core.context.ContextManager;
import io.framework.core.context.DriverContext;

/**
 * Base for page objects. Subclasses call driver()/context() to reach the current
 * thread's session without threading the driver through constructors.
 */
public abstract class BaseScreen {

    protected DriverContext context() {
        return ContextManager.current();
    }

    protected AppiumDriver driver() {
        return context().driver();
    }
}
```

- [ ] **Step 4: Write BaseTest**

Create `core/src/main/java/io/framework/core/base/BaseTest.java`:

```java
package io.framework.core.base;

import io.framework.core.config.Platform;
import io.framework.core.lifecycle.DriverLifecycle;
import org.openqa.selenium.Capabilities;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.util.concurrent.TimeUnit;

/**
 * TestNG lifecycle skeleton (Template Method). Concrete test base classes in higher
 * modules supply the wired DriverLifecycle, Platform, and Capabilities; here we only
 * define the start/stop hooks so every test gets context setup + teardown for free.
 *
 * Subclasses override provideLifecycle()/providePlatform()/provideCapabilities().
 */
public abstract class BaseTest {

    protected abstract DriverLifecycle provideLifecycle();
    protected abstract Platform providePlatform();
    protected abstract Capabilities provideCapabilities();

    private DriverLifecycle lifecycle;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        lifecycle = provideLifecycle();
        lifecycle.start(providePlatform(), provideCapabilities(), 60, TimeUnit.SECONDS);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        boolean failed = result.getStatus() == ITestResult.FAILURE;
        lifecycle.stop(failed);
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q -pl core test -Dtest=BaseScreenTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add core/src/main/java/io/framework/core/base core/src/test/java/io/framework/core/base/BaseScreenTest.java
git commit -m "feat(core): add BaseScreen and TestNG BaseTest lifecycle skeleton"
```

---

## Task 12: Full module build + DOCS.md

**Files:**
- Create: `core/DOCS.md`

- [ ] **Step 1: Run the entire core test suite**

Run: `mvn -q -pl core test`
Expected: `BUILD SUCCESS`, all test classes green (ElementNotFoundException, ConfigValueTypes, PlaceholderResolver, ConfigLoader, ServiceRegistry, EventBus, DevicePool, ContextManager, DriverLifecycle, BaseScreen).

- [ ] **Step 2: Write the module documentation**

Create `core/DOCS.md`:

```markdown
# core — module documentation

## 1. What it is
The dependency root of the platform. Owns configuration loading, per-thread context
isolation, the SPI registry, the event bus, the device pool, driver lifecycle
orchestration, and the TestNG base classes. It knows interfaces only; concrete drivers,
clouds, reporters, AI, secrets, and security implementations live in other modules and
are discovered via ServiceLoader. **It does NOT** create real Appium drivers, capture
artifacts, or talk to devices itself.

## 2. How to maintain
- `config/` — immutable value types + ConfigLoader cascade (yaml -> CLI -> ENV).
  Invariant: FrameworkConfig is immutable; never mutate after load().
- `context/` — DriverContext is per-thread; ContextManager.clear() MUST run in teardown
  or threads leak contexts. DriverLifecycle.stop() handles this.
- `parallel/` — DevicePool removes a lease from the queue while held; release() returns it.
- `events/` — EventBus isolates listener failures; a broken listener never breaks a run.

## 3. How to add new methods
- New lifecycle event: add to TestEvent enum, emit from DriverLifecycle (or higher),
  add a test in EventBusTest/DriverLifecycleTest.
- New config block: add a record in config/, parse it in ConfigLoader.load(), expose a
  typed getter on FrameworkConfig, cover it in ConfigLoaderTest.
- New SPI: define the interface here (or in a dedicated SPI module), resolve via
  ServiceRegistry.get(YourSpi.class). Add a contract test.

## 4. Coding structure
Patterns: Strategy/SPI (ServiceRegistry + DriverProvider), Builder (DriverContext),
Observer (EventBus), ThreadLocal context (ContextManager), Template Method (BaseTest),
Facade (ContextManager.current, BaseScreen). DSA: BlockingQueue (DevicePool),
ConcurrentHashMap cache (ServiceRegistry), CopyOnWriteArrayList (EventBus).

## 5. Token-optimization usage
core enforces heuristic-first ordering and never sends page-source to AI. Higher modules
must pass scoped element context to AI healers, consult `knowledge` before AI, and keep
`ai.enabled=false` as the default so zero tokens are spent until AI is explicitly enabled.

## 6. Examples
\`\`\`java
FrameworkConfig cfg = new ConfigLoader("config/staging.yaml",
        k -> System.getProperty(k), k -> System.getenv(k)).load();
ServiceRegistry registry = new ServiceRegistry();
DriverProvider provider = registry.get(DriverProvider.class);
DevicePool pool = new DevicePool(/* leases from devices module */);
EventBus bus = new EventBus();
DriverLifecycle lifecycle = new DriverLifecycle(pool, provider, bus);
\`\`\`
```

- [ ] **Step 3: Verify full build once more**

Run: `mvn -q test`
Expected: `BUILD SUCCESS` for parent + core.

- [ ] **Step 4: Commit**

```bash
git add core/DOCS.md
git commit -m "docs(core): add module documentation"
```

---

## Self-Review

**1. Spec coverage (against §7 core deep spec):**
- config (FrameworkConfig immutable, ConfigLoader cascade, PlaceholderResolver) — Tasks 3,4,5 ✔
- context (DriverContext, ContextManager ThreadLocal) — Task 9 ✔
- spi (ServiceRegistry ServiceLoader + cache) — Task 6 ✔
- events (EventBus, TestEvent, EventListener, copy-on-write) — Task 7 ✔
- parallel (DevicePool BlockingQueue, DeviceLease) — Task 8 ✔
- lifecycle (DriverLifecycle, evidence-before-teardown ordering) — Task 10 ✔
- base (BaseTest template method, BaseScreen facade) — Task 11 ✔
- exception (typed tree incl. ElementNotFoundException carrying candidates) — Task 2 ✔
- DriverProvider SPI so engine is testable without a real device — Task 9 ✔
- DSA choices (BlockingQueue, ConcurrentHashMap cache, CopyOnWriteArrayList) — Tasks 6,7,8 ✔
- Error handling (fail-fast config/secret, contained runtime, no silent catches) — Tasks 2,4,5,7 ✔
- Token-optimization note documented — Task 12 DOCS.md ✔

**Deferred to later module plans (correctly out of core scope):** real Android/iOS driver creation + AppSource (drivers plan), local device discovery feeding DevicePool (devices plan), TestNG `IRetryAnalyzer` wiring + suite XML parallelism (exec wiring in the drivers/examples plan where a real DriverProvider exists), LRU knowledge cache + success-ranked locators (knowledge/locators plans). RetryPolicy value type is defined here; its TestNG enforcement lands when a runnable suite exists.

**2. Placeholder scan:** No TBD/TODO/"add error handling"/"similar to". Every code step shows complete code. ✔

**3. Type consistency:** `FrameworkConfig`, `Execution(Mode,ParallelBy,int)`, `Capture(When,When,boolean,boolean)`, `RetryPolicy(boolean,int,List,int)`, `DeviceLease(String,int,String)`, `DriverContext.builder().driver().device().config().build()`, `ContextManager.set/current/isSet/clear`, `EventBus.subscribe/emit`, `DevicePool.acquire(long,TimeUnit)/release`, `DriverProvider.create(Platform,Capabilities)`, `DriverLifecycle(DevicePool,DriverProvider,EventBus).start(...)/stop(boolean)`. Names used consistently across all tasks and tests. ✔

---

## Done criteria

`mvn test` is green; `core` exposes config loading, SPI registry, event bus, device pool, per-thread context, driver lifecycle, and base classes; the engine is fully unit-tested against a fake driver with no device, emulator, or network required. This module is the foundation the `secrets`, `drivers`, `devices`, `locators`, `actions`, `observability`, and `reporting` plans build on next.
