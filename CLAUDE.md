# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an IntelliJ IDEA plugin that enables running OMS integration tests directly from the IDE against a running ORS (OMS Runtime Server) instance — either locally or on a remote SUV host. Built with Java 17 and the IntelliJ Platform Plugin SDK.

## Build Commands

```bash
# Build the plugin ZIP (output: build/distributions/)
./gradlew clean buildPlugin

# Launch IntelliJ IDEA with the plugin loaded for manual testing
./gradlew runIde
```

There are no automated unit tests. Plugin testing is done manually via `runIde`.

**Installation after build:** Settings → Plugins → gear icon → Install Plugin from Disk → select the `.zip` from `build/distributions/`

## Architecture

The plugin adds four OMS-specific actions into IntelliJ's native test gutter menu and a re-run shortcut (⌃⌥⌘R on macOS).

### Gutter Menu Actions (in order)

| Action | Target | Description |
|--------|--------|-------------|
| Run X **(SUV JMX)** | method / class | SSH + jmxterm to ORS container on a SUV host |
| **Test Package (SUV JMX)** | method / class | Same path, but runs the whole package; auto-detects package and `@Tag` category from the PSI class — only asks for the SUV host |
| Run X **(Local JMX)** | method / class | JMX to a locally-running ORS |
| Run X **(RemoteJ)** | method / class | `./gradlew :oms-application:remoteServerTest` via Gradle |

### Execution Flow

1. **`GutterMarkerContributor`** (actions/) — detects test methods/classes via PSI and injects OMS actions into the gutter popup; extracts package name and `@Tag(OmsTestCategories.*)` constant value via PSI resolution for the package action
2. **`TestTargetExtractor`** (target/) — PSI helpers: `isOmsTestClass`, `isTestLikeMethod`
3. **`ParamBuilder`** (execution/) — builds the 5-element JMX params array (`testMethod`, `testClass`, `testPackage`, `testConcurrent`, `testCategory`) passed to `JunitTestListener.executeTestSuite`
4. **`TestRunner`** (execution/) — logs a `Testing: (class=..., method=..., package=...), category=...` header, then orchestrates execution via a `RunStrategy`
5. **Strategy** executes the test; results are parsed by `TestResultPresenter`

### Two Execution Strategies

**RemoteJ (`RemoteJRunStrategy`)** — runs `./gradlew :oms-application:remoteServerTest` with JMX params; communicates with ORS on port 12701. Works for local ORS or SSH-tunneled SUV. Parses `##OMS|` events emitted by `tc-listener.gradle` (an init script injected into the Gradle build).

**ORS (`OrsRunStrategy`)** — connects via SSH to `root@<suv-host>`, locates the ORS JVM by JMX port 12096, runs `jmxterm-1.0-SNAPSHOT-uber.jar` (must be at `/usr/local/bin/` on SUV) inside the ORS PID namespace, stores results in `/tmp/testout` (ORS) / `/root/testout` (host), SCPs results back. No Gradle overhead.

### Stop / Cancel

The Run tool window toolbar contains a **Stop** button. Clicking it:
1. Kills the local SSH/jmxterm process (`BypassTestExecutor.cancel()`)
2. Opens a separate SSH connection and calls `cancelRunningTest` on `JunitTestListener` via jmxterm — this clears the `RUNNING_TEST_THREAD` lock so the next run is not blocked with "another test is running"

### Event Pipeline

`tc-listener.gradle` (Groovy, injected as Gradle init script) emits `##OMS|EVENT_TYPE|...` lines → `OmsEventParser` converts them to TeamCity `##teamcity[...]` service messages → `TestResultPresenter` renders them in the Run tool window with clickable stack traces.

### State / Context

- **`LastTestStorage`** — persists per-tab test execution context (host, params, strategy) and host history (up to 10 entries) across IDE restarts
- **`HostPromptDialog`** — dialog with editable dropdown for SUV hostname entry; pre-selects most recent host
- **`Locations`** — constants for result file paths on ORS, SUV, and localhost

### Key Packages

| Package | Responsibility |
|---------|---------------|
| `actions/` | Gutter icon contribution and re-run action |
| `execution/` | Run strategies, event parsing, JMX/SSH execution |
| `common/` | State persistence, SSH probing, host dialog, path constants |
| `ui/` | Run tool window rendering, XML result parsing |
| `target/` | PSI-based test target extraction |

## Plugin Manifest

