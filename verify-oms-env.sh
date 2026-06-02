#!/bin/zsh
# verify-oms-env.sh — Verify (and optionally fix) the local OMS development environment.
# Based on https://oms.workday.build/omsdev/getting-started/
#
# Usage:
#   ./verify-oms-env.sh [OMS_ROOT]
#
# If OMS_ROOT is not provided and the current directory is not an OMS repo root,
# the script will prompt, suggesting ~/code/oms as the default.

set -euo pipefail

# ── Colours & helpers ──────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'

pass()  { echo "${GREEN}  [PASS]${NC} $1"; }
fail()  { echo "${RED}  [FAIL]${NC} $1"; }
warn()  { echo "${YELLOW}  [WARN]${NC} $1"; }
info()  { echo "${CYAN}  [INFO]${NC} $1"; }
header(){ echo ""; echo "${CYAN}═══ $1 ═══${NC}"; }

ERRORS=0
WARNINGS=0

record_fail() { ((ERRORS++)) || true; }
record_warn() { ((WARNINGS++)) || true; }

# Prompt user; defaults to "no" when stdin is not a terminal
ask_yn() {
    if [[ -t 0 ]]; then
        echo -n "$1 [y/N]: "
        read -r REPLY
        [[ "$REPLY" =~ ^[Yy]$ ]]
    else
        return 1
    fi
}

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 1 — Software Install (formal — verify only)
# https://oms.workday.build/omsdev/getting-started/software-install/
# ══════════════════════════════════════════════════════════════════════════════
header "1. Software Install Verification"

# ── Homebrew ──
if command -v brew &>/dev/null; then
    pass "Homebrew installed ($(brew --version | head -1))"
else
    fail "Homebrew not found — see https://brew.sh"; record_fail
fi

# ── Java 17 ──
if command -v java &>/dev/null; then
    JAVA_VER=$(java -version 2>&1 | head -1)
    if echo "$JAVA_VER" | grep -q '17\.' ; then
        pass "Java 17 found: $JAVA_VER"
    else
        warn "Java found but not version 17: $JAVA_VER"; record_warn
    fi
else
    fail "java not found on PATH"; record_fail
fi

if [[ -n "${JAVA_HOME:-}" ]]; then
    pass "JAVA_HOME is set: $JAVA_HOME"
else
    fail "JAVA_HOME is not set"; record_fail
fi

# ── Git ──
if command -v git &>/dev/null; then
    pass "Git installed ($(git --version))"
else
    fail "Git not found"; record_fail
fi

# ── Docker ──
if command -v docker &>/dev/null; then
    DOCKER_VER=$(docker --version 2>/dev/null || echo "unknown")
    pass "Docker installed ($DOCKER_VER)"
    if docker info &>/dev/null; then
        pass "Docker daemon is running"
    else
        warn "Docker daemon is not running — starting Docker Desktop..."
        open -a Docker
        echo -n "  Waiting for Docker to start"
        for i in {1..30}; do
            sleep 2
            if docker info &>/dev/null; then
                echo ""
                pass "Docker daemon started"
                break
            fi
            echo -n "."
            if (( i == 30 )); then
                echo ""
                fail "Docker daemon did not start after 60s — start Docker Desktop manually"; record_fail
            fi
        done
    fi
else
    fail "Docker not found — brew install --cask docker-desktop"; record_fail
fi

# ── Gradle properties (Artifactory credentials) ──
GRADLE_PROPS="$HOME/.gradle/gradle.properties"
if [[ -f "$GRADLE_PROPS" ]]; then
    if grep -q 'artifactory_user' "$GRADLE_PROPS" && grep -q 'artifactory_password' "$GRADLE_PROPS"; then
        pass "Artifactory credentials found in $GRADLE_PROPS"
    else
        fail "Artifactory credentials missing in $GRADLE_PROPS"; record_fail
    fi
else
    fail "$GRADLE_PROPS not found — create it with artifactory_user/artifactory_password"; record_fail
fi

# ── SSH key ──
if [[ -f "$HOME/.ssh/id_rsa" ]] || [[ -f "$HOME/.ssh/id_ed25519" ]]; then
    pass "SSH key found in ~/.ssh/"
else
    warn "No SSH key found in ~/.ssh/ — may be needed for Bitbucket/GHE"; record_warn
fi

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 2 — Resolve OMS Root
# ══════════════════════════════════════════════════════════════════════════════
header "2. OMS Repository"

is_oms_root() {
    { [[ -f "$1/settings.gradle" ]] || [[ -f "$1/settings.gradle.kts" ]]; } && [[ -d "$1/oms-deployment" ]]
}

OMS_ROOT="${1:-}"

