# IoT Security in Java with Conductor : Network Scanning, Vulnerability Detection, Automated Patching, and Verification

## Why IoT Security Workflows Need Orchestration

Securing an IoT network is a strict pipeline where skipping a step or executing out of order creates real risk. You scan the network to discover all connected devices. cameras, sensors, gateways. You run vulnerability detection against the inventory to find devices with expired TLS certificates, default passwords, or firmware versions affected by known CVEs. You push security patches to the specific affected devices. After patching, you re-verify that every device on the network passes the security check.

If the vulnerability scanner finds issues on some devices but the patch delivery fails partway through, you need to know exactly which devices were patched and which still need attention. If you skip verification after patching, you cannot confirm the vulnerability was actually remediated. Without orchestration, you'd build a monolithic security script that mixes network discovery, CVE database lookups, patch distribution, and verification. making it impossible to audit which vulnerability triggered which patch or to retry a failed patch without rescanning the entire network.

## How This Workflow Solves It

**You just write the IoT security workers. Network scanning, vulnerability detection, patch deployment, and remediation verification. Conductor handles scan-to-verify sequencing, patch delivery retries, and audit trails linking every vulnerability to its remediation proof.**

Each security operation is an independent worker. scan devices, detect vulnerabilities, apply patches, verify remediation. Conductor sequences them strictly, passes the affected device list from detection to patching, retries if a patch delivery times out, and maintains a complete audit trail linking every vulnerability to its patch and verification result.

### What You Write: Workers

Four workers secure the IoT network: ScanDevicesWorker discovers connected devices, DetectVulnerabilitiesWorker checks for CVEs and expired certificates, PatchWorker deploys security fixes, and VerifyWorker confirms remediation succeeded.

| Worker | Task | What It Does |
|---|---|---|
| **DetectVulnerabilitiesWorker** | `ios_detect_vulnerabilities` | Checks discovered devices for expired certificates, default credentials, and known CVEs. |
| **PatchWorker** | `ios_patch` | Pushes security patches to affected devices and tracks patch application status. |
| **ScanDevicesWorker** | `ios_scan_devices` | Scans the IoT network to discover all connected devices and their firmware versions. |
| **VerifyWorker** | `ios_verify` | Re-scans patched devices to confirm vulnerabilities are remediated and security baseline is met. |

the workflow and alerting logic stay the same.

### The Workflow

```
ios_scan_devices
 │
 ▼
ios_detect_vulnerabilities
 │
 ▼
ios_patch
 │
 ▼
ios_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
