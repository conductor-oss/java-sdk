# Role Management in Java Using Conductor

A Java Conductor workflow example demonstrating Role Management. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

An admin requests to assign the "editor" role to a team member. The system needs to log the role request with the requester's identity, validate that the assignment complies with access policies and doesn't conflict with existing roles, assign the role with its associated permissions (read, write), and sync those permissions to the IAM provider, API gateway, and database layer. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the request-logging, policy-validation, role-assignment, and permission-sync workers. Conductor handles the role lifecycle and cross-system propagation.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

RequestRoleWorker logs the assignment request, ValidateRoleWorker checks policy compliance, AssignRoleWorker grants permissions, and SyncPermissionsWorker propagates to IAM, API gateway, and database targets.

| Worker | Task | What It Does |
|---|---|---|
| **AssignRoleWorker** | `rom_assign` | Assigns the role to the user with its associated permissions (e.g., admin gets read, write, delete, manage_users) |
| **RequestRoleWorker** | `rom_request_role` | Logs the role assignment request with the requester's identity and assigns a unique request ID |
| **SyncPermissionsWorker** | `rom_sync_permissions` | Propagates the role's permissions to IAM, API gateway, and database targets |
| **ValidateRoleWorker** | `rom_validate` | Validates that the requested role assignment complies with access policies and checks for conflicting roles |

Workers simulate user lifecycle operations .  account creation, verification, profile setup ,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### The Workflow

```
rom_request_role
    │
    ▼
rom_validate
    │
    ▼
rom_assign
    │
    ▼
rom_sync_permissions
```

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build
```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build
```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/role-management-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/role-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rom_role_management \
  --version 1 \
  --input '{"userId": "TEST-001", "requestedRole": "test-value", "requestedBy": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rom_role_management -s COMPLETED -c 5
```

## How to Extend

Each worker handles one RBAC step .  connect your IAM provider (Okta, Auth0, AWS IAM) for role assignment and your API gateway for permission sync, and the role-management workflow stays the same.

- **RequestRoleWorker** (`rom_request_role`): log the role assignment request in your audit system and create an approval ticket in your ITSM platform (Jira, ServiceNow) if required by policy
- **ValidateRoleWorker** (`rom_validate`): check the requested role against your RBAC policy engine (Open Policy Agent, Casbin) for conflicts, separation-of-duties violations, and approval requirements
- **AssignRoleWorker** (`rom_assign`): assign the role and its permissions in your identity provider (Auth0 Roles API, Cognito Groups, Okta Groups) with the correct permission set
- **SyncPermissionsWorker** (`rom_sync_permissions`): propagate the role's permissions to AWS IAM policies, API gateway authorization rules, and database-level access controls

Integrate your IAM provider and API gateway and the request-validate-assign-sync role lifecycle operates without any workflow definition changes.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>
```

## Project Structure

```
role-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/rolemanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RoleManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssignRoleWorker.java
│       ├── RequestRoleWorker.java
│       ├── SyncPermissionsWorker.java
│       └── ValidateRoleWorker.java
└── src/test/java/rolemanagement/workers/
    ├── AssignRoleWorkerTest.java        # 4 tests
    ├── RequestRoleWorkerTest.java        # 3 tests
    └── SyncPermissionsWorkerTest.java        # 3 tests
```
