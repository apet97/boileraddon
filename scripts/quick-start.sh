#!/usr/bin/env bash
#
# Clockify Add-on - Quick Start Script
#
# Zero-configuration quick start for first-time users.
# This script will:
# 1. Fix Java to version 17
# 2. Build the project
# 3. Let you choose which addon to run
# 4. Run it with sensible defaults
#
# Usage:
#   ./scripts/quick-start.sh
#

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Helper functions
fail() {
    echo -e "${RED}✗ $1${NC}"
    exit 1
}

pass() {
    echo -e "${GREEN}✓ $1${NC}"
}

warn() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

header() {
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

clear

cat << "EOF"
   ____ _            _    _  __
  / ___| | ___   ___| | _(_)/ _|_   _
 | |   | |/ _ \ / __| |/ / | |_| | | |
 | |___| | (_) | (__|   <| |  _| |_| |
  \____|_|\___/ \___|_|\_\_|_|  \__, |
                                |___/
  Add-on Quick Start
EOF

header "Environment Check"

# Step 1: Check and fix Java 17
info "Checking Java version..."

if ! command -v java &> /dev/null; then
    fail "Java not found!\n\nInstall Java 17 with:\n  brew install openjdk@17\n\nThen run this script again."
fi

CURRENT_JAVA=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')

if [[ ! "$CURRENT_JAVA" =~ ^17\. ]]; then
    warn "Current Java: $CURRENT_JAVA (need Java 17)"

    # Try to fix it
    if [ -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
        info "Found Java 17 installation, switching..."
        export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
        export PATH="$JAVA_HOME/bin:$PATH"

        NEW_JAVA=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
        if [[ "$NEW_JAVA" =~ ^17\. ]]; then
            pass "Switched to Java 17"
        else
            fail "Could not switch to Java 17"
        fi
    else
        fail "Java 17 not found!\n\nInstall with:\n  brew install openjdk@17\n  sudo ln -sfn \$(brew --prefix)/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk\n\nThen add to ~/.zshrc:\n  export JAVA_HOME=\"/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home\"\n  export PATH=\"\$JAVA_HOME/bin:\$PATH\""
    fi
else
    pass "Java 17 detected: $CURRENT_JAVA"
fi

# Step 2: Check Maven
info "Checking Maven..."

if ! command -v mvn &> /dev/null; then
    fail "Maven not found!\n\nInstall with:\n  brew install maven\n\nThen run this script again."
fi

pass "Maven found: $(mvn -version | head -n 1 | awk '{print $3}')"

# Step 3: Build project
header "Building Project"

cd "$(dirname "$0")/.."

info "This will take about 1-2 minutes on first run..."
echo ""

if mvn clean package -DskipTests -q; then
    pass "Build successful!"
else
    fail "Build failed. Check error messages above."
fi

# Step 4: Choose addon
header "Choose Add-on to Run"

echo "Available add-ons:"
echo ""
echo "  1) rules              - IFTTT-style automation engine"
echo "  2) auto-tag-assistant - AI-powered time entry tagging"
echo "  3) overtime           - Overtime tracking and notifications"
echo ""

read -p "Enter number (1-3) [1]: " choice
choice=${choice:-1}

case $choice in
    1)
        ADDON="rules"
        ;;
    2)
        ADDON="auto-tag-assistant"
        ;;
    3)
        ADDON="overtime"
        ;;
    *)
        warn "Invalid choice, defaulting to 'rules'"
        ADDON="rules"
        ;;
esac

pass "Selected: $ADDON"

# Step 5: Check for ngrok
header "Network Configuration"

if command -v ngrok &> /dev/null && pgrep -x "ngrok" > /dev/null; then
    info "ngrok is running!"

    # Try to detect URL
    NGROK_URL=$(curl -s http://localhost:4040/api/tunnels 2>/dev/null | grep -o '"public_url":"[^"]*' | grep https | head -n 1 | cut -d '"' -f 4)

    if [ -n "$NGROK_URL" ]; then
        pass "Detected ngrok URL: $NGROK_URL"
        USE_NGROK=true
        BASE_URL="$NGROK_URL/$ADDON"
    else
        warn "Could not detect ngrok URL"
        USE_NGROK=false
        BASE_URL="http://localhost:8080/$ADDON"
    fi
else
    info "ngrok not detected (optional)"
    info "Running locally at http://localhost:8080"
    USE_NGROK=false
    BASE_URL="http://localhost:8080/$ADDON"
fi

# Step 6: Find JAR
JAR_PATH=$(find "addons/$ADDON/target" -name "*-jar-with-dependencies.jar" 2>/dev/null | head -n 1)

if [ -z "$JAR_PATH" ] || [ ! -f "$JAR_PATH" ]; then
    fail "JAR not found. This shouldn't happen after a successful build."
fi

# Step 7: Run addon
header "Starting $ADDON Add-on"

export ADDON_PORT=8080
export ADDON_BASE_URL="$BASE_URL"

echo -e "${GREEN}Configuration:${NC}"
echo -e "  Port:      ${BLUE}8080${NC}"
echo -e "  Base URL:  ${BLUE}$BASE_URL${NC}"
echo ""

echo -e "${GREEN}Endpoints:${NC}"
echo -e "  Manifest:  ${BLUE}$BASE_URL/manifest.json${NC}"
echo -e "  Health:    ${BLUE}$BASE_URL/health${NC}"

if [ "$ADDON" = "rules" ]; then
    echo -e "  Settings:  ${BLUE}$BASE_URL/settings${NC}"
    echo -e "  IFTTT:     ${BLUE}$BASE_URL/ifttt${NC}"
fi

echo ""

if [ "$USE_NGROK" = true ]; then
    echo -e "${GREEN}To install in Clockify:${NC}"
    echo -e "  1. Go to: ${BLUE}Admin → Add-ons → Install Custom Add-on${NC}"
    echo -e "  2. Enter: ${BLUE}$BASE_URL/manifest.json${NC}"
    echo ""
else
    warn "Running locally without ngrok"
    info "For Clockify integration, start ngrok: ngrok http 8080"
    info "Then restart this script"
    echo ""
fi

info "Starting addon..."
info "Press Ctrl+C to stop"
echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

exec java -jar "$JAR_PATH"