`src/main/resources/META-INF/plugin.xml` declares the plugin ID (`com.workday.plugin.omstest`), the gutter contributor extension point, the notification group, and the re-run action with its keyboard shortcut.

`src/main/resources/tc-listener.gradle` is the Groovy Gradle listener script — it hooks `beforeSuite`, `afterSuite`, `beforeTest`, `afterTest` and emits `##OMS|` events. Escaping of special characters is handled in the Java parser, not here.

## IntelliJ Platform Notes

- Targets IntelliJ IDEA 2024.1+ (Community and Ultimate), `sinceBuild = 241`
- Depends on bundled `java` and `gradle` plugins
- Uses `GutterMarkersContributor` to extend (not replace) native test gutter icons
- Interacts with IntelliJ's Run tool window via `ProcessHandler` and `ConsoleView`

## Future: Replace RemoteJ Gradle with Direct Distributor Protocol

RemoteJ currently runs `./gradlew :oms-application:remoteServerTest` which adds ~30s of Gradle startup overhead. The actual test execution uses a lightweight HTTP protocol (the "distributor protocol") that can be called directly.

### How the Distributor Protocol Works

```
Client (reply port, ephemeral)     ORS (port 12090)
  |                                   |
  |-- HTTP POST /ors/execute_remote_junit
  |   {replyPort: <port>,             |
  |    discoverySelectors: [...]}     |
  |                                   |
  |   <--- TEST_PLAN_STARTED ---------|
  |   <--- EXECUTION_STARTED ---------|  (per test)
  |   <--- EXECUTION_FINISHED --------|  (per test, with result)
  |   <--- TEST_PLAN_FINISHED --------|
```

### Key Classes (from `junit-distributor-launcher:1.1.5` / `runner:1.1.5`)

- **Package**: `com.workday.junit.distributor.messages`
- `JunitDistributorDiscoveryRequest(replyPort, List<DiscoverySelectorData>)` — request body, deserialized via Jackson `ObjectMapper.readValue()`
- `DiscoverySelectorData(type, value, className, methodName, methodParameterTypes)` — test selector
- `DistributorEventHandlingServer(eventHandler, configParams)` — reply port HTTP server (**protected constructor** — must subclass or build own server with `com.sun.net.httpserver.HttpServer`)
- `AbstractJUnitDistributorTestEvent` with `Type` enum: `EXECUTION_STARTED`, `EXECUTION_FINISHED`, `EXECUTION_SKIPPED`, `TEST_PLAN_STARTED`, `TEST_PLAN_FINISHED`, etc.
- `ExecutionFinishedEvent` has `TestExecutionResult` (status + optional throwable)
- `JunitDistributorHttpUtil.sendPostRequest(url, jsonBody)` — sends HTTP POST

### ORS Server-Side Handler

`RemoteJunitRequestHandlerProvider` (in `omsbase/src/test/java/com/workday/oms/server/`) registers at path `execute_remote_junit` for `DEV_LIKE_V2` environments. It uses `ObjectMapper.readValue(requestString, JunitDistributorDiscoveryRequest.class)` to parse the request, then delegates to `DistributorTestRunner.runTests(request)` via ServiceLoader.

### Library Coordinates

From `gradle/libs.versions.toml` in OMS/oms:
```
managed_workday_remote_junit_launcher = { module = "com.workday.remotejunit:junit-distributor-launcher", version.ref = "com_workday_remote_junit_consume"}
managed_workday_remote_junit_runner = { module = "com.workday.remotejunit:junit-distributor-runner", version.ref = "com_workday_remote_junit_consume"}
```

### Gradle Properties Used by Current RemoteJ

```
-Pjunit.platform.engine.distributor.server.path=ors/execute_remote_junit
-Pjunit.platform.engine.distributor.server.port=12090
-Pjunit.platform.engine.distributor.reply.port=43096
```

### Implementation Notes

- The source repository for the distributor libraries is not indexed in Unblocked — exact field names/Jackson annotations for the request and event classes are unknown. Before implementing, decompile the JARs from Artifactory or find the source repo.
- The `DistributorEventHandlingServer` has a **protected constructor**. Options: subclass it, or build a custom reply server using `com.sun.net.httpserver.HttpServer`.
- Dependency risks: `okhttp3` / `jackson-databind` versions may conflict with IntelliJ's bundled versions. Test with `runIde`.
- Alternative: implement the protocol entirely without the library dependencies using `java.net.http.HttpClient` + `com.sun.net.httpserver.HttpServer` + IntelliJ's bundled Jackson, once the JSON wire format is confirmed.
