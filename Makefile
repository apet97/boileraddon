.PHONY: help setup validate build build-template build-auto-tag-assistant build-rules run-auto-tag-assistant run-rules docker-run dev clean test briefings-open briefings-verify

TEMPLATE ?= _template-addon
ADDON_PORT ?= 8080
ADDON_BASE_URL ?= http://localhost:$(ADDON_PORT)/$(TEMPLATE)
DOCKER_IMAGE ?= clockify-addon-$(TEMPLATE)

# Default target
help:
	@echo "Clockify Add-on Boilerplate - Make targets:"
	@echo ""
	@echo "  setup                      - Install dependencies and prepare environment"
	@echo "  validate                   - Validate all manifest.json files"
	@echo "  build                      - Build all modules (templates + addons)"
	@echo "  build-template             - Build only the _template-addon module"
	@echo "  build-auto-tag-assistant   - Build only the auto-tag-assistant addon"
	@echo "  build-rules                - Build only the rules addon"
	@echo "  build-overtime             - Build only the overtime addon"
	@echo "  dev                        - Build and run the template add-on using .env"
	@echo "  docker-run                 - Build and run an add-on inside Docker (override TEMPLATE=...)"
	@echo "  test                       - Run all tests"
	@echo "  run-auto-tag-assistant     - Run the auto-tag-assistant addon locally"
	@echo "  run-rules                  - Run the rules addon locally"
	@echo "  rules-apply                - Run rules with RULES_APPLY_CHANGES=true"
	@echo "  rules-seed-demo            - Seed a demo rule and dry-run test"
	@echo "  rules-webhook-sim          - Simulate a signed webhook locally"
	@echo "  dev-rules                  - Run rules add-on using .env.rules"
	@echo "  new-addon                  - Scaffold a new add-on (NAME, DISPLAY)"
	@echo "  zero-shot-run              - Build & run selected addon, print manifest URL; pairs well with ngrok"
	@echo "  manifest-url               - Print the current manifest URL"
	@echo "  clean                      - Clean all build artifacts"
	@echo ""
	@echo "Quick start:"
	@echo "  1. make build              # Build everything"
	@echo "  2. make run-auto-tag-assistant  # Run the demo addon"
	@echo "  3. In another terminal: ngrok http 8080"
	@echo "  4. Restart with: ADDON_BASE_URL=https://YOUR-NGROK.ngrok-free.app/auto-tag-assistant make run-auto-tag-assistant"
	@echo "  5. Install in Clockify using the printed https manifest URL"
	@echo ""
	@echo "Note: This boilerplate now uses ONLY Maven Central dependencies."
	@echo "      No GitHub Packages authentication or external SDK needed!"
	@echo ""
	@echo "AI onboarding:"
	@echo "  make ai-start               - Print AI onboarding pointers"
 
briefings-open:
	@echo "_briefings/INDEX.md is at $(PWD)/_briefings/INDEX.md"

briefings-verify:
	python3 tools/check_briefing_links.py _briefings

# Setup environment
setup:
	@echo "Checking Java version..."
	@java -version
	@echo "Checking Maven version..."
	@mvn -version
	@echo "✓ Setup complete!"

# Validate manifest files
validate:
	@echo "Validating manifest files..."
	@python3 tools/validate-manifest.py
	@echo "✓ All manifests valid"

