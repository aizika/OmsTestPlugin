<h1>
  <img src="https://raw.githubusercontent.com/aizika/OmsTest/master/src/main/resources/META-INF/GradleTest.png" alt="Plugin Icon" width="40" style="vertical-align: middle;"/>
  Gradle Test Runner IntelliJ Plugin
</h1>

This plugin enhances IntelliJ IDEA with convenient actions to run Gradle tests by method or class. It also includes a one-click option to re-run the last test.
Tests could be run against SUV or locally running OMS. 

*It was tested very lightly, so please use it at your own risk and report any issues you find.*

---
## 🚀 Get the Plugin Running
You can download the pre-built ZIP file:
👉 [Download Plugin ZIP](https://github.com/aizika/OmsTest/releases/download/v1.1-beta/OmsTest-1.1-BETA.zip)

or build it from source:

Run
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
4. Choose the built or downloaded `.zip` file
5. Restart IntelliJ when prompted

---

## ✨ Features

* ✅ Context-aware Gradle test execution:
    * Run the current test **method**
    * Run the current test **class**
* ✅ clickable icons in the IntelliJ gutter
* ✅ Works in the **editor**, **Project**, and **Structure** views
* ✅ Re-run the **last test** with one click or shortcut
* ✅ Outputs results to the **Run tool window**

---

## Running Tests

### Context-Aware Test Execution
#### ⚠️ All the actions depend on the context of the current file or selection. Re-Run Last Test is the only exception.
**Test class** from the plugin perspective is a class annotated with `@Tag(OmsTestCategory)` except for `@Tag(OmsTestCategory.UNIT)`.
Tested with `@Tag(OmsTestCategory.OMSBI)`

**Test method** is a method in a test class annotated with `@Test`, `@ParameterizedTest`, or `@RepeatedTest`.

### Gutter Menu Integration

The plugin extends IntelliJ’s **native gutter icons** for test classes and methods by adding custom OMS actions directly into the standard popup menu.

When you click the green triangle gutter icon next to a test method or class, IntelliJ displays its default test execution options. The plugin injects additional **OMS-specific actions** into this menu:

```
Run 'UnionParsingTest.testUnion'               |
Debug 'UnionParsingTest.testUnion'             |  Native IntelliJ actions
Run 'UnionParsingTest.testUnion' with Coverage |
Modify Run Configuration...                    |

OMS                                            
├─ ▶️ Run Method on SUV                                |   Added by the plugin
└─ ▶️ Run Method on Local                              |
```
This is convenient, but not 100% reliable. The plugin attempts to add these actions only when it detects a valid test method or class. If the actions do not appear, you can still use the context menu options described below.

### Context Menu Usage

Right-click inside a test file in editor, on a class node in **Project** view (`⌘1`), or on a method in the **Structure** view (`⌘7`) to access the context menu. The plugin adds several options for running tests.
They appear when the context is appropriate (e.g., when a test class or method is selected).

```
📂 OMS Gradle Test
│ Remote SUV
├─ ▶️ Run Remote Test (Method)  ⌃⌥⌘M   
├─ ▶️ Run Remote Test (Class)   ⌃⌥⌘C
│ Local OMS
├─ ▶️ Run Local Test (Method)         ⇧⌥⌘M
├─ ▶️ Run Local Test (Class)          ⇧⌥⌘C
│---------------------
└─ 🔁 Re-Run Last Test         ⌃⌥⌘R
```
You can re-run the last executed test with a single click or shortcut.
Same rules apply to remote and local tests.


### Resolving test category
Running remote tests requires one of the **OmsTestCategories**. The plugin takes the category from the class annotation.

Running tests on packages is not supported. (Among other things to avoid the hassle of choosing the category 😉).

### 🔁 Re-Run Last Test

Quickly re-run the most recently executed test — local or remote — with a single click or shortcut. This is especially helpful for fast iterations during development and debugging.

A useful **Shortcut (macOS):** `⌃⌥⌘R` (Control + Option + Command + R)
No need to reselect anything — just hit the button and go.


### Test Panel Overview
Tests run through the plugin appear in the standard **Run tool window**, using IntelliJ’s test UI infrastructure.
Attempts to mimic the native IntelliJ test runner experience as closely as possible.
One known issue is the rerun button visual appearance (something like 🎛️), which does not match the native look.
- ✅ The **left panel** shows a structured test tree  
  Managed by `TestResultsViewer`, backed by a hierarchy of `SMTestProxy` nodes.
- ✅ The **right panel** shows test output and stack traces  
  Powered by `ConsoleViewImpl`, it displays logs, failure messages, and raw output.
- ✅ Both panels are displayed in a split view managed by `SMTRunnerConsoleView`.


Tests results are taken from an XML file generated by the Gradle test runner task, which is parsed and displayed in the UI.
Failed and ignored tests display the reasons of failure or ignore status, including stack traces and messages.
---
The functionality is based on:
- Local OMS [Running tests with runTestJmx task via IntelliJ](https://oms.workday.build/omsdev/getting-started/running-server-tests-with-jmx/#running-tests-with-runtestjmx-task-via-intellij) of OMS Encyclopedia.
- SUV: [Clinton's script](https://confluence.workday.com/display/~kyle.l.harris/Running+OMS+Server+Tests+on+an+SUV?focusedCommentId=750401483#comment-750401483)
---

## 🔧 Compatibility

* Tested on IntelliJ IDEA 2024.1+
* Tested on Community (IC); should work on Ultimate (IU)
* Tested on macOS; should work cross-platform

---
## 👨‍💻 Author
alexander.aizikivsky
