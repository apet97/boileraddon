#!/usr/bin/env bash
#
# Run Clockify Rules Add-on locally with ngrok
#
# Usage:
#   ./scripts/run-rules-local.sh [options]
#
# Options:
#   --apply              Enable mutations (RULES_APPLY_CHANGES=true)
#   --skip-signature     Skip webhook signature verification (DEV ONLY)
#   --port <port>        Port to listen on (default: 8080)
#   --db                 Use database storage (requires DB_URL env)
#   --help               Show this help message
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
PORT=8080
APPLY_CHANGES="false"
SKIP_SIGNATURE="false"
USE_DATABASE="false"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --apply)
            APPLY_CHANGES="true"
            shift
            ;;
        --skip-signature)
            SKIP_SIGNATURE="true"
            shift
            ;;
        --port)
            PORT="$2"
            shift 2
            ;;
        --db)
            USE_DATABASE="true"
            shift
            ;;
        --help)
            head -n 15 "$0" | tail -n +3
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Check if JAR exists
JAR_PATH="addons/rules/target/rules-0.1.0-jar-with-dependencies.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo -e "${RED}✗ JAR not found: $JAR_PATH${NC}"
    echo -e "${YELLOW}Building Rules addon...${NC}"
    mvn clean package -pl addons/rules -am -DskipTests
fi

# Check if ngrok is running
if ! pgrep -x "ngrok" > /dev/null; then
    echo -e "${RED}✗ ngrok is not running${NC}"
    echo ""
    echo -e "${YELLOW}Please start ngrok in a separate terminal:${NC}"
    echo -e "  ${BLUE}ngrok http $PORT${NC}"
    echo ""
    echo -e "Then set the ADDON_BASE_URL environment variable:"
    echo -e "  ${BLUE}export ADDON_BASE_URL=https://your-ngrok-url.ngrok-free.app/rules${NC}"
    echo ""
    exit 1
fi

# Check if ADDON_BASE_URL is set
if [ -z "$ADDON_BASE_URL" ]; then
    echo -e "${RED}✗ ADDON_BASE_URL not set${NC}"
    echo ""
    echo -e "${YELLOW}Please set ADDON_BASE_URL with your ngrok URL:${NC}"
    echo -e "  ${BLUE}export ADDON_BASE_URL=https://your-ngrok-url.ngrok-free.app/rules${NC}"
    echo ""
    echo -e "You can find your ngrok URL in the ngrok terminal window."
    echo ""
    exit 1
fi

# Print configuration
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Clockify Rules Add-on - Local Setup${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Configuration:"
echo -e "  Base URL:       ${BLUE}$ADDON_BASE_URL${NC}"
echo -e "  Port:           ${BLUE}$PORT${NC}"
echo -e "  Apply Changes:  ${BLUE}$APPLY_CHANGES${NC}"
echo -e "  Skip Signature: ${BLUE}$SKIP_SIGNATURE${NC}"
echo -e "  Use Database:   ${BLUE}$USE_DATABASE${NC}"
echo ""

# Export environment variables
export ADDON_PORT="$PORT"
export RULES_APPLY_CHANGES="$APPLY_CHANGES"
export ADDON_SKIP_SIGNATURE_VERIFY="$SKIP_SIGNATURE"

# Warning messages
if [ "$APPLY_CHANGES" = "true" ]; then
    echo -e "${YELLOW}⚠️  WARNING: Apply mode is ENABLED - Rules will modify Clockify data${NC}"
    echo ""
fi

if [ "$SKIP_SIGNATURE" = "true" ]; then
    echo -e "${RED}⚠️  WARNING: Signature verification is DISABLED - Use only for development${NC}"
    echo ""
fi

# Database configuration
if [ "$USE_DATABASE" = "true" ]; then
    if [ -z "$DB_URL" ]; then
        echo -e "${RED}✗ DB_URL not set but --db flag provided${NC}"
        echo -e "${YELLOW}Set database environment variables:${NC}"
        echo -e "  export DB_URL=jdbc:postgresql://localhost:5432/addons"
        echo -e "  export DB_USERNAME=addons"
        echo -e "  export DB_PASSWORD=addons"
        exit 1
    fi
    echo -e "Database:       ${BLUE}$DB_URL${NC}"
    echo ""
fi

# Print endpoints
echo -e "${GREEN}Endpoints:${NC}"
echo -e "  Manifest:       ${BLUE}$ADDON_BASE_URL/manifest.json${NC}"
echo -e "  Settings UI:    ${BLUE}$ADDON_BASE_URL/settings${NC}"
echo -e "  IFTTT UI:       ${BLUE}$ADDON_BASE_URL/ifttt${NC}"
echo -e "  Health:         ${BLUE}$ADDON_BASE_URL/health${NC}"
echo -e "  Status:         ${BLUE}$ADDON_BASE_URL/status?workspaceId=<ws>${NC}"
echo ""

# Print installation instructions
echo -e "${GREEN}To install in Clockify:${NC}"
echo -e "  1. Go to: ${BLUE}Admin > Add-ons > Install Custom Add-on${NC}"
echo -e "  2. Enter manifest URL: ${BLUE}$ADDON_BASE_URL/manifest.json${NC}"
echo -e "  3. Approve scopes and install"
echo ""

# Print testing instructions
echo -e "${GREEN}To test locally:${NC}"
echo -e "  ${BLUE}curl $ADDON_BASE_URL/health${NC}"
echo -e "  ${BLUE}curl $ADDON_BASE_URL/status?workspaceId=<ws>${NC}"
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Starting Rules Add-on...${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Run the addon
java -jar "$JAR_PATH"
