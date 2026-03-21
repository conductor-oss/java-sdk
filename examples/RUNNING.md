# Running Conductor Examples

Every example in this repository follows the same structure and can be run the same way.

## Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

## Option 1: Docker Compose

Each example includes a `docker-compose.yml` that starts both Conductor and the example:

```bash
cd examples/<category>/<example>
docker compose up --build
```

Conductor UI will be available at `http://localhost:1234`.

## Option 2: Run Locally

Start Conductor:

```bash
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3
```

Build and run the example:

```bash
cd examples/<category>/<example>
mvn package -DskipTests
java -jar target/<example>-1.0.0.jar
```

## Option 3: Launcher Script

```bash
cd examples/<category>/<example>
./run.sh
```

The script auto-detects `CONDUCTOR_BASE_URL` or defaults to `http://localhost:8080/api`.

## Worker-Only Mode

Start workers without running the workflow (useful with CLI or UI):

```bash
java -jar target/<example>-1.0.0.jar --workers
```

## Using the Conductor CLI

With workers running in `--workers` mode, use the CLI to start workflows:

```bash
conductor workflow start \
  --workflow <workflow_name> \
  --version 1 \
  --input '{"key": "value"}'
```

Check status:

```bash
conductor workflow get-execution <workflow_id>
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## SDK Dependency

All examples use [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>
```

## Project Layout

Every example follows this layout:

```
<example>/
├── pom.xml                    # Maven build (Java 21)
├── run.sh                     # Launcher script
├── README.md                  # What this example does
├── Dockerfile                 # Multi-stage build
├── docker-compose.yml         # Conductor + workers
├── src/main/java/
│   ├── *Example.java          # Main class
│   ├── ConductorClientHelper.java
│   └── workers/               # Worker implementations
├── src/main/resources/
│   └── workflow.json          # Workflow definition
└── src/test/java/
    └── workers/               # Unit tests
```