# Build everything (templates + addons) - NO SDK build needed!
build:
	@echo "Building all modules..."
	@echo "Note: Using inline SDK with Maven Central dependencies only"
	@mvn -q clean package -DskipTests
	@echo "✓ Build complete!"
	@echo ""
	@echo "Built artifacts:"
	@ls -lh addons/_template-addon/target/*jar-with-dependencies.jar 2>/dev/null || true
	@ls -lh addons/auto-tag-assistant/target/*jar-with-dependencies.jar 2>/dev/null || true
	@ls -lh addons/rules/target/*jar-with-dependencies.jar 2>/dev/null || true

# Build template only
build-template:
	@echo "Building _template-addon module..."
	@mvn -q -pl addons/_template-addon package -DskipTests
	@echo "✓ Template built: addons/_template-addon/target/_template-addon-0.1.0-jar-with-dependencies.jar"

# Build auto-tag-assistant only
build-auto-tag-assistant:
	@echo "Building auto-tag-assistant addon..."
	mvn -q -f addons/auto-tag-assistant/pom.xml clean package -DskipTests
	@echo "✓ Auto-Tag Assistant built: addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar"

# Build rules only
build-rules:
	@echo "Building rules addon..."
	mvn -q -f addons/rules/pom.xml clean package -DskipTests
	@echo "✓ Rules built: addons/rules/target/rules-0.1.0-jar-with-dependencies.jar"

# Build overtime only
build-overtime:
	@echo "Building overtime addon..."
	mvn -q -f addons/overtime/pom.xml clean package -DskipTests
	@echo "✓ Overtime built: addons/overtime/target/overtime-0.1.0-jar-with-dependencies.jar"

# No longer needed - kept for backward compatibility
install:
	@echo "Note: SDK installation is no longer needed."
	@echo "This boilerplate now uses inline SDK with Maven Central dependencies only."

# Run tests
test:
	@echo "Running tests..."
	@bash scripts/test-new-addon.sh
	mvn test
	@echo "✓ Tests passed"

# Run auto-tag-assistant locally
run-auto-tag-assistant:
	@echo "Starting Auto-Tag Assistant..."
	@echo "================================"
	@echo "Base URL: http://localhost:8080/auto-tag-assistant"
	@echo "Manifest: http://localhost:8080/auto-tag-assistant/manifest.json"
	@echo "================================"
	@echo ""
	@echo "To expose via ngrok:"
	@echo "  1. In another terminal: ngrok http 8080"
	@echo "  2. Restart with: ADDON_BASE_URL=https://YOUR-SUBDOMAIN.ngrok-free.app/auto-tag-assistant make run-auto-tag-assistant"
	@echo "  3. Install in Clockify using: https://YOUR-SUBDOMAIN.ngrok-free.app/auto-tag-assistant/manifest.json"
	@echo ""
	ADDON_PORT=8080 ADDON_BASE_URL=http://localhost:8080/auto-tag-assistant \
	java -jar addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar

# Run auto-tag-assistant with database-backed token store (set DB_URL/DB_USERNAME/DB_PASSWORD)
run-auto-tag-assistant-db:
	@echo "Starting Auto-Tag Assistant with DatabaseTokenStore..."
	@echo "Ensure DB_URL, DB_USERNAME, DB_PASSWORD are set in your environment or .env"
	ADDON_PORT=$(ADDON_PORT) ADDON_BASE_URL=$(ADDON_BASE_URL) \
	DB_URL=$(DB_URL) DB_USERNAME=$(DB_USERNAME) DB_PASSWORD=$(DB_PASSWORD) \
	java -jar addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar

# Run rules addon locally
run-rules:
	@echo "Starting Rules Add-on..."
	@echo "================================"
	@echo "Base URL: http://localhost:8080/rules"
	@echo "Manifest: http://localhost:8080/rules/manifest.json"
	@echo "Rules API: http://localhost:8080/rules/api/rules"
	@echo "================================"
	@echo ""
	@echo "To expose via ngrok:"
	@echo "  1. In another terminal: ngrok http 8080"
	@echo "  2. Restart with: ADDON_BASE_URL=https://YOUR-SUBDOMAIN.ngrok-free.app/rules make run-rules"
	@echo "  3. Install in Clockify using: https://YOUR-SUBDOMAIN.ngrok-free.app/rules/manifest.json"
	@echo ""
	ADDON_PORT=8080 ADDON_BASE_URL=http://localhost:8080/rules \
	java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar

# Run rules and actually apply changes (idempotent updates)
rules-apply:
	@echo "Starting Rules Add-on with RULES_APPLY_CHANGES=true..."
	RULES_APPLY_CHANGES=true \
	ADDON_PORT=8080 ADDON_BASE_URL=$(ADDON_BASE_URL) \
	java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar

docker-run:
	@echo "Building Docker image for $(TEMPLATE)..."
	docker build \
                --build-arg ADDON_DIR=addons/$(TEMPLATE) \
                --build-arg DEFAULT_BASE_URL=$(ADDON_BASE_URL) \
                -t $(DOCKER_IMAGE) .
	@echo "Starting container (Ctrl+C to stop)..."
	docker run --rm -it \
                -e ADDON_PORT=$(ADDON_PORT) \
                -e ADDON_BASE_URL=$(ADDON_BASE_URL) \
                -e JAVA_OPTS="$(JAVA_OPTS)" \
                -p $(ADDON_PORT):$(ADDON_PORT) \
                $(DOCKER_IMAGE)

dev: build-template
	@if [ ! -f .env ]; then \
		echo "Missing .env file. Run: cp .env.example .env"; \
		exit 1; \
	fi
	@echo "Starting _template-addon with settings from .env..."
	@bash -c 'set -a; source .env; set +a; java -jar addons/_template-addon/target/_template-addon-0.1.0-jar-with-dependencies.jar'

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	mvn clean
	@echo "✓ Clean complete"


# Validate manifests using local JSON schema (if jsonschema installed)
schema-validate:
	@python3 -c "import pkgutil,sys;import json;import os; from jsonschema import Draft7Validator" >/dev/null 2>&1 || { echo 'jsonschema not installed: pip install jsonschema'; exit 1; }
	@python3 tools/validate-manifest.py addons/_template-addon/manifest.json || true

# Quick environment sanity check
dev-check:
	@bash scripts/dev-env-check.sh

# AI helpers
ai-start:
	@echo "AI Onboarding: /Users/15x/boileraddon/docs/AI_ONBOARDING.md"
	@echo "AGENTS Guide: /Users/15x/boileraddon/AGENTS.md"

# AI zero-shot cheat sheet (one-screen quick steps)
ai-cheatsheet:
	@echo "AI Zero-Shot Playbook — Quick Steps"
	@echo "1) Toolchain: java -version && mvn -version"
	@echo "2) Validate: python3 tools/validate-manifest.py"
	@echo "3) Tests: mvn -e -pl addons/addon-sdk -am test"
	@echo "   Single test: mvn -e -pl addons/addon-sdk -Dtest=Class#method test"
	@echo "4) Verify: mvn -e -fae verify"
	@echo "5) Commit with proof lines (validator OK, tests 0 failures, BUILD SUCCESS)"
	@echo "6) Optional run: TEMPLATE=auto-tag-assistant make zero-shot-run"
	@echo "Docs: docs/AI_ZERO_SHOT_PLAYBOOK.md"

# AI verification helper: runs validator, addon-sdk tests, and reactor verify, then prints proof lines
ai-verify:
	@bash -euo pipefail -c "\
	  echo '== Manifest validation =='; \
	  python3 tools/validate-manifest.py | tee /tmp/ai_manifest.out; \
	  if grep -q '^Invalid:' /tmp/ai_manifest.out; then MAN='FAIL'; else MAN='OK'; fi; \
	  echo; \
	  echo '== addon-sdk tests =='; \
	  mvn -e -DtrimStackTrace=false -Dsurefire.printSummary=true -pl addons/addon-sdk -am test | tee /tmp/ai_sdktest.out; \
	  SDK_SUM=\$$(rg -n 'Tests run: .*Failures:.*Errors:.*' -S addons/addon-sdk/target/surefire-reports/*.txt 2>/dev/null | tail -n 1 || true); \
	  if grep -E 'Failures:\\s*[1-9]|Errors:\\s*[1-9]' /tmp/ai_sdktest.out >/dev/null 2>&1; then SDK='FAIL'; else SDK='OK'; fi; \
	  echo; \
	  echo '== Reactor verify =='; \
	  mvn -e -DtrimStackTrace=false -fae verify | tee /tmp/ai_verify.out; \
	  if grep -q 'BUILD SUCCESS' /tmp/ai_verify.out; then REACTOR='BUILD SUCCESS'; else REACTOR='FAIL'; fi; \
	  echo; \
	  echo '== Proof lines =='; \
	  echo "- python3 tools/validate-manifest.py → \$${MAN}"; \
	  if [ -n "\$${SDK_SUM}" ]; then echo "- addon-sdk surefire summary: \$${SDK_SUM}"; fi; \
	  echo "- mvn -fae verify → \$${REACTOR}"; \
	"

# Print the current manifest URL based on ADDON_BASE_URL
manifest-url:
	@if [ -z "$(ADDON_BASE_URL)" ]; then \
		echo "ADDON_BASE_URL is not set. Example:"; \
		echo "  export ADDON_BASE_URL=https://YOUR-SUBDOMAIN.ngrok-free.app/auto-tag-assistant"; \
		echo "  or"; \
		echo "  export ADDON_BASE_URL=https://YOUR-SUBDOMAIN.ngrok-free.app/rules"; \
	else \
		echo "Manifest URL: $(ADDON_BASE_URL)/manifest.json"; \
	fi

# Zero-shot run helper: build selected addon and run it with sensible defaults
zero-shot-run:
	@if [ -z "$(TEMPLATE)" ]; then \
		echo "TEMPLATE is required (e.g., TEMPLATE=auto-tag-assistant or TEMPLATE=rules)"; \
		exit 1; \
	fi
	@echo "Building $(TEMPLATE) ..."
	@if [ "$(TEMPLATE)" = "_template-addon" ]; then \
		mvn -q -pl addons/_template-addon package -DskipTests; \
	else \
		mvn -q -pl addons/$(TEMPLATE) -am package -DskipTests; \
	fi
	@echo "Starting $(TEMPLATE) at $(ADDON_BASE_URL) ..."
	@if [ "$(TEMPLATE)" = "_template-addon" ]; then \
		ADDON_PORT=$(ADDON_PORT) ADDON_BASE_URL=$(ADDON_BASE_URL) \
		java -jar addons/_template-addon/target/_template-addon-0.1.0-jar-with-dependencies.jar; \
	elif [ "$(TEMPLATE)" = "auto-tag-assistant" ]; then \
		ADDON_PORT=$(ADDON_PORT) ADDON_BASE_URL=$(ADDON_BASE_URL) \
		java -jar addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar; \
	elif [ "$(TEMPLATE)" = "rules" ]; then \
		ADDON_PORT=$(ADDON_PORT) ADDON_BASE_URL=$(ADDON_BASE_URL) \
		java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar; \
	elif [ "$(TEMPLATE)" = "overtime" ]; then \
		ADDON_PORT=$(ADDON_PORT) ADDON_BASE_URL=$(ADDON_BASE_URL) \
		java -jar addons/overtime/target/overtime-0.1.0-jar-with-dependencies.jar; \
	else \
		echo "Unknown TEMPLATE=$(TEMPLATE). Supported: _template-addon, auto-tag-assistant, rules, overtime"; \
		exit 1; \
	fi

# Seed a demo rule and exercise /api/test
rules-seed-demo:
	@bash scripts/rules-demo.sh

# Simulate a signed webhook request for local testing
rules-webhook-sim:
	@bash scripts/rules-webhook-sim.sh

# Overtime run target
run-overtime:
	@echo "Starting Overtime Add-on..."
	@echo "================================"
	@echo "Base URL: http://localhost:8080/overtime"
	@echo "Manifest: http://localhost:8080/overtime/manifest.json"
	@echo "================================"
	ADDON_PORT=8080 ADDON_BASE_URL=http://localhost:8080/overtime \
	java -jar addons/overtime/target/overtime-0.1.0-jar-with-dependencies.jar

# Run Rules using .env.rules (similar to dev target for template)
dev-rules: build-rules
	@if [ ! -f .env.rules ]; then \
		echo "Missing .env.rules. Create one: cp .env.rules.example .env.rules"; \
		exit 1; \
	fi
	@echo "Starting Rules add-on with settings from .env.rules..."
	@bash -c 'set -a; source .env.rules; set +a; \
	  java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar'

new-addon:
	@if [ -z "$(NAME)" ] || [ -z "$(DISPLAY)" ]; then \
		echo "Usage: make new-addon NAME=my-addon DISPLAY=\"My Add-on\""; \
		exit 1; \
	fi
	@bash scripts/new-addon.sh $(NAME) "$(DISPLAY)"
