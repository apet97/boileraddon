#!/usr/bin/env bash
#
# Clockify Add-on - Complete Setup and Run Script
#
# This script provides a one-command solution to:
# 1. Validate and fix Java 17 environment
# 2. Build the project with Maven
# 3. Configure and run any add-on
#
# Usage:
#   ./scripts/setup-and-run.sh [options]
#
# Examples:
#   ./scripts/setup-and-run.sh --addon rules
#   ./scripts/setup-and-run.sh --addon rules --clean --use-ngrok
#   ./scripts/setup-and-run.sh --validate-only
#

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Default configuration
ADDON=""
PORT=8080
CLEAN_BUILD=false
SKIP_BUILD=false
USE_NGROK=false
BASE_URL=""
APPLY_CHANGES=false
SKIP_SIGNATURE=false
USE_DATABASE=false
ENV_FILE=""
VALIDATE_ONLY=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --addon)
            ADDON="$2"
            shift 2
            ;;
        --port)
            PORT="$2"
            shift 2
            ;;
        --clean)
            CLEAN_BUILD=true
            shift
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --use-ngrok)
            USE_NGROK=true
            shift
            ;;
        --base-url)
            BASE_URL="$2"
            shift 2
            ;;
        --apply)
            APPLY_CHANGES=true
            shift
            ;;
        --skip-signature)
            SKIP_SIGNATURE=true
            shift
            ;;
        --db)
            USE_DATABASE=true
            shift
            ;;
        --env-file)
            ENV_FILE="$2"
            shift 2
            ;;
        --validate-only)
            VALIDATE_ONLY=true
            shift
            ;;
        --help)
            head -n 25 "$0" | tail -n +3
            echo ""
            echo "Options:"
            echo "  --addon <name>         Which addon to build/run (rules, auto-tag-assistant, overtime)"
            echo "  --port <port>          Port to run on (default: 8080)"
            echo "  --clean                Clean build (mvn clean)"
            echo "  --skip-build           Skip build, just run existing JAR"
            echo "  --use-ngrok            Auto-detect ngrok URL and configure addon"
            echo "  --base-url <url>       Explicit base URL (overrides ngrok)"
            echo "  --apply                Enable mutations (RULES_APPLY_CHANGES=true)"
            echo "  --skip-signature       Skip webhook signature verification (DEV ONLY)"
            echo "  --db                   Use database storage"
            echo "  --env-file <file>      Load environment from file"
            echo "  --validate-only        Only check environment, don't build/run"
            echo "  --help                 Show this help"
            exit 0
            ;;
        *)
            fail "Unknown option: $1\nUse --help for usage information"
            ;;
    esac
