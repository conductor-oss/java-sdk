# WAF Management

A web portal detects SQL injection attempts from 3 IP addresses. The pipeline analyzes the traffic pattern, updates WAF rules (adding 4 new blocking rules), deploys them to CloudFront WAF, and verifies that the attack is blocked while legitimate traffic flows normally.

## Workflow

```
waf_analyze_traffic ──> waf_update_rules ──> waf_deploy_rules ──> waf_verify_protection
```

Workflow `waf_management_workflow` accepts `application` and `threatType`. Times out after `300` seconds.

## Workers

**AnalyzeTrafficWorker** (`waf_analyze_traffic`) -- reports `"web-portal: detected SQL injection attempts from 3 IPs"`. Returns `analyze_trafficId` = `"ANALYZE_TRAFFIC-1364"`.

**UpdateRulesWorker** (`waf_update_rules`) -- creates blocking rules. Reports `"Updated WAF rules: 4 new blocking rules added"`. Returns `update_rules` = `true`.

**DeployRulesWorker** (`waf_deploy_rules`) -- pushes rules to production. Reports `"Rules deployed to CloudFront WAF"`. Returns `deploy_rules` = `true`.

**VerifyProtectionWorker** (`waf_verify_protection`) -- confirms protection. Reports `"Attack simulation blocked -- protection confirmed"`. Returns `verify_protection` = `true`.

## Workflow Output

The workflow produces `analyze_trafficResult`, `verify_protectionResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `waf_management_workflow` defines 4 tasks with input parameters `application`, `threatType` and a timeout of `300` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify the end-to-end WAF management pipeline from traffic analysis through protection verification.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
