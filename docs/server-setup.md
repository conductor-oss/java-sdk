# Connect the SDK to a Conductor server

Choose the hosted Orkes Developer Edition by default, or run Conductor locally for a self-managed development environment.

## Recommended default: Orkes Developer Edition

Sign in to [Orkes Developer Edition](https://developer.orkescloud.com/), create an application and access key, then configure the SDK with the hosted API endpoint. Do not commit the key or secret to source control.

```bash
export CONDUCTOR_SERVER_URL=https://developer.orkescloud.com/api
export CONDUCTOR_AUTH_KEY=<your-key-id>
export CONDUCTOR_AUTH_SECRET=<your-key-secret>
```

For AI agents, configure the LLM provider integration in the hosted cluster before running an agent. See the [agent getting-started guide](agents/getting-started.md).

## Run locally

Use the [Conductor CLI](https://github.com/conductor-oss/conductor-cli) when you want a local server. The CLI also creates workflows, starts executions, inspects status, and manages schedules.

Prerequisites: Java 21+ (the CLI runs the local server JAR) and Node.js/npm.

### Preferred local path: CLI

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

### Docker fallback

Use Docker when you need a containerized server or are running an example that includes a `docker-compose.yml`:

```bash
docker run --rm -p 8080:8080 -p 1234:5000 conductoross/conductor:latest
```

The API is `http://localhost:8080/api` and the UI is `http://localhost:1234`.

## Use another remote server

Set `CONDUCTOR_SERVER_URL` to the server's `/api` endpoint and, when required, set `CONDUCTOR_AUTH_KEY` and `CONDUCTOR_AUTH_SECRET`. Do not put credentials in source code or workflow input.

## Give your coding agent Conductor context

[Conductor Skills](https://github.com/conductor-oss/conductor-skills) teaches supported coding agents how to create, run, inspect, and manage Conductor workflows. Install it with npm and load it for detected agents:

```bash
npm install -g @conductor-oss/conductor-skills
conductor-skills --all
```
