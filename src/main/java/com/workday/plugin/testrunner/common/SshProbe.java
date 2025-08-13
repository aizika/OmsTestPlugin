package com.workday.plugin.testrunner.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

public final class SshProbe {

    public static final class Result {
        public final boolean reachable;   // host up & SSH reachable (auth may still fail)
        public final String  reason;      // diagnostic
        public final int     exitCode;    // ssh exit code
        public final String  stderr;      // raw stderr (for logs)

        Result(boolean reachable, String reason, int exitCode, String stderr, final String host) {
            this.reachable = reachable;
            this.reason = reason;
            this.exitCode = exitCode;
            this.stderr = stderr;
        }

        @Override public String toString() {
            return "reachable=" + reachable + ", reason=" + reason + ", exitCode=" + exitCode;
        }
    }

    /**
     * Probes SSH reachability to user@host without prompting.
     * Semantics:
     *  - exit=0                      => reachable (and you had working keys)
     *  - "Permission denied"/auth    => reachable (host up, SSH answering)
     *  - DNS/timeout/no route        => not reachable
     */
    public static Result probe(String user, String host, int timeoutSeconds) {
        String cmd = String.format(
            "ssh -o BatchMode=yes -o StrictHostKeyChecking=no -o ConnectTimeout=%d %s@%s exit",
            timeoutSeconds, user, host);
        try {
            Process p = new ProcessBuilder("/bin/bash", "-lc", cmd).start();

            StringBuilder err = new StringBuilder();
            try (BufferedReader er = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = er.readLine()) != null) err.append(line).append('\n');
            }
            int code = p.waitFor();

            String stderr = err.toString();
            String e = stderr.toLowerCase(Locale.ROOT);

            if (code == 0) {
                return new Result(true, "connected (auth ok)", code, stderr, host);
            }
            if (e.contains("permission denied") ||
                e.contains("authentication failed") ||
                e.contains("no supported authentication methods")) {
                // Host answered SSH; credentials weren’t accepted -> host is reachable.
                return new Result(true, "reachable (auth failed)", code, stderr, host);
            }
            if (e.contains("could not resolve hostname") ||
                e.contains("name or service not known")) {
                return new Result(false, "dns failure", code, stderr, host);
            }
            if (e.contains("connection timed out") ||
                e.contains("operation timed out") ||
                e.contains("no route to host") ||
                e.contains("temporary failure in name resolution")) {
                return new Result(false, "network timeout/unreachable", code, stderr, host);
            }
            if (e.contains("connection refused")) {
                // Host up but sshd closed/refused -> usually not usable for ssh.
                return new Result(false, "ssh port refused", code, stderr, host);
            }
            // Fallback: unknown error -> treat as not reachable, include stderr for logs
            return new Result(false, "unknown ssh error", code, stderr, host);

        } catch (Exception ex) {
            return new Result(false, "exception: " + ex.getMessage(), -1, ex.toString(), host);
        }
    }

    // Convenience overload
    public static Result probe(String host) { return probe("root", host, 5); }
}