if [[ -z "$OMS_ROOT" ]]; then
    if is_oms_root "$(pwd)"; then
        OMS_ROOT="$(pwd)"
    else
        DEFAULT_OMS="$HOME/code/oms"
        if [[ -t 0 ]]; then
            echo ""
            echo -n "  Current directory is not an OMS root. Enter OMS root [$DEFAULT_OMS]: "
            read -r USER_INPUT
            OMS_ROOT="${USER_INPUT:-$DEFAULT_OMS}"
        else
            OMS_ROOT="$DEFAULT_OMS"
        fi
    fi
fi

# Resolve to absolute path
OMS_ROOT="$(cd "$OMS_ROOT" 2>/dev/null && pwd)" || { fail "Directory not found: $OMS_ROOT"; exit 1; }

if is_oms_root "$OMS_ROOT"; then
    pass "OMS root: $OMS_ROOT"
else
    fail "$OMS_ROOT does not look like an OMS repo (no settings.gradle or oms-deployment/)"; exit 1
fi

# ── Check OMS repo is on a valid branch ──
OMS_BRANCH=$(cd "$OMS_ROOT" && git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
info "OMS branch: $OMS_BRANCH"

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 3 — Build and Load OMS
# https://oms.workday.build/omsdev/getting-started/load-oms/
# ══════════════════════════════════════════════════════════════════════════════
header "3. Build & Install Distribution"

CORS_DEV="$OMS_ROOT/oms-deployment/build/install/cors-dev"
INSTALL_CMD="./gradlew -PwdConfidenceLevel=internal installCOrsDist"

run_install_cors_dist() {
    (cd "$OMS_ROOT" && eval "$INSTALL_CMD")
    if [[ -f "$CORS_DEV/start-oms.sh" ]]; then
        pass "installCOrsDist completed successfully"
    else
        fail "installCOrsDist did not produce cors-dev distribution"; record_fail
    fi
}

if [[ -d "$CORS_DEV" ]] && [[ -f "$CORS_DEV/start-oms.sh" ]]; then
    pass "cors-dev distribution found at $CORS_DEV"

    DIST_MTIME=$(stat -f %m "$CORS_DEV/start-oms.sh")
    LAST_COMMIT_TIME=$(git -C "$OMS_ROOT" log -1 --format="%ct" 2>/dev/null || echo "0")

    if (( LAST_COMMIT_TIME > DIST_MTIME )); then
        LAST_COMMIT=$(git -C "$OMS_ROOT" log -1 --format="%h %s" 2>/dev/null || echo "unknown")
        warn "Distribution predates latest commit: $LAST_COMMIT"
        if ask_yn "  Rebuild with installCOrsDist?"; then
            run_install_cors_dist
        else
            record_warn
        fi
    else
        DIST_AGE_DAYS=$(( ( $(date +%s) - DIST_MTIME ) / 86400 ))
        pass "cors-dev distribution is up to date (${DIST_AGE_DAYS} day(s) old)"
    fi
else
    fail "cors-dev distribution not found"
    if ask_yn "  Run installCOrsDist now?"; then
        run_install_cors_dist
    else
        record_fail
    fi
fi

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 4 — Start Environment (Docker services)
# https://oms.workday.build/omsdev/getting-started/starting-environment/
# ══════════════════════════════════════════════════════════════════════════════
header "4. Environment Services (Docker)"

ENV_SCRIPT="$OMS_ROOT/env"
if [[ ! -x "$ENV_SCRIPT" ]]; then
    ENV_SCRIPT="$OMS_ROOT/oms/env"
fi

if [[ -x "$ENV_SCRIPT" ]]; then
    pass "env script found: $ENV_SCRIPT"

    # Use './env ps -a' to verify Docker services.
    # Docs: expect 8 services; tomcat and pds-properties exit after setup — the rest must be running/healthy.
    check_env_health() {
        local output
        output=$(cd "$OMS_ROOT" && "$ENV_SCRIPT" ps -a 2>/dev/null || true)
        local up_count exited_count unhealthy_count
        up_count=$(echo "$output" | grep -c ' Up ' || true)
        exited_count=$(echo "$output" | grep -c 'Exited' || true)
        # Containers that are Up but NOT healthy (exclude those without health checks like oms-mysql-ui)
        unhealthy_count=$(echo "$output" | grep ' Up ' | grep -c 'unhealthy' || true)
        local total=$(( up_count + exited_count ))

        if (( total < 8 )); then
            fail "Only $total containers found (expected 8) — $up_count up, $exited_count exited"
            return 1
        elif (( unhealthy_count > 0 )); then
            fail "$unhealthy_count container(s) are unhealthy"
            echo "$output" | grep 'unhealthy' | while read -r line; do warn "  $line"; done
            return 1
        elif (( up_count < 6 )); then
            # At least 6 must be Up (8 total minus tomcat and pds-properties)
            fail "Only $up_count containers running (expected at least 6)"
            return 1
        else
            pass "Docker services healthy ($up_count running, $exited_count exited — total $total)"
            return 0
        fi
    }

    if ! check_env_health; then
        info "Start/repair with: cd $OMS_ROOT && ./env up --wait"
        if ask_yn "  Run './env up --wait' now?"; then
            (cd "$OMS_ROOT" && "$ENV_SCRIPT" up --wait)
            info "Re-checking Docker services..."
            if ! check_env_health; then
                fail "Services still not healthy after './env up --wait'"; record_fail
            fi
        else
            record_fail
        fi
    fi
else
    fail "env script not found at $OMS_ROOT/env or $OMS_ROOT/oms/env"
    record_fail
fi

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 5 — Start OMS (ORS)
# https://oms.workday.build/omsdev/getting-started/starting-independent-oms/
# ══════════════════════════════════════════════════════════════════════════════
run_verify_up() {
    local verify_script="$CORS_DEV/verify-up.sh"
    if [[ ! -x "$verify_script" ]]; then
        fail "verify-up.sh not found — cannot verify ORS readiness"; record_fail; return
    fi
    info "Waiting for ORS to be ready (via verify-up.sh)..."
    local exit_file
    exit_file=$(mktemp)
    (cd "$OMS_ROOT" && "$verify_script" 2>&1; echo $? > "$exit_file") \
        | grep -E 'Status:|Elapsed time:' || true
    local verify_exit
    verify_exit=$(cat "$exit_file" 2>/dev/null || echo 1)
    rm -f "$exit_file"
    if (( verify_exit == 0 )); then
        pass "ORS ready"
    else
        fail "ORS did not become ready — check $CORS_DEV/logs/ors.log"; record_fail
    fi
}

start_ors() {
    if [[ ! -x "$CORS_DEV/start-oms.sh" ]]; then
        fail "start-oms.sh not found — run installCOrsDist first"; record_fail; return
    fi
    (cd "$OMS_ROOT" && "$CORS_DEV/start-oms.sh")
    sleep 3
    ORS_PID=$(ps -ef | grep 'wd.service.type=' | grep -v grep | awk '{print $2}' | head -1 || true)
    if [[ -n "$ORS_PID" ]]; then
        pass "ORS started (PID $ORS_PID)"
    else
        fail "ORS failed to start — check $CORS_DEV/logs/ors.log"; record_fail
    fi
}

header "5. OMS (ORS) Process"

ORS_PID=$(ps -ef | grep 'wd.service.type=' | grep -v grep | awk '{print $2}' | head -1 || true)

if [[ -n "$ORS_PID" ]]; then
    pass "ORS process running (PID $ORS_PID)"
    if ask_yn "  Kill and restart ORS?"; then
        info "Stopping ORS..."
        pkill -9 -f wd.tag || true
        sleep 2
        ORS_PID=""
        start_ors
    fi
else
    fail "ORS process is NOT running"
    if ask_yn "  Start ORS now?"; then
        start_ors
    else
        record_fail
    fi
fi

# Discover ports and verify readiness (whether we restarted or just found it running)
ORS_PID=$(ps -ef | grep 'wd.service.type=' | grep -v grep | awk '{print $2}' | head -1 || true)
if [[ -n "$ORS_PID" ]]; then
    JMX_PORT=$(ps -ef | grep 'wd.service.type=' | grep -v grep | grep -o 'com.sun.management.jmxremote.port=[0-9]*' | cut -d'=' -f2 | head -1 || true)
    [[ -n "$JMX_PORT" ]] && pass "JMX port: $JMX_PORT" || { fail "Could not discover JMX port"; record_fail; }

    CATALINA_BASE=$(ps -ef | grep 'wd.service.type=' | grep -v grep | grep -o 'catalina.base=[^ ]*' | cut -d'=' -f2 | head -1 || true)
    if [[ -n "$CATALINA_BASE" ]]; then
        HTTP_PORT=$(grep '^wd.connector.port=' "$CATALINA_BASE/conf/catalina.properties" 2>/dev/null | cut -d'=' -f2 || echo "12701")
    else
        HTTP_PORT=12701
    fi
    pass "HTTP connector port: $HTTP_PORT"

    run_verify_up
fi

# ══════════════════════════════════════════════════════════════════════════════
# Summary
# ══════════════════════════════════════════════════════════════════════════════
header "Summary"

if (( ERRORS == 0 && WARNINGS == 0 )); then
    echo "${GREEN}  All checks passed. OMS environment is ready for testing.${NC}"
elif (( ERRORS == 0 )); then
    echo "${YELLOW}  Passed with $WARNINGS warning(s). Review warnings above.${NC}"
else
    echo "${RED}  $ERRORS error(s), $WARNINGS warning(s). Fix errors above before running tests.${NC}"
fi
echo ""
