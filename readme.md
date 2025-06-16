<h1>
  <img src="https://raw.githubusercontent.com/aizika/OmsTest/master/src/main/resources/META-INF/GradleTest.png" alt="Plugin Icon" width="40" style="vertical-align: middle;"/>
  Gradle Test Runner IntelliJ Plugin
</h1>

This plugin enhances IntelliJ IDEA with convenient right-click actions to run Gradle tests by method, class, or package. It also includes a one-click option to re-run the last test.
It was tested very lightly, so please use it at your own risk and report any issues you find.

---

## 🔌 Download the Plugin

You can download the latest version of the plugin from the link below and install it manually in IntelliJ:

👉 [Download Plugin ZIP](https://files.slack.com/files-pri/T7U335QS3-F091HNGPVRA/download/omstest-1.0-snapshot.zip?origin_team=E7U335QS3)

### 📦 Installation
1. Open **IntelliJ IDEA**
2. Go to **Settings → Plugins**
3. Click the **⚙️ (gear icon)** → **Install Plugin from Disk...**
4. Choose the downloaded `.zip` file
5. Restart IntelliJ when prompted

---

## ✨ Features

* ✅ Context-aware Gradle test execution:

    * Run the current test **method**
    * Run the test **class**
    * Run all tests in a **package**
* ✅ Re-run the **last test** with one click or shortcut
* ✅ Works in both the **editor** and **project view**
* ✅ Outputs results to the **Run tool window**
* ✅ Fully compatible with **IntelliJ Community Edition 2024.1+**

---

## 🖱 Context Menu Usage

Right-click inside a test file or on a package folder:

```
📂 OMS Gradle Test
├─ Run Test (Method)
├─ Run Test (Class)
├─ Run Test (Package)
└─ Re-run Last Test
```
There is a limited verification of the context menu items, so they may not appear in all cases.
But it's user's responsibility to ensure they are used in the correct context.

---

## ⚙️ Gradle Task Setup

Ensure your project has a test runner task like described in the [Running tests with runTestJmx task via IntelliJ](https://oms.workday.build/omsdev/getting-started/running-server-tests-with-jmx/#running-tests-with-runtestjmx-task-via-intellij) of OMS Encyclopedia.
If you use the default Gradle test task, no additional setup is needed.

---

## 🚀 Build the Plugin

```bash
./gradlew clean buildPlugin
```

Find the plugin ZIP under:

```
build/distributions/
```

---

## 📥 Installation

1. Open IntelliJ IDEA
2. Go to **Settings → Plugins → ⚙️ → Install Plugin from Disk...**
3. Select the generated ZIP file
4. Restart IntelliJ if prompted

---

## ⌨️ Keyboard Shortcuts

Assign shortcuts under:

```
Settings → Keymap → Plugins → Gradle Test Runner
```

Recommended mappings:

* Run Test (Method): `⌃⇧⌘M`
* Run Test (Class): `⌃⇧⌘C`
* Run Test (Package): `⌃⇧⌘P`
* Re-run Last Test: `⌃⇧⌘R`

---

## 🔧 Compatibility

* Tested on IntelliJ IDEA 2024.1+
* Tested on Community (IC); should work on Ultimate (IU)
* Tested on macOS; should work cross-platform

---

## 👨‍💻 Author

alexander.aizikivsky

Found bugs? Have ideas? Want enhancements? Open an issue or PR!
