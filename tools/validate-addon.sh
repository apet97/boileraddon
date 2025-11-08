#!/bin/bash

# Clockify Addon Validation Script
# Validates addon structure, manifest, code quality, and common mistakes

set -e

ADDON_DIR="$1"
EXIT_CODE=0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper functions
error() {
    echo -e "${RED}❌ ERROR: $1${NC}"
    EXIT_CODE=1
}

warning() {
    echo -e "${YELLOW}⚠️  WARNING: $1${NC}"
}

success() {
    echo -e "${GREEN}✓ $1${NC}"
}

info() {
    echo "ℹ️  $1"
}

# Check if addon directory provided
if [ -z "$ADDON_DIR" ]; then
    echo "Usage: $0 <addon-directory>"
    echo "Example: $0 addons/my-addon"
    exit 1
fi

# Check if directory exists
if [ ! -d "$ADDON_DIR" ]; then
    error "Directory not found: $ADDON_DIR"
    exit 1
fi

echo "========================================="
echo "Validating Addon: $ADDON_DIR"
echo "========================================="
echo ""

# 1. Check directory structure
info "Checking directory structure..."

if [ ! -f "$ADDON_DIR/pom.xml" ]; then
    error "Missing pom.xml"
else
    success "Found pom.xml"
fi

if [ ! -d "$ADDON_DIR/src/main/java" ]; then
    error "Missing src/main/java directory"
else
    success "Found src/main/java directory"
fi

if [ ! -d "$ADDON_DIR/src/test/java" ]; then
    warning "Missing src/test/java directory (tests recommended)"
else
    success "Found src/test/java directory"
fi

echo ""

# 2. Validate manifest.json (if it exists as static file)
info "Checking for manifest.json..."

if [ -f "$ADDON_DIR/manifest.json" ]; then
    echo "Found static manifest.json - validating..."

    # Check for $schema field (CRITICAL ERROR)
    if grep -q '"$schema"' "$ADDON_DIR/manifest.json"; then
        error "manifest.json contains \$schema field! Clockify will reject this manifest."
        echo "  Remove the \$schema field from runtime manifest."
    else
        success "No \$schema field in manifest (correct)"
    fi

    # Check for schemaVersion
    if ! grep -q '"schemaVersion".*:.*"1.3"' "$ADDON_DIR/manifest.json"; then
        error "manifest.json missing or incorrect schemaVersion (should be \"1.3\")"
    else
        success "Correct schemaVersion: 1.3"
    fi

    # Check required fields
    for field in key name description baseUrl scopes; do
        if ! grep -q "\"$field\"" "$ADDON_DIR/manifest.json"; then
            error "manifest.json missing required field: $field"
        fi
    done

    # Run Python validator if available
    if command -v python3 &> /dev/null; then
        if [ -f "tools/validate-manifest.py" ]; then
            echo "Running Python manifest validator..."
            python3 tools/validate-manifest.py "$ADDON_DIR/manifest.json" || EXIT_CODE=1
        fi
    fi
else
    warning "No static manifest.json found (may be generated programmatically)"
fi

echo ""

# 3. Check for common code mistakes
info "Scanning for common mistakes..."

# Check for $schema in any JSON files
if find "$ADDON_DIR" -name "*.json" -exec grep -l '"$schema"' {} \; 2>/dev/null | grep -v node_modules; then
    warning "Found \$schema in JSON files (ensure runtime manifests don't include it)"
fi

# Check for wrong auth header
if grep -r "Authorization.*Bearer" "$ADDON_DIR/src/" 2>/dev/null | grep -v ".class"; then
    error "Found 'Authorization: Bearer' header usage - should use 'X-Addon-Token' instead!"
    grep -rn "Authorization.*Bearer" "$ADDON_DIR/src/" 2>/dev/null | head -5
fi

# Check for token storage in INSTALLED handler
if ! grep -r "installationToken" "$ADDON_DIR/src/" 2>/dev/null | grep -q "save\|put\|store"; then
    warning "May not be storing installation token from INSTALLED event"
fi

# Check for hardcoded secrets
if grep -r "api_key.*=.*\"" "$ADDON_DIR/src/" 2>/dev/null | grep -v ".class"; then
    warning "Possible hardcoded API keys found - use environment variables"
fi

if grep -r "sk_live\|sk_test" "$ADDON_DIR/src/" 2>/dev/null | grep -v ".class"; then
    warning "Possible hardcoded API keys found (sk_live/sk_test pattern)"
fi

# Check for sensitive data logging
if grep -r "System.out.println.*token\|System.out.println.*password" "$ADDON_DIR/src/" 2>/dev/null | grep -v ".class"; then
    error "Possible sensitive data logging detected"
fi

success "Code scan complete"

echo ""

# 4. Check Maven configuration
info "Checking Maven configuration..."

# Check for main class in pom.xml
if ! grep -q "<mainClass>" "$ADDON_DIR/pom.xml"; then
    error "pom.xml missing <mainClass> configuration"
else
    success "Found mainClass configuration in pom.xml"
fi

# Check for jar-with-dependencies
if ! grep -q "jar-with-dependencies" "$ADDON_DIR/pom.xml"; then
    warning "pom.xml may not be configured to create fat JAR (jar-with-dependencies)"
else
    success "Fat JAR configuration found"
fi

echo ""

# 5. Build check
info "Checking if addon builds..."

if command -v mvn &> /dev/null; then
    echo "Running Maven compile..."
    if mvn -f "$ADDON_DIR/pom.xml" compile -q -DskipTests; then
        success "Addon compiles successfully"
    else
        error "Addon failed to compile"
    fi
else
    warning "Maven not found - skipping build check"
fi

echo ""

# 6. Test check
info "Checking tests..."

if [ -d "$ADDON_DIR/src/test/java" ]; then
    if command -v mvn &> /dev/null; then
        echo "Running tests..."
        if mvn -f "$ADDON_DIR/pom.xml" test -q; then
            success "All tests passed"
        else
            error "Some tests failed"
        fi
    else
        warning "Maven not found - skipping test execution"
    fi
else
    warning "No tests found"
fi

echo ""

# 7. Security checks
info "Running security checks..."

# Check for HTTPS in baseUrl (if in manifest)
if [ -f "$ADDON_DIR/manifest.json" ]; then
    if grep -q '"baseUrl".*"http://' "$ADDON_DIR/manifest.json"; then
        warning "baseUrl uses HTTP (should use HTTPS in production)"
    fi
fi

# Check for webhook signature validation (legacy and current header names)
if ! grep -rE "clockify-webhook-signature|x-clockify-signature" "$ADDON_DIR/src/" 2>/dev/null | grep -q "validate\|verify"; then
    warning "May not be validating webhook signatures (security risk)"
fi

success "Security checks complete"

echo ""

# 8. Documentation checks
info "Checking documentation..."

if [ ! -f "$ADDON_DIR/README.md" ]; then
    warning "Missing README.md"
else
    success "Found README.md"
fi

echo ""

# Summary
echo "========================================="
echo "Validation Summary"
echo "========================================="

if [ $EXIT_CODE -eq 0 ]; then
    success "All validation checks passed! ✨"
else
    error "Some validation checks failed. Please review errors above."
fi

echo ""

exit $EXIT_CODE
