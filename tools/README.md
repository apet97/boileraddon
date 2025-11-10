# Development Tools

This directory contains scripts and tools for development, validation, and CI/CD.

## Scripts

### Pre-commit Hook

**File**: `pre-commit-hook.sh`

Automated validation checks run before each commit to catch common issues early.

**Installation**:

```bash
# Option 1: Copy to .git/hooks (traditional)
cp tools/pre-commit-hook.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

# Option 2: Use Git hooks path (recommended, easier to update)
git config core.hooksPath tools/hooks
mkdir -p tools/hooks
ln -sf ../pre-commit-hook.sh tools/hooks/pre-commit
```

**Checks performed**:

1. **Manifest validation** - Ensures manifest files compile
2. **TODO/FIXME detection** - Warns about unresolved markers in production code
3. **Secrets detection** - Blocks commits with potential credentials/API keys
4. **Environment file protection** - Prevents .env files from being committed
5. **Fast compilation** - Verifies Java code compiles (can be skipped)
6. **File size validation** - Warns about files >1MB
7. **License headers** - Checks new files have basic license/copyright info

**Bypass** (not recommended):
```bash
git commit --no-verify
```

**Skip compilation check**:
```bash
SKIP_COMPILE_CHECK=1 git commit
```

---

### Manifest Validation

**File**: `validate-manifest.py`

Validates Clockify add-on manifest files against the schema.

**Usage**:
```bash
python3 tools/validate-manifest.py path/to/manifest.json
```

---

### Add-on Validation

**File**: `validate-addon.sh`

Comprehensive validation of add-on structure, endpoints, and configuration.

**Usage**:
```bash
./tools/validate-addon.sh addons/rules
```

---

### OpenAPI Validation

**File**: `validate-openapi.sh`

Validates OpenAPI specification files.

**Usage**:
```bash
./tools/validate-openapi.sh path/to/openapi.yaml
```

---

### Coverage Badge Generator

**File**: `coverage_badge.py`

Generates SVG badges for JaCoCo test coverage reports.

**Usage**:
```bash
python3 tools/coverage_badge.py target/site/jacoco/jacoco.csv
```

---

### Briefing Link Checker

**File**: `check_briefing_links.py`

Validates links in briefing documents.

**Usage**:
```bash
python3 tools/check_briefing_links.py _briefings/
```

---

### JWT Verification Example

**File**: `verify-jwt-example.py`

Example script demonstrating JWT token verification for webhook signatures.

**Usage**:
```bash
python3 tools/verify-jwt-example.py
```

---

## Codex Prompts

The `codex_prompts/` directory contains AI-assisted development prompts and templates.

---

## Integration with CI

These tools are integrated into GitHub Actions workflows:

- `.github/workflows/build-and-test.yml` - Uses manifest validation
- `.github/workflows/validate.yml` - Runs validation scripts
- `.github/workflows/pages.yml` - Uses coverage badge generator

---

## Contributing

When adding new tools:

1. Make scripts executable: `chmod +x tools/your-script.sh`
2. Add documentation to this README
3. Consider integration with pre-commit hook
4. Test thoroughly before committing
