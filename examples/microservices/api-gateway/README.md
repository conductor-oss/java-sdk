# API Gateway with Authentication and Response Transformation

Every incoming API request needs authentication, routing to the right backend, and response
transformation before it reaches the client. Without a gateway, each service duplicates auth
logic and clients get inconsistent response formats. This workflow authenticates the caller,
routes the request, transforms the response per client tier, and sends it back.

## Workflow

```
endpoint, method, payload
          |
          v
+--------------------+     +--------------------+     +---------------------------+     +--------------------+
| ag_authenticate    | --> | ag_route_request   | --> | ag_transform_response     | --> | ag_send_response   |
+--------------------+     +--------------------+     +---------------------------+     +--------------------+
  clientId:                  statusCode: 200           transformed response            final response with
  client-enterprise-01       backendLatency: 45ms      per client tier                 headers & caching
  tier: premium
  rateLimit: 10000
```

## Workers

**AgAuthenticateWorker** -- Validates the API key. Returns `clientId: "client-enterprise-01"`,
`tier: "premium"`, and `rateLimit: 10000`.

**RouteRequestWorker** -- Routes the `method` + `endpoint` to the backend. Returns
`statusCode: 200` with `backendLatency: 45`ms and the raw response.

**TransformResponseWorker** -- Transforms the response for the client's tier level.

**SendResponseWorker** -- Sends the final response with status code, body, and headers.

## Tests

4 unit tests cover authentication, routing, transformation, and response delivery.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
