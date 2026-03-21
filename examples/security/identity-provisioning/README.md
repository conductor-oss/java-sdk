# Implementing Identity Provisioning in Java with Conductor :  Create Identity, Assign Roles, Provision Access, and Verify Setup

A Java Conductor workflow example for identity provisioning .  creating a user identity, assigning department-appropriate roles, provisioning access to required systems, and verifying the complete setup.

## The Problem

When a new employee joins, they need an identity (corporate email, SSO account), department-specific roles (engineering gets GitHub, sales gets Salesforce), provisioned access to all required systems, and verification that everything was set up correctly. If role assignment happens before identity creation, it fails. If verification is skipped, the new hire discovers on day one that they can't access the tools they need.

Without orchestration, identity provisioning is a manual checklist. IT creates accounts in 8 different systems, HR assigns roles in a spreadsheet, and nobody verifies the setup. New hires spend their first day filing IT tickets for missing access. Offboarding is even worse .  accounts are left active for months after departure.

## The Solution

**You just write the IdP account creation and SCIM provisioning calls. Conductor handles strict sequencing so roles are assigned only after identity creation, retries on SCIM failures, and a compliance audit trail for every provisioning action.**

Each provisioning step is an independent worker .  identity creation, role assignment, access provisioning, and setup verification. Conductor runs them in strict sequence: create the identity first, then assign roles, then provision access, then verify everything. Every provisioning action is tracked for compliance audit. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

The provisioning pipeline sequences four workers: CreateIdentityWorker sets up the corporate account, AssignRolesWorker maps department-appropriate permissions, ProvisionAccessWorker grants system access, and VerifySetupWorker confirms everything works on day one.

| Worker | Task | What It Does |
|---|---|---|
| **AssignRolesWorker** | `ip_assign_roles` | Assigns the appropriate role to the new identity based on job function |
| **CreateIdentityWorker** | `ip_create_identity` | Creates a new user identity in the identity provider for a given department |
| **ProvisionAccessWorker** | `ip_provision_access` | Provisions access to required systems (e.g., GitHub, AWS, Slack, Jira) based on assigned role |
| **VerifySetupWorker** | `ip_verify_setup` | Verifies the user can successfully access all provisioned systems |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations .  the workflow logic stays the same.

### The Workflow

```
ip_create_identity
    │
    ▼
ip_assign_roles
    │
    ▼
ip_provision_access
    │
    ▼
ip_verify_setup

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
java -jar target/identity-provisioning-1.0.0.jar

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
java -jar target/identity-provisioning-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow identity_provisioning_workflow \
  --version 1 \
  --input '{"userId": "TEST-001", "department": "engineering", "role": "user"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w identity_provisioning_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one provisioning step .  connect CreateIdentityWorker to Okta or Azure AD, ProvisionAccessWorker to SCIM-enabled SaaS apps, and the identity lifecycle workflow stays the same.

- **AssignRolesWorker** (`ip_assign_roles`): assign roles based on department/title in your IdP, map to application-specific groups (GitHub teams, Slack channels)
- **CreateIdentityWorker** (`ip_create_identity`): create accounts in your identity provider (Okta, Azure AD, Google Workspace) and email system
- **ProvisionAccessWorker** (`ip_provision_access`): provision access via SCIM to SaaS apps, create AWS IAM users, add to Kubernetes RBAC, invite to Slack/Teams

Connect to your IdP and SCIM endpoints, and the identity lifecycle orchestration runs without any workflow changes.

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
identity-provisioning-identity-provisioning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/identityprovisioning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssignRolesWorker.java
│       ├── CreateIdentityWorker.java
│       ├── ProvisionAccessWorker.java
│       └── VerifySetupWorker.java
└── src/test/java/identityprovisioning/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation

```
