.PHONY: help setup validate build build-template build-auto-tag-assistant run-auto-tag-assistant docker-run dev clean test

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
	@echo "  dev                        - Build and run the template add-on using .env"
	@echo "  docker-run                 - Build and run an add-on inside Docker (override TEMPLATE=...)"
	@echo "  test                       - Run all tests"
	@echo "  run-auto-tag-assistant     - Run the auto-tag-assistant addon locally"
	@echo "  clean                      - Clean all build artifacts"
	@echo ""
	@echo "Quick start:"
	@echo "  1. make build              # Build everything"
	@echo "  2. make run-auto-tag-assistant  # Run the demo addon"
	@echo "  3. In another terminal: ngrok http 8080"
	@echo "  4. Update manifest.json baseUrl with ngrok URL"
	@echo "  5. Install in Clockify using ngrok manifest URL"
	@echo ""
	@echo "Note: This boilerplate now uses ONLY Maven Central dependencies."
	@echo "      No GitHub Packages authentication or external SDK needed!"

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

# No longer needed - kept for backward compatibility
install:
	@echo "Note: SDK installation is no longer needed."
	@echo "This boilerplate now uses inline SDK with Maven Central dependencies only."

# Run tests
test:
	@echo "Running tests..."
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
	@echo "  2. Update manifest.json baseUrl to: https://YOUR-SUBDOMAIN.ngrok-free.app/auto-tag-assistant"
	@echo "  3. Install in Clockify using: https://YOUR-SUBDOMAIN.ngrok-free.app/auto-tag-assistant/manifest.json"
	@echo ""
	ADDON_PORT=8080 ADDON_BASE_URL=http://localhost:8080/auto-tag-assistant \
	java -jar addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar

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
