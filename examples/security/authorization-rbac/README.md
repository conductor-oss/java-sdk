# Implementing Role-Based Access Control (RBAC) in Java with Conductor :  Role Resolution, Permission Evaluation, Context Check, and Decision Enforcement

A Java Conductor workflow example implementing role-based access control. resolving a user's roles from the identity store, evaluating whether those roles grant the requested permission on the target resource, checking contextual constraints (time of day, network location, device trust), and enforcing the allow/deny decision with a full audit log. Uses [Conductor](https://github.

## The Problem

You need to authorize every access request against a role-based policy model. When a user requests to perform an action (read, write, delete, admin) on a resource (document, API endpoint, database table), the system must resolve their assigned roles (admin, editor, viewer) from the identity store, evaluate whether any of those roles grant the requested permission, apply contextual constraints (is the request within business hours? from a trusted network? on a managed device?), and enforce the final allow or deny decision. all while logging every decision for compliance audits.

Without orchestration, authorization logic is scattered across middleware, decorators, and inline checks. Each service implements its own role-checking code with hardcoded role names and permission mappings. When the role hierarchy changes, you update a dozen services. When an auditor asks "who accessed what and why was it allowed?", you grep through application logs across multiple services trying to reconstruct the decision chain. Contextual policies (time-based access, network restrictions) are bolted on as afterthoughts with inconsistent enforcement.

## The Solution

**You just write the role resolution and permission evaluation logic. Conductor handles the ordered policy evaluation chain, retries when identity providers are unreachable, and a structured audit record of every access decision with the contributing roles and context signals.**

Each authorization concern is a simple, independent worker. one resolves the user's roles from the identity store, one evaluates permissions against the role-permission matrix, one checks contextual constraints like time and network, one enforces the decision and writes the audit record. Conductor takes care of executing them in strict order so no access is granted without a complete policy evaluation, retrying if the identity store is temporarily unavailable, and maintaining a complete audit trail that shows exactly which roles were resolved, which permissions were evaluated, which context was checked, and what decision was reached for every access request. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers evaluate access requests: ResolveRolesWorker looks up user roles from the identity store, EvaluatePermissionsWorker checks the role-permission matrix, CheckContextWorker applies time/location/device constraints, and EnforceDecisionWorker logs the allow or deny decision for compliance.

| Worker | Task | What It Does |
|---|---|---|
| **ResolveRolesWorker** | `rbac_resolve_roles` | Looks up the user in the identity store and resolves their assigned roles (admin, editor, viewer) including inherited roles from group memberships |
| **EvaluatePermissionsWorker** | `rbac_evaluate_permissions` | Checks the role-permission matrix to determine whether any of the user's resolved roles grant the requested action (read, write, delete) on the target resource |
| **CheckContextWorker** | `rbac_check_context` | Evaluates contextual constraints. time of day, source IP/network location, device trust level,  that may restrict or allow access beyond static role permissions |
| **EnforceDecisionWorker** | `rbac_enforce_decision` | Applies the final allow or deny decision, writes a structured audit record with the user, resource, action, roles, and reasoning for compliance |

Workers implement security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations. the workflow logic stays the same.

### The Workflow

```
rbac_resolve_roles
    │
    ▼
rbac_evaluate_permissions
    │
    ▼
rbac_check_context
    │
    ▼
rbac_enforce_decision

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
java -jar target/authorization-rbac-1.0.0.jar

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
java -jar target/authorization-rbac-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow authorization_rbac \
  --version 1 \
  --input '{}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w authorization_rbac -s COMPLETED -c 5

```

## How to Extend

Each worker evaluates one authorization layer. connect ResolveRolesWorker to Okta or Keycloak, EvaluatePermissionsWorker to Open Policy Agent, and the role-permission-context-enforce workflow stays the same.

- **ResolveRolesWorker** (`rbac_resolve_roles`): query your identity provider (Okta, Auth0, Active Directory, Keycloak) for the user's group memberships and role assignments, including role inheritance hierarchies
- **EvaluatePermissionsWorker** (`rbac_evaluate_permissions`): evaluate permissions against an Open Policy Agent (OPA) policy, a Casbin model, or a custom role-permission matrix stored in your database
- **CheckContextWorker** (`rbac_check_context`): integrate with your network perimeter (Zscaler, Cloudflare Access) for location/device context, or query a device management API (Jamf, Intune) for device trust signals
- **EnforceDecisionWorker** (`rbac_enforce_decision`): write audit records to Splunk, Elasticsearch, or a compliance database, and send real-time alerts to SIEM systems for denied high-privilege access attempts

Point each worker at your IdP and OPA policies, and the role-permission-context authorization chain carries over to production without modification.

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
authorization-rbac/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/authorizationrbac/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AuthorizationRbacExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckContextWorker.java
│       ├── EnforceDecisionWorker.java
│       ├── EvaluatePermissionsWorker.java
│       └── ResolveRolesWorker.java
└── src/test/java/authorizationrbac/workers/
    ├── CheckContextWorkerTest.java        # 8 tests
    ├── EnforceDecisionWorkerTest.java        # 8 tests
    ├── EvaluatePermissionsWorkerTest.java        # 8 tests
    └── ResolveRolesWorkerTest.java        # 8 tests

```
