#!/usr/bin/env bash
#
# Git pre-commit hook for Clockify Add-on Boilerplate
#
# Installation:
#   cp tools/pre-commit-hook.sh .git/hooks/pre-commit
#   chmod +x .git/hooks/pre-commit
#
# Or use: git config core.hooksPath tools/hooks
#

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "Running pre-commit validation checks..."

# Exit codes
EXIT_CODE=0

# Helper function to print colored messages
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Check 1: Validate manifest files if changed
echo ""
echo "Check 1: Manifest validation"
MANIFEST_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep -E 'Manifest\.java$' || true)
if [ -n "$MANIFEST_FILES" ]; then
    echo "Manifest files changed:"
    echo "$MANIFEST_FILES"
    # Validate that manifest builds compile
    if mvn -q compile -DskipTests > /dev/null 2>&1; then
        print_success "Manifest files compile successfully"
    else
        print_error "Manifest files fail to compile"
        EXIT_CODE=1
    fi
else
    print_success "No manifest files changed"
fi

# Check 2: Detect TODO/FIXME/HACK in production code (not in templates or examples)
echo ""
echo "Check 2: TODO/FIXME/HACK detection in production code"
PRODUCTION_FILES=$(git diff --cached --name-only --diff-filter=ACM | \
    grep -E '\.java$' | \
    grep -v '/test/' | \
    grep -v '_template-addon' | \
    grep -v 'templates/' || true)

if [ -n "$PRODUCTION_FILES" ]; then
    TODO_FOUND=0
    for file in $PRODUCTION_FILES; do
        if git diff --cached "$file" | grep -E '^\+.*\b(TODO|FIXME|HACK|XXX)\b' > /dev/null 2>&1; then
            if [ $TODO_FOUND -eq 0 ]; then
                print_warning "Found TODO/FIXME/HACK markers in production code:"
                TODO_FOUND=1
            fi
            echo "  - $file"
            git diff --cached "$file" | grep -E '^\+.*\b(TODO|FIXME|HACK|XXX)\b' | head -n 3
        fi
    done

    if [ $TODO_FOUND -eq 1 ]; then
        print_warning "Consider resolving or documenting these items before committing"
        # Not failing the build, just warning
    else
        print_success "No TODO/FIXME/HACK markers in production code"
    fi
else
    print_success "No production Java files changed"
fi

# Check 3: Secrets detection (basic patterns)
echo ""
echo "Check 3: Secrets detection"
SECRETS_PATTERNS=(
    'password\s*=\s*["\047][^"\047]{8,}'
    'api[_-]?key\s*[=:]\s*["\047][A-Za-z0-9]{20,}'
    'secret\s*[=:]\s*["\047][A-Za-z0-9]{20,}'
    'token\s*[=:]\s*["\047][A-Za-z0-9_-]{20,}'
    'private[_-]?key.*BEGIN\s+(RSA|EC|OPENSSH)\s+PRIVATE\s+KEY'
    'AWS|AKIA[0-9A-Z]{16}'
    'mongodb(\+srv)?:\/\/[^:]+:[^@]+@'
    'postgres:\/\/[^:]+:[^@]+@'
)

SECRETS_FOUND=0
for pattern in "${SECRETS_PATTERNS[@]}"; do
    if git diff --cached | grep -iE "$pattern" > /dev/null 2>&1; then
        if [ $SECRETS_FOUND -eq 0 ]; then
            print_error "Potential secrets detected in staged changes:"
            SECRETS_FOUND=1
        fi
        git diff --cached | grep -niE "$pattern" | head -n 2
    fi
done

if [ $SECRETS_FOUND -eq 1 ]; then
    print_error "BLOCKING: Secrets detected. Remove sensitive data before committing."
    EXIT_CODE=1
else
    print_success "No secrets detected"
fi

# Check 4: Verify .env files are not committed
echo ""
echo "Check 4: Environment file protection"
ENV_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep -E '\.env$|\.env\.' || true)
if [ -n "$ENV_FILES" ]; then
    print_error "BLOCKING: .env files should not be committed:"
    echo "$ENV_FILES"
    EXIT_CODE=1
else
    print_success "No .env files in commit"
fi

# Check 5: Fast compilation check (optional, can be slow)
if [ "${SKIP_COMPILE_CHECK:-0}" != "1" ]; then
    echo ""
    echo "Check 5: Fast compilation check"
    JAVA_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep '\.java$' || true)
    if [ -n "$JAVA_FILES" ]; then
        if mvn -q compile -DskipTests -T 1C > /dev/null 2>&1; then
            print_success "Fast compilation successful"
        else
            print_error "Compilation failed. Run 'mvn compile' to see details."
            EXIT_CODE=1
        fi
    else
        print_success "No Java files to compile"
    fi
else
    echo ""
    print_warning "Skipping compilation check (SKIP_COMPILE_CHECK=1)"
fi

# Check 6: Verify file size limits (prevent large files)
echo ""
echo "Check 6: File size validation"
LARGE_FILES=$(git diff --cached --name-only --diff-filter=ACM | while read file; do
    if [ -f "$file" ]; then
        SIZE=$(wc -c < "$file")
        if [ "$SIZE" -gt 1048576 ]; then # 1MB
            echo "$file ($(numfmt --to=iec-i --suffix=B $SIZE))"
        fi
    fi
done)

if [ -n "$LARGE_FILES" ]; then
    print_warning "Large files detected (>1MB):"
    echo "$LARGE_FILES"
    print_warning "Consider if these files should be committed to Git"
else
    print_success "No large files detected"
fi

# Check 7: License headers (basic check)
echo ""
echo "Check 7: License/Copyright headers (sample check)"
NEW_JAVA_FILES=$(git diff --cached --name-only --diff-filter=A | \
    grep -E '\.java$' | \
    grep -v '/test/' | \
    grep -v '_template-addon' || true)

if [ -n "$NEW_JAVA_FILES" ]; then
    MISSING_HEADERS=0
    for file in $NEW_JAVA_FILES; do
        # Check if file has package declaration (basic heuristic for production code)
        if head -n 10 "$file" | grep -q "^package"; then
            # Very basic check - just ensure it's not completely missing structure
            if ! head -n 5 "$file" | grep -qE "(Copyright|License|SPDX|Apache|MIT)"; then
                if [ $MISSING_HEADERS -eq 0 ]; then
                    print_warning "New Java files without license/copyright headers:"
                    MISSING_HEADERS=1
                fi
                echo "  - $file"
            fi
        fi
    done

    if [ $MISSING_HEADERS -eq 0 ]; then
        print_success "License headers check passed"
    fi
else
    print_success "No new Java files to check for headers"
fi

# Summary
echo ""
echo "========================================"
if [ $EXIT_CODE -eq 0 ]; then
    print_success "All pre-commit checks passed!"
    echo ""
    echo "Tip: To skip these checks (not recommended), use:"
    echo "  git commit --no-verify"
else
    print_error "Pre-commit checks failed!"
    echo ""
    echo "Fix the issues above or use --no-verify to bypass (not recommended)"
fi
echo "========================================"

exit $EXIT_CODE
