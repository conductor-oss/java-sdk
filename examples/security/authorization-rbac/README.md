# Implementing Role-Based Access Control (RBAC) in Java with Conductor : Role Resolution, Permission Evaluation, Context Check, and Decision Enforcement

## The Problem

You need to authorize every access request against a role-based policy model. When a user requests to perform an action (read, write, delete, admin) on a resource (document, API endpoint, database table), the system must resolve their assigned roles (admin, editor, viewer) from the identity store, evaluate whether any of those roles grant the requested permission, apply contextual constraints (is the request within business hours? from a trusted network? on a managed device?), and enforce the final allow or deny decision. all while logging every decision for compliance audits.

Without orchestration, authorization logic is scattered across middleware, decorators, and inline checks. Each service implements its own role-checking code with hardcoded role names and permission mappings. When the role hierarchy changes, you update a dozen services. When an auditor asks "who accessed what and why was it allowed?", you grep through application logs across multiple services trying to reconstruct the decision chain. Contextual policies (time-based access, network restrictions) are bolted on as afterthoughts with inconsistent enforcement.

## The Solution

**You just write the role resolution and permission evaluation logic. Conductor handles the ordered policy evaluation chain, retries when identity providers are unreachable, and a structured audit record of every access decision with the contributing roles and context signals.**

Each authorization concern is a simple, independent worker. one resolves the user's roles from the identity store, one evaluates permissions against the role-permission matrix, one checks contextual constraints like time and network, one enforces the decision and writes the audit record. Conductor takes care of executing them in strict order so no access is granted without a complete policy evaluation, retrying if the identity store is temporarily unavailable, and maintaining a complete audit trail that shows exactly which roles were resolved, which permissions were evaluated, which context was checked, and what decision was reached for every access request.

### What You Write: Workers

Four workers evaluate access requests: ResolveRolesWorker looks up user roles from the identity store, EvaluatePermissionsWorker checks the role-permission matrix, CheckContextWorker applies time/location/device constraints, and EnforceDecisionWorker logs the allow or deny decision for compliance.

| Worker | Task | What It Does |
|---|---|---|
| **ResolveRolesWorker** | `rbac_resolve_roles` | Looks up the user in the identity store and resolves their assigned roles (admin, editor, viewer) including inherited roles from group memberships |
| **EvaluatePermissionsWorker** | `rbac_evaluate_permissions` | Checks the role-permission matrix to determine whether any of the user's resolved roles grant the requested action (read, write, delete) on the target resource |
| **CheckContextWorker** | `rbac_check_context` | Evaluates contextual constraints. time of day, source IP/network location, device trust level, that may restrict or allow access beyond static role permissions |
| **EnforceDecisionWorker** | `rbac_enforce_decision` | Applies the final allow or deny decision, writes a structured audit record with the user, resource, action, roles, and reasoning for compliance |

the workflow logic stays the same.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
