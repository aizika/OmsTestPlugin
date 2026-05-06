package com.workday.plugin.testrunner.execution;

import com.workday.plugin.testrunner.ui.UiContentDescriptor;

/**
 * Parses ##OMS| structured event lines emitted by tc-listener.gradle
 * and converts them into ##teamcity[ service messages for IntelliJ's SMTestRunner.
 *
 * ##OMS| format (pipe-delimited, no escaping):
 *   SUITE_START | className | displayName
 *   SUITE_END   | className | displayName
 *   TEST_START  | className | displayName | methodName
 *   TEST_PASS   | className | displayName | methodName | durationMs
 *   TEST_FAIL   | className | displayName | methodName | durationMs | message
 *   STACK       | stackTraceLine
 *   TEST_SKIP   | className | displayName | methodName | durationMs
 *
 * @author alexander.aizikivsky
 * @since Feb-2026
 */
public class OmsEventParser {

    private static final String PREFIX = "##OMS|";

    private final UiContentDescriptor.UiProcessHandler handler;

    // Accumulate stack trace lines between TEST_FAIL and TEST_FINISH
    private String pendingFailName;
    private String pendingFailMsg;
    private final StringBuilder pendingStack = new StringBuilder();

    public OmsEventParser(UiContentDescriptor.UiProcessHandler handler) {
        this.handler = handler;
    }

    /**
     * Process one line of Gradle output.
     * Returns true if the line was an ##OMS| event (suppress from plain log),
     * false if it should be passed through as regular console output.
     */
    public boolean process(String line) {
        if (!line.startsWith(PREFIX)) {
            // Pass through — do NOT flush pending fail here, stack lines may still be coming
            return false;
        }

        String[] parts = line.substring(PREFIX.length()).split("\\|", -1);
        if (parts.length == 0) return true;

        String event = parts[0];
        switch (event) {
            case "SUITE_START" -> {
                // parts: SUITE_START | className | displayName
                String displayName = get(parts, 2);
                String className   = get(parts, 1);
                String location    = "java:" + (className.isEmpty() ? displayName : className);
                tc("testSuiteStarted name='" + esc(displayName) + "' locationHint='" + location + "'");
            }
            case "SUITE_END" -> {
                String displayName = get(parts, 2);
                tc("testSuiteFinished name='" + esc(displayName) + "'");
            }
            case "TEST_START" -> {
                // parts: TEST_START | className | displayName | methodName
                flushPendingFail();
                String className   = get(parts, 1);
                String displayName = get(parts, 2);
                String methodName  = get(parts, 3);
                String location    = "java:" + className + "#" + methodName;
                tc("testStarted name='" + esc(displayName)
                        + "' captureStandardOutput='true' locationHint='" + location + "'");
            }
            case "TEST_PASS" -> {
                // parts: TEST_PASS | className | displayName | methodName | durationMs
                flushPendingFail();
                String displayName = get(parts, 2);
                String dur         = get(parts, 4);
                tc("testFinished name='" + esc(displayName) + "' duration='" + dur + "'");
            }
            case "TEST_FAIL" -> {
                // parts: TEST_FAIL | className | displayName | methodName | durationMs | message
                // Stack lines follow as ##OMS|STACK|... — accumulate until flushed
                flushPendingFail(); // flush any previous (shouldn't happen)
                pendingFailName = get(parts, 2);
                pendingFailMsg  = get(parts, 5).replace("\\n", "\n");
                pendingStack.setLength(0);
            }
            case "STACK" -> {
                // parts: STACK | stackTraceLine
                if (pendingFailName != null) {
                    pendingStack.append(get(parts, 1)).append("\n");
                }
            }
            case "TEST_SKIP" -> {
                flushPendingFail();
                String displayName = get(parts, 2);
                String dur         = get(parts, 4);
                tc("testIgnored name='" + esc(displayName) + "'");
                tc("testFinished name='" + esc(displayName) + "' duration='" + dur + "'");
            }
        }
        return true;
    }

    /** Call at end of stream to flush any remaining pending failure. */
    public void finish() {
        flushPendingFail();
    }

    private void flushPendingFail() {
        if (pendingFailName == null) return;
        tc("testFailed name='" + esc(pendingFailName)
                + "' message='" + esc(pendingFailMsg)
                + "' details='" + esc(pendingStack.toString()) + "'");
        tc("testFinished name='" + esc(pendingFailName) + "'");
        pendingFailName = null;
        pendingFailMsg  = null;
        pendingStack.setLength(0);
    }

    private void tc(String msg) {
        handler.log("##teamcity[" + msg + "]");
    }

    /** TeamCity escaping: |, ', \n, \r, [, ] */
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("|",  "||")
                .replace("'",  "|'")
                .replace("\n", "|n")
                .replace("\r", "|r")
                .replace("[",  "|[")
                .replace("]",  "|]");
    }

    private String get(String[] parts, int index) {
        return (index < parts.length) ? parts[index] : "";
    }
}