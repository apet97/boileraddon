#!/usr/bin/env bash
#
# Production Deployment Validation Script
#
# This script validates that an addon is properly configured for production deployment.
# Addresses Problem #25-26 from ADDON-CREATION-PROBLEMS.md
#
# Usage: scripts/validate-production.sh <addon-name>
#
# Example:
#   scripts/validate-production.sh my-addon
#

set -euo pipefail

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

error() {
  echo -e "${RED}✗ $*${NC}" >&2
}

warn() {
  echo -e "${YELLOW}⚠ $*${NC}" >&2
}

success() {
  echo -e "${GREEN}✓ $*${NC}"
}

info() {
  echo "  $*"
}

if [ $# -lt 1 ]; then
  echo "Usage: $0 <addon-name>" >&2
  echo "" >&2
  echo "Example: $0 my-addon" >&2
  exit 2
fi

ADDON_NAME="$1"
ADDON_DIR="addons/$ADDON_NAME"

# Validation counters
ERRORS=0
WARNINGS=0
PASSED=0

check_pass() {
  success "$1"
  ((PASSED++))
}

check_fail() {
  error "$1"
  ((ERRORS++))
}

check_warn() {
  warn "$1"
  ((WARNINGS++))
}

echo ""
echo "========================================"
echo "Production Deployment Checklist"
echo "========================================"
echo ""
info "Addon: $ADDON_NAME"
info "Location: $ADDON_DIR"
echo ""

# Check addon exists
if [ ! -d "$ADDON_DIR" ]; then
  error "Addon directory not found: $ADDON_DIR"
  exit 1
fi

echo "1. Environment Configuration"
echo "----------------------------"

# Check for .env file
if [ -f "$ADDON_DIR/.env" ]; then
  check_warn ".env file exists (should not be committed to git)"
  info "Make sure production uses environment variables, not .env file"
else
  check_pass "No .env file in repository"
fi

# Check for .env.example or production config guide
if [ -f "$ADDON_DIR/.env.example" ] || [ -f "$ADDON_DIR/README.md" ]; then
  check_pass "Configuration documentation exists"
else
  check_warn "No .env.example or configuration guide found"
  info "Create .env.example with all required environment variables"
fi

echo ""
echo "2. Security Configuration"
echo "-------------------------"

# Check for hardcoded secrets
if grep -r "WEBHOOK_SECRET.*=.*['\"]" "$ADDON_DIR/src" >/dev/null 2>&1; then
  check_fail "Hardcoded WEBHOOK_SECRET found in source code"
  info "WEBHOOK_SECRET must be provided via environment variable"
else
  check_pass "No hardcoded webhook secrets"
fi

# Check for proper token storage
if grep -r "InMemoryTokenStore" "$ADDON_DIR/src/main/java" >/dev/null 2>&1; then
  check_warn "Using InMemoryTokenStore (not suitable for production)"
  info "Use DatabaseTokenStore for production deployments"
else
  check_pass "Not using InMemoryTokenStore"
fi

# Check for disabled signature verification
if grep -r "SKIP_SIGNATURE_VERIFY.*true" "$ADDON_DIR" >/dev/null 2>&1; then
  check_fail "Signature verification is disabled!"
  info "Remove ADDON_SKIP_SIGNATURE_VERIFY=true for production"
else
  check_pass "Signature verification not disabled"
fi

echo ""
echo "3. Build Configuration"
echo "----------------------"

# Check if addon builds
if [ -f "$ADDON_DIR/pom.xml" ]; then
  check_pass "pom.xml exists"

  # Try to build
  info "Testing build..."
  if mvn -f "$ADDON_DIR/pom.xml" clean package -DskipTests -q 2>/dev/null; then
    check_pass "Addon builds successfully"

    # Check for fat JAR
    jar_file=$(find "$ADDON_DIR/target" -name "*-jar-with-dependencies.jar" 2>/dev/null | head -1)
    if [ -n "$jar_file" ]; then
      check_pass "Fat JAR created: $(basename "$jar_file")"
    else
      check_warn "No fat JAR found (may need assembly plugin)"
    fi
  else
    check_fail "Build failed"
    info "Run: mvn -f $ADDON_DIR/pom.xml clean package"
  fi
else
  check_fail "pom.xml not found"
fi

echo ""
echo "4. Manifest Configuration"
echo "-------------------------"

manifest_file="$ADDON_DIR/manifest.json"
if [ -f "$manifest_file" ]; then
  check_pass "manifest.json exists"

  # Validate JSON
  if python3 -c "import json; json.load(open('$manifest_file'))" 2>/dev/null; then
    check_pass "manifest.json is valid JSON"

    # Check for required fields
    if python3 -c "import json; m=json.load(open('$manifest_file')); exit(0 if all(k in m for k in ['key','name','baseUrl','schemaVersion']) else 1)" 2>/dev/null; then
      check_pass "Manifest has all required fields"
    else
      check_fail "Manifest missing required fields (key, name, baseUrl, schemaVersion)"
    fi

    # Check for $schema field (Clockify rejects this)
    if grep -q '"\$schema"' "$manifest_file"; then
      check_fail "Manifest contains \$schema field (Clockify will reject this)"
      info "Remove the \$schema field from manifest.json"
    else
      check_pass "No \$schema field in manifest"
    fi

    # Check baseUrl
    base_url=$(python3 -c "import json; print(json.load(open('$manifest_file')).get('baseUrl', ''))" 2>/dev/null || echo "")
    if [[ "$base_url" == *"localhost"* ]]; then
      check_warn "Manifest baseUrl points to localhost: $base_url"
      info "Update baseUrl to your production URL before installing"
    elif [[ "$base_url" == *"ngrok"* ]]; then
      check_warn "Manifest baseUrl points to ngrok: $base_url"
      info "Update baseUrl to your production URL before installing"
    elif [ -z "$base_url" ]; then
      check_fail "Manifest baseUrl is empty"
    else
      check_pass "Manifest baseUrl configured: $base_url"
    fi
  else
    check_fail "manifest.json is not valid JSON"
  fi
else
  check_fail "manifest.json not found"
fi

echo ""
echo "5. Database Configuration"
echo "-------------------------"

# Check for database configuration references
if grep -r "DatabaseTokenStore\|DB_URL\|jdbc:" "$ADDON_DIR/src" >/dev/null 2>&1; then
  check_pass "Database configuration found in source"
  info "Ensure DB_URL, DB_USERNAME, DB_PASSWORD are set in production"

  # Check for database migration files
  if [ -d "$ADDON_DIR/db/migrations" ] || [ -d "$ADDON_DIR/src/main/resources/db" ]; then
    check_pass "Database migration files found"
  else
    check_warn "No database migration files found"
    info "Consider using Flyway for database schema management"
  fi
else
  check_warn "No database configuration found"
  info "If using persistent token storage, configure DatabaseTokenStore"
fi

echo ""
echo "6. Logging Configuration"
echo "------------------------"

logback_file="$ADDON_DIR/src/main/resources/logback.xml"
if [ -f "$logback_file" ]; then
  check_pass "logback.xml exists"

  # Check for debug mode
  if grep -q "DEBUG" "$logback_file"; then
    check_warn "logback.xml contains DEBUG level logging"
    info "Consider using INFO or WARN for production"
  else
    check_pass "No DEBUG level logging in logback.xml"
  fi

  # Check for proper log appender
  if grep -q "FILE\|ASYNC" "$logback_file"; then
    check_pass "File or async logging configured"
  else
    check_warn "Only console logging configured"
    info "Consider adding file-based logging for production"
  fi
else
  check_warn "No logback.xml found"
  info "Default logging configuration will be used"
fi

echo ""
echo "7. Health & Monitoring"
echo "----------------------"

# Check for health endpoint
if grep -r "/health" "$ADDON_DIR/src" >/dev/null 2>&1; then
  check_pass "Health endpoint configured"
else
  check_warn "No health endpoint found"
  info "Health endpoint recommended for monitoring"
fi

# Check for metrics endpoint
if grep -r "/metrics\|prometheus" "$ADDON_DIR/src" >/dev/null 2>&1; then
  check_pass "Metrics endpoint configured"
else
  check_warn "No metrics endpoint found"
  info "Metrics endpoint recommended for monitoring"
fi

echo ""
echo "8. CORS & Security Headers"
echo "--------------------------"

# Check for CORS configuration
if grep -r "ADDON_CORS_ORIGINS\|CorsFilter" "$ADDON_DIR/src" >/dev/null 2>&1; then
  check_pass "CORS configuration found"
  info "Set ADDON_CORS_ORIGINS to your production Clockify domain"
else
  check_warn "No CORS configuration found"
fi

# Check for security headers
if grep -r "SecurityHeadersFilter\|ADDON_FRAME_ANCESTORS" "$ADDON_DIR/src" >/dev/null 2>&1; then
  check_pass "Security headers configuration found"
  info "Set ADDON_FRAME_ANCESTORS to 'self' and Clockify domains"
else
  check_warn "No security headers configuration found"
fi

echo ""
echo "9. Rate Limiting"
echo "----------------"

if grep -r "RateLimiter\|ADDON_RATE_LIMIT" "$ADDON_DIR/src" >/dev/null 2>&1; then
  check_pass "Rate limiting configured"
  info "Set ADDON_RATE_LIMIT appropriately for production load"
else
  check_warn "No rate limiting found"
  info "Consider adding rate limiting to protect against abuse"
fi

echo ""
echo "10. Tests"
echo "---------"

# Check for test files
if [ -d "$ADDON_DIR/src/test" ]; then
  test_count=$(find "$ADDON_DIR/src/test" -name "*Test.java" -o -name "*IT.java" | wc -l | tr -d ' ')
  if [ "$test_count" -gt 0 ]; then
    check_pass "Found $test_count test files"

    # Try to run tests
    info "Running tests..."
    if mvn -f "$ADDON_DIR/pom.xml" test -q 2>/dev/null; then
      check_pass "All tests pass"
    else
      check_fail "Some tests are failing"
      info "Fix failing tests before deploying to production"
    fi
  else
    check_warn "No test files found"
  fi
else
  check_warn "No test directory found"
fi

echo ""
echo "========================================"
echo "Summary"
echo "========================================"
echo ""

total=$((PASSED + WARNINGS + ERRORS))
echo "Total checks: $total"
success "Passed: $PASSED"
if [ "$WARNINGS" -gt 0 ]; then
  warn "Warnings: $WARNINGS"
fi
if [ "$ERRORS" -gt 0 ]; then
  error "Errors: $ERRORS"
fi

echo ""

if [ "$ERRORS" -gt 0 ]; then
  error "Production readiness check FAILED"
  echo ""
  info "Please fix the errors above before deploying to production"
  exit 1
elif [ "$WARNINGS" -gt 0 ]; then
  warn "Production readiness check PASSED with warnings"
  echo ""
  info "Review the warnings above and address them if applicable"
  exit 0
else
  success "Production readiness check PASSED"
  echo ""
  info "Addon appears ready for production deployment"
  exit 0
fi