done

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Clockify Add-on Setup & Run${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Step 1: Detect and fix Java 17
info "Step 1: Validating Java 17 environment..."
echo ""

# Check current Java version
if command -v java &> /dev/null; then
    CURRENT_JAVA=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
    if [[ "$CURRENT_JAVA" =~ ^17\. ]]; then
        pass "Java 17 detected: $CURRENT_JAVA"
    else
        warn "Current Java version: $CURRENT_JAVA"
        warn "Project requires Java 17"

        # Try to find and use Java 17 from Homebrew
        if [ -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
            export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
            export PATH="$JAVA_HOME/bin:$PATH"

            # Verify the switch worked
            NEW_JAVA=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
            if [[ "$NEW_JAVA" =~ ^17\. ]]; then
                pass "Switched to Java 17: $NEW_JAVA"
            else
                fail "Failed to switch to Java 17. Current: $NEW_JAVA"
            fi
        else
            fail "Java 17 not found at /opt/homebrew/opt/openjdk@17\nInstall with: brew install openjdk@17"
        fi
    fi
else
    fail "Java not found. Install with: brew install openjdk@17"
fi

# Step 2: Check Maven
info "Step 2: Validating Maven..."
echo ""

if ! command -v mvn &> /dev/null; then
    fail "Maven not found. Install with: brew install maven"
fi

MVN_VERSION=$(mvn -version 2>&1 | head -n 1)
pass "Maven found: $MVN_VERSION"

# Verify Maven is using Java 17
MVN_JAVA=$(mvn -version 2>&1 | grep "Java version" | awk '{print $3}')
if [[ "$MVN_JAVA" =~ ^17\. ]]; then
    pass "Maven using Java 17: $MVN_JAVA"
else
    warn "Maven using Java $MVN_JAVA instead of Java 17"
    warn "This may cause build issues"
fi

# Step 3: Check toolchains.xml
info "Step 3: Checking Maven toolchains..."
echo ""

if [ -f "$HOME/.m2/toolchains.xml" ]; then
    if grep -q "version>17<" "$HOME/.m2/toolchains.xml"; then
        pass "Maven toolchains.xml configured for Java 17"
    else
        warn "Maven toolchains.xml exists but may not have Java 17 configured"
    fi
else
    warn "Maven toolchains.xml not found at ~/.m2/toolchains.xml"
    info "See docs/TOOLCHAINS_SETUP.md for setup instructions"
fi

# Step 4: Validate addon selection
if [ "$VALIDATE_ONLY" = false ]; then
    if [ -z "$ADDON" ]; then
        fail "No addon specified. Use --addon <name>\nAvailable: rules, auto-tag-assistant, overtime"
    fi

    ADDON_DIR="addons/$ADDON"
    if [ ! -d "$ADDON_DIR" ]; then
        fail "Addon not found: $ADDON\nDirectory does not exist: $ADDON_DIR"
    fi

    pass "Addon selected: $ADDON"
fi

# If validate-only, stop here
if [ "$VALIDATE_ONLY" = true ]; then
    echo ""
    echo -e "${GREEN}========================================${NC}"
    pass "Environment validation complete!"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    info "Your environment is ready. Run without --validate-only to build and run."
    exit 0
fi

# Step 5: Build project
echo ""
info "Step 5: Building project..."
echo ""

if [ "$SKIP_BUILD" = true ]; then
    warn "Skipping build (--skip-build specified)"
else
    # Change to project root
    cd "$(dirname "$0")/.."

    if [ "$CLEAN_BUILD" = true ]; then
        info "Running clean build..."
        mvn clean install -pl addons/addon-sdk -am -DskipTests || fail "Build failed"
        mvn clean package -pl "addons/$ADDON" -am -DskipTests || fail "Build failed"
    else
        info "Running incremental build..."
        mvn install -pl addons/addon-sdk -am -DskipTests || fail "Build failed"
        mvn package -pl "addons/$ADDON" -am -DskipTests || fail "Build failed"
    fi

    pass "Build successful"
fi

# Step 6: Find JAR file
echo ""
info "Step 6: Locating addon JAR..."
echo ""

JAR_PATH=$(find "addons/$ADDON/target" -name "*-jar-with-dependencies.jar" 2>/dev/null | head -n 1)

if [ -z "$JAR_PATH" ] || [ ! -f "$JAR_PATH" ]; then
    fail "JAR not found in addons/$ADDON/target/\nTry running with --clean to rebuild"
fi

pass "Found JAR: $JAR_PATH"

# Step 7: Configure ngrok (if requested)
echo ""
info "Step 7: Configuring addon URL..."
echo ""

if [ "$USE_NGROK" = true ]; then
    if ! command -v ngrok &> /dev/null; then
        fail "ngrok not installed. Install with: brew install ngrok"
    fi

    if ! pgrep -x "ngrok" > /dev/null; then
        fail "ngrok is not running. Start with: ngrok http $PORT"
    fi

    # Try to get ngrok URL from API
    NGROK_URL=$(curl -s http://localhost:4040/api/tunnels 2>/dev/null | grep -o '"public_url":"[^"]*' | grep https | head -n 1 | cut -d '"' -f 4)

    if [ -n "$NGROK_URL" ]; then
        BASE_URL="$NGROK_URL/$ADDON"
        pass "Detected ngrok URL: $NGROK_URL"
    else
        fail "Could not detect ngrok URL. Is ngrok running on port $PORT?"
    fi
fi

if [ -z "$BASE_URL" ]; then
    BASE_URL="http://localhost:$PORT/$ADDON"
    warn "No base URL specified, using: $BASE_URL"
    info "For Clockify integration, use --use-ngrok or --base-url"
fi

# Step 8: Set environment variables
export ADDON_PORT="$PORT"
export ADDON_BASE_URL="$BASE_URL"

if [ "$APPLY_CHANGES" = true ]; then
    export RULES_APPLY_CHANGES="true"
fi

if [ "$SKIP_SIGNATURE" = true ]; then
    export ADDON_SKIP_SIGNATURE_VERIFY="true"
fi

if [ "$USE_DATABASE" = true ]; then
    if [ -z "$DB_URL" ]; then
        warn "Database mode enabled but DB_URL not set"
        info "Set DB_URL, DB_USERNAME, DB_PASSWORD environment variables"
    fi
fi

# Load env file if specified
if [ -n "$ENV_FILE" ] && [ -f "$ENV_FILE" ]; then
    pass "Loading environment from: $ENV_FILE"
    set -a
    source "$ENV_FILE"
    set +a
fi

# Step 9: Print configuration
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Configuration Summary${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Addon:          ${BLUE}$ADDON${NC}"
echo -e "Port:           ${BLUE}$PORT${NC}"
echo -e "Base URL:       ${BLUE}$BASE_URL${NC}"
echo -e "Apply Changes:  ${BLUE}${APPLY_CHANGES}${NC}"
echo -e "Skip Signature: ${BLUE}${SKIP_SIGNATURE}${NC}"
echo -e "Use Database:   ${BLUE}${USE_DATABASE}${NC}"
echo ""

# Warnings
if [ "$APPLY_CHANGES" = true ]; then
    warn "Apply mode ENABLED - Addon will modify Clockify data!"
fi

if [ "$SKIP_SIGNATURE" = true ]; then
    warn "Signature verification DISABLED - Use only for development!"
fi

# Print endpoints
echo -e "${GREEN}Endpoints:${NC}"
echo -e "  Manifest:       ${BLUE}$BASE_URL/manifest.json${NC}"
echo -e "  Health:         ${BLUE}$BASE_URL/health${NC}"
echo -e "  Status:         ${BLUE}$BASE_URL/status?workspaceId=<ws>${NC}"

if [ "$ADDON" = "rules" ]; then
    echo -e "  Settings UI:    ${BLUE}$BASE_URL/settings${NC}"
    echo -e "  IFTTT UI:       ${BLUE}$BASE_URL/ifttt${NC}"
fi

echo ""

# Print installation instructions
if [[ "$BASE_URL" =~ ^https?:// ]]; then
    echo -e "${GREEN}To install in Clockify:${NC}"
    echo -e "  1. Go to: ${BLUE}Admin → Add-ons → Install Custom Add-on${NC}"
    echo -e "  2. Enter manifest URL: ${BLUE}$BASE_URL/manifest.json${NC}"
    echo -e "  3. Approve scopes and install"
    echo ""
fi

# Step 10: Run the addon
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Starting $ADDON addon...${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

exec java -jar "$JAR_PATH"
