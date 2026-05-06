<h1>
  <img src="https://raw.githubusercontent.com/aizika/OmsTestPlugin/master/src/main/resources/META-INF/oms-logo.png" alt="Plugin Icon" width="40" style="vertical-align: middle;"/>
  OMS Test Runner IntelliJ Plugin
</h1>

This plugin allows running OMS integration tests by method or class from IntelliJ IDEA against a running ORS instance — either locally or on a remote SUV host.
It also includes a one-click option to re-run the last test.

*It was tested very lightly, so please use it at your own risk and report any issues you find.*

---
## 🚀 Get the Plugin
Build from source:

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

* ✅ Clickable icons in the IntelliJ gutter
* ✅ Re-run the **last test** with one click or shortcut
* ✅ Outputs results to the **Run tool window**
* ✅ For failing tests, displays clickable **stack trace**

---

## ▶️ Running Tests

### Gutter Menu Integration

The plugin extends IntelliJ's **native gutter icons** for test classes and methods by adding custom OMS actions directly into the standard popup menu.

When you click the green triangle gutter icon next to a test method or class, IntelliJ displays its default test execution options. The plugin injects two additional **OMS-specific actions**:

```
Run 'FormatDateSpartaTest'                         |
Debug 'FormatDateSpartaTest'                       |  Native IntelliJ actions
...                                                |
----
OMS
├─ ▶️ Run FormatDateSpartaTest (RemoteJ)            |  Gradle-based, local or tunneled SUV
└─ ▶️ Run FormatDateSpartaTest on ORS               |  SSH + JMX, direct SUV access
```

### Mode 1: RemoteJ

Uses Gradle's `remoteServerTest` task to distribute tests via the ORS RemoteJ endpoint (port 12701).

**When to use:**
- Running against a **local ORS** instance (no configuration required)
- Running against a **SUV host via SSH tunnel** (set up the tunnel first, then run)

**Local ORS** (no setup needed — just click):
```
Run FormatDateSpartaTest (RemoteJ)
```

**SUV via SSH tunnel** — first forward the ports:
```bash
ssh -A -f -N -L 12090:localhost:12090 -R 43096:localhost:43096 root@<suv-host>
```
Then click **Run X (RemoteJ)** — the plugin will prompt for the SUV hostname.

### Mode 2: Run on ORS (SSH + JMX)

Connects directly to the SUV host via SSH, locates the ORS JVM by its JMX port (12096), creates the test output directory inside the ORS PID namespace, runs jmxterm, and SCPs results back.

**When to use:**
- Running against a **remote SUV host** without an SSH tunnel
- Faster iteration on a SUV — no Gradle overhead

**Requirements:**
- `jmxterm-1.0-SNAPSHOT-uber.jar` at `/usr/local/bin/` on the SUV host
- SSH access to `root@<suv-host>`

Click **Run X on ORS** — the plugin will prompt for the SUV hostname.

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

---

## 🔧 Compatibility

* Tested on IntelliJ IDEA 2024.1+
* Tested on Community (IC); should work on Ultimate (IU)
* Tested on macOS

---
## 👨‍💻 Author
[alexander.aizikivsky](https://workday.enterprise.slack.com/team/U06CQC7KAQM)
