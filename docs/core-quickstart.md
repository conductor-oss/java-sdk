# Core workflow and worker quickstart

**Audience:** Java developers building their first Conductor workflow worker.  
**Works with:** OSS and Orkes.  
**Prerequisites:** Java 21+, Docker or the Conductor CLI, and a shell.

This is the CI-smoke-tested Hello World path. It registers a task definition and workflow, starts a Java worker, executes the workflow, and shuts the worker down.

## Run it

Start a local server once:

```bash
conductor server start
export CONDUCTOR_SERVER_URL=http://localhost:8080/api
```

Run the checked-in example:

```bash
cd examples/basics/hello-world
./run.sh
```

Expected output:

```text
Status: COMPLETED
Output: {greeting=Hello, Developer! Welcome to Conductor.}
Result: PASSED
```

Open the execution in the local UI at `http://localhost:8080`. When you are finished, stop the local server:

```bash
conductor server stop
```

If `CONDUCTOR_SERVER_URL` is unset, `run.sh` instead starts the example's Docker Compose server. Stop that server with `docker compose down` in this directory.

## What it proves

- A `SIMPLE` task definition, workflow task name, and Java worker task name match.
- A live worker polls the task and returns output.
- The workflow result is retrieved and checked.

## Common failures

- `SCHEDULED` task: the worker is not polling the exact task name; see [workers](workers.md).
- Connection failure: verify the endpoint ends in `/api`; see [connection and authentication](connection-authentication.md).
- Repeated side effect: make the worker idempotent before using the pattern in production; see [reliability](reliability.md).

Next, define a real [workflow](workflows.md), make the worker [reliable and scalable](reliability.md), and add an integration [test](workflow-testing.md).
