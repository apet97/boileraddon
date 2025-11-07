.PHONY: validate build-java run-java

validate:
	python3 tools/validate-manifest.py

build-java:
	mvn -q -f templates/java-basic-addon/pom.xml -DskipTests package

run-java:
	ADDON_KEY=example.addon ADDON_BASE_URL=http://localhost:8080 \
	java -jar templates/java-basic-addon/target/java-basic-addon-0.1.0-jar-with-dependencies.jar
