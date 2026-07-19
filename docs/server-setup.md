# Start a local Conductor server

Use the [Conductor CLI](https://github.com/conductor-oss/conductor-cli) for local development. It is the recommended path because the same CLI also creates workflows, starts executions, inspects status, and manages schedules.

Prerequisites: Java 21+ (the CLI runs the local server JAR) and Node.js/npm.

## Recommended: CLI

Install the CLI with npm, then start the local server:

```bash
npm install -g @conductor-oss/conductor-cli
conductor server start
```

Verify it and point SDK applications at its API:

```bash
conductor server status
export CONDUCTOR_SERVER_URL=http://localhost:8080/api
```

Use `conductor server stop` when you are finished. See the [CLI repository](https://github.com/conductor-oss/conductor-cli) for alternative installation methods and server options.

## Optional: Docker

Use Docker when you need a containerized server or are running an example that includes a `docker-compose.yml`:

```bash
docker run --rm -p 8080:8080 -p 1234:5000 conductoross/conductor:latest
```

The API is `http://localhost:8080/api` and the UI is `http://localhost:1234`.

## Use an existing server

Set `CONDUCTOR_SERVER_URL` to the server API endpoint and, when required, set `CONDUCTOR_AUTH_KEY` and `CONDUCTOR_AUTH_SECRET`. Do not put credentials in source code or workflow input.

## Give your coding agent Conductor context

[Conductor Skills](https://github.com/conductor-oss/conductor-skills) teaches supported coding agents how to create, run, inspect, and manage Conductor workflows. Install it with npm and load it for detected agents:

```bash
npm install -g @conductor-oss/conductor-skills
conductor-skills --all
```
