<h1>
  <img src="https://raw.githubusercontent.com/aizika/OmsTest/master/src/main/resources/META-INF/GradleTest.png" alt="Plugin Icon" width="40" style="vertical-align: middle;"/>
  Gradle Test Runner IntelliJ Plugin
</h1>

This plugin enhances IntelliJ IDEA with convenient actions to run Gradle tests by method or class. It also includes a one-click option to re-run the last test.
Tests could be run against SUV or locally running OMS. 

*It was tested very lightly, so please use it at your own risk and report any issues you find.*

---

## 🔌 Download the Plugin

You can download the latest version of the plugin from the link below and install it manually in IntelliJ:

👉 [Download Plugin ZIP](https://github.com/aizika/OmsTest/releases/download/v1.0-beta/OmsTest-1.0-BETA.zip)
Follow the installation instructions below to set it up in IntelliJ IDEA.

---

## ✨ Features

* ✅ Context-aware Gradle test execution:
    * Run the current test **method**
    * Run the current test **class**
* ✅ clickable icons in the IntelliJ gutter
* ✅ Re-run the **last test** with one click or shortcut
* ✅ Works in the **editor**, **Project**, and **Structure** views
* ✅ Outputs results to the **Run tool window**
* ✅ Fully compatible with **IntelliJ Community Edition 2024.1+**

---

## 🖱 Context Menu Usage

Right-click inside a test file in editor, on a class node in **Project** view (`⌘1`), or on a method in the **Structure** view (`⌘7`) to access the context menu. The plugin adds several options for running tests:

```
📂 OMS Gradle Test
│ Remote SUV
├─ ▶️ Run Remote Test (Method)  ⌃⌥⌘M   
├─ ▶️ Run Remote Test (Class)   ⌃⌥⌘C
│ Local OMS
├─ ▶️ Run Test (Method)         ⇧⌥⌘M
├─ ▶️ Run Test (Class)          ⇧⌥⌘C
│---------------------
└─ 🔁 Re-Run Last Test         ⌃⌥⌘R
```
There is limited verification of the context menu items, so they may not appear in all cases. And it's user's responsibility to ensure they are used in the correct context. Recommended shortcuts are provided for convenience.
You can re-run the last executed test with a single click or shortcut.
Same rules apply to remote and local tests.

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

### 📦 Installation
1. Open **IntelliJ IDEA**
2. Go to **Settings → Plugins**
3. Click the **⚙️ (gear icon)** → **Install Plugin from Disk...**
4. Choose the downloaded `.zip` file
5. Restart IntelliJ when prompted

---

## 🔁 Re-Run Last Test

Quickly re-run the most recently executed test — local or remote — with a single click or shortcut. This is especially helpful for fast iterations during development and debugging.

A useful 🎯 **Shortcut (macOS):** `⌃⌥⌘R` (Control + Option + Command + R)
No need to reselect anything — just hit the button and go.

## 🏠Local Test Running
Use the Context Menu to run tests locally on your machine.

## 🌐 Remote Test Running
Use the Context Menu to run tests remotely on SUVs via JMX. The test methods or classes will be executed on a remote server, and the logs will be downloaded to the base directory of your project on the local machine.

### Gutter Icons for Remote OMS Tests

The clickable **gutter icons** appear in the editor next to test classes and methods. These icons allow you to trigger **Remote OMS test execution** directly from the code.
### ![Class Icon](https://github.com/aizika/OmsTest/blob/master/src/main/resources/icons/omsTestMethodIcon.svg?raw=true) Method Test Icon
- Appears next to any method annotated with `@Test` (JUnit 4 or 5).
- Clicking the icon runs that specific method on the remote OMS server.

### ![Class Icon](https://github.com/aizika/OmsTest/blob/master/src/main/resources/icons/omsTestClassIcon.svg?raw=true) Class Test Icon
- Appears next to any class that contains test methods.
- Clicking the icon runs the entire test class on the remote OMS server.

### 🔧 Tips
- Icons only appear in Java files inside the editor (and will not show up in Structure or Project views).
- Make sure the method/class is properly structured and annotated for the icon to appear.
### 🛠 Remote Run Requirements

Ensure the remote server is accessible via SSH and has the necessary JMX tools installed. The plugin uses `ssh` and `scp` commands to execute tests and retrieve logs.

### 📋 Example Workflow

1. Select a test method or class.
2. Right-click and choose the appropriate "Run Remote Test" option.
3. Enter the remote host details when prompted (e.g., `i-09b8e9ee004730f12.workdaysuv.com`, `i-09b8e9ee004730f12` or something like `https://i-09b8e9ee004730f12.workdaysuv.com/wday/authgwy/oms/login.htmld').
4. View the test execution logs in the **Run tool window**.

### 📝 Notes
Running remote tests requires one of the **OmsTestCategories**. The plugin takes the category from the class annotation.
Running tests on packages is not supported. Among other things to avoid the hassle of choosing the category.

---
## 🔧 Compatibility

* Tested on IntelliJ IDEA 2024.1+
* Tested on Community (IC); should work on Ultimate (IU)
* Tested on macOS; should work cross-platform

---
## 👨‍💻 Author
alexander.aizikivsky
