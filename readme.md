<h1>
  <img src="https://ghe.megaleo.com/alexander-aizikivsky/oms-test-plugin/raw/master/src/main/resources/META-INF/oms-logo.png" alt="Plugin Icon" width="40" style="vertical-align: middle;"/>
  OMS Test Runner IntelliJ Plugin
</h1>

This plugin allows running OMS integration tests by method, class, or package from IntelliJ IDEA against a running ORS instance — either locally or on a remote SUV host.
It also includes a one-click option to re-run the last test.

*It was tested very lightly, so please use it at your own risk and report any issues you find.*

---
## 🚀 Get the Plugin

#### Download:
Get the latest plugin ZIP from the [latest release](https://ghe.megaleo.com/alexander-aizikivsky/oms-test-plugin/releases/latest).

#### Or build from source:

```
./gradlew clean buildPlugin
```

Find the plugin ZIP under:

```
build/distributions/
```
---

### 📦 Installation
1. Open **IntelliJ IDEA**
2. Go to **Settings → Plugins**
3. Click the **⚙️ (gear icon)** → **Install Plugin from Disk...**
4. Choose the built `.zip` file
5. Restart IntelliJ when prompted


### ✨ Features

* ✅ Clickable icons in the IntelliJ gutter (method or class)
* ✅ Right-click in the **Project panel** to run a class or package
* ✅ Re-run the **last test** with one click or shortcut
* ✅ Right-click any node in the test results tree to re-run it
* ✅ Outputs results to the **Run tool window**
* ✅ For failing tests, displays clickable **stack trace**

---

## ▶️ Running Tests

### Gutter Menu Integration

The plugin extends IntelliJ's **native gutter icons** for test classes and methods by adding custom OMS actions directly into the standard popup menu.

When you click the green triangle gutter icon next to a test method or class, IntelliJ displays its default test execution options. The plugin injects additional **OMS-specific actions**:

```
Run 'FormatDateSpartaTest'                         |
Debug 'FormatDateSpartaTest'                       |  Native IntelliJ actions
...                                                |
----
OMS
├─ ▶️ Run FormatDateSpartaTest (SUV JMX)            |  SSH + JMX, direct SUV access
└─ ▶️ Run FormatDateSpartaTest (Local JMX)          |  JMX, local ORS/OTS instance
```

### Mode 1: SUV JMX (SSH + JMX)

Connects directly to the SUV host via SSH, locates the ORS JVM by its JMX port (12096), creates the test output directory inside the ORS PID namespace, runs jmxterm, and SCPs results back.

**When to use:**
- Running against a **remote SUV host**
- Faster iteration — no Gradle overhead

**Requirements:**
- `jmxterm-1.0-SNAPSHOT-uber.jar` at `/usr/local/bin/` on the SUV host
- SSH access to `root@<suv-host>`

Click **Run X (SUV JMX)** — the plugin will prompt for the SUV hostname.

### Mode 2: Local JMX

Connects via JMX directly to a locally-running ORS or OTS instance. Discovers the JMX port automatically by inspecting the local Java process.

**When to use:**
- Running against a **local ORS/OTS** instance

Click **Run X (Local JMX)** — no host prompt, runs immediately.

---

### 🗂️ Project Panel Right-Click

Right-clicking a **Java test file** or a **package directory** in the Project panel shows two OMS entries directly in the context menu:

| Entry | What it does |
|-------|-------------|
| **Run (Local JMX)** | Runs the class or package against the local ORS instance |
| **Run (SUV JMX)** | Prompts for a SUV host, then runs via SSH + JMX |

The entries are hidden when the selection is not an OMS test class or package.  
Package runs additionally prompt for a test category (e.g. `OMSBI`) — the last value is pre-filled.  
Trivially shallow packages (`com`, `com.workday`) are excluded; everything from `com.workday.X` downward is supported.

---

### 🔁 Re-Run Last Test

Quickly re-run the most recently executed test with a single click or shortcut.

Shortcut (macOS): **⌃⌥⌘R** (Control + Option + Command + R)

#### Tab-Dependent Reruns

Multiple Run tabs can stay open simultaneously for different servers or configurations, each preserving its own context (test, runner mode, host). When you re-run from an existing tab, the plugin restores that tab's configuration automatically.

#### Stored Hosts & History

The Host Prompt dialog includes an editable drop-down with previously used hosts. The most recently used host is pre-selected. History persists across IDE restarts (up to 10 entries).

---

### 👀 Test Panel Overview

Results appear in the standard **Run tool window**. Failed and ignored tests display the failure reason with clickable stack traces.

The test tree groups results by package, class, and method. Intermediate single-child package nodes are collapsed (e.g. `com` → `com.workday` → `com.workday.bi` shows as `com.workday.bi`). Parameterized tests expand into an extra level showing each individual variant:

```
▼ com.workday.bi.queryables
  ▼ FormatDateSpartaTest
    ▼ testFormatDate               (method node — click to run all variants)
        testFormatDate(1)          (variant leaf)
        testFormatDate(2)
    ✓ testSimpleDate
```

#### Run Button & Right-Click

A **Run** button in the test results toolbar lets you re-execute any selected node from the test tree using the **same strategy that was used most recently** (SUV JMX or Local JMX). You can also **right-click** any node in the tree to get the same option in a context menu. This lets you quickly re-run a specific failing test without going back to the gutter menu.

---

## 🛠️ verify-oms-env.sh

`verify-oms-env.sh` is a helper script included in this repository that checks whether your local OMS development environment is set up correctly. It verifies Java, Docker, Artifactory credentials, the OMS distribution, and whether ORS is running — and offers to fix issues interactively.

**Usage:**
```bash
./verify-oms-env.sh [OMS_ROOT]
```

> ⚠️ **Use at your own risk.** This script is provided as-is with no guarantees or warranties of any kind. It may modify your local environment (start Docker, run `installCOrsDist`, start ORS). The author takes no responsibility for any issues caused by running it.

---

## 🔧 Compatibility

* Tested on IntelliJ IDEA 2024.1+
* Tested on Community (IC); should work on Ultimate (IU)
* Tested on macOS

---
## 👨‍💻 Author
[alexander.aizikivsky](https://workday.enterprise.slack.com/team/U06CQC7KAQM)
