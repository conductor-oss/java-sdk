# Connection and authentication

**Audience:** SDK users connecting an application to OSS or Orkes.  
**Security:** keep credentials in environment variables or your platform secret store; never in workflow input, source, or logs.

## OSS

For local development, follow [server setup](server-setup.md), then point the application at the API endpoint:

```bash
export CONDUCTOR_SERVER_URL=http://localhost:8080/api
```

For a remote OSS deployment, set the same variable to its HTTPS API endpoint. Confirm it includes `/api`.

## Orkes

Set the server URL and credentials in the environment used to launch the application:

```bash
export CONDUCTOR_SERVER_URL=https://your-tenant.orkesconductor.com/api
export CONDUCTOR_AUTH_KEY=your-key-id
export CONDUCTOR_AUTH_SECRET=your-secret-value
```

Use your deployment platform's secret injection mechanism for the last two values. Rotate credentials through that platform and restart workers safely after rotation.

## Verify before debugging code

Use the CLI against the same endpoint and credentials:

```bash
conductor workflow list
```

Expected result: the server returns workflow definitions or an empty list. A `401` or `403` is an authorization problem, while a connection error normally means a URL, network, TLS, or proxy problem.

Next: [core quickstart](core-quickstart.md) or [agent quickstart](agents/getting-started.md).
