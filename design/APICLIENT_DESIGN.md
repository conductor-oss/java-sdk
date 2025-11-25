# ApiClient & Authentication Design Document

## Table of Contents
- [Overview](#overview)
- [Requirements](#requirements)
- [Architecture](#architecture)
- [Token Management](#token-management)
- [Environment Variables & Configuration](#environment-variables--configuration)
- [Request Lifecycle](#request-lifecycle)
- [Current Limitations](#current-limitations)
- [Proposed Improvements](#proposed-improvements)
- [Java SDK Implementation Reference](#java-sdk-implementation-reference)

---

## Overview

This document outlines the design and requirements for building a Conductor API client with authentication support. The design is language-agnostic and provides a blueprint for implementing HTTP clients that can reliably interact with Conductor servers, particularly those requiring JWT-based authentication (Orkes Conductor).

ApiClient is the base client which is used by all the other SDK clients to make requests to the Conductor server. 

### Key Design Goals

- **Reliability**: Automatic token refresh prevents authentication failures
- **Performance**: Connection pooling and HTTP/2 support for efficient communication
- **Flexibility**: Environment-based configuration with sensible defaults
- **Extensibility**: Plugin architecture for custom header injection and middleware
- **Security**: Secure credential handling and token management
- **Compatibility**: Works with both Orkes Conductor (authenticated) and OSS Conductor (open)

---

## Requirements

### Functional Requirements

#### FR-1: HTTP Client Capabilities

**Required:**
- Support HTTP/1.1 and HTTP/2 protocols
- Handle standard HTTP methods: GET, POST, PUT, DELETE, PATCH
- Support request/response with JSON payloads
- Handle URL path parameter substitution (e.g., `/workflow/{workflowId}`)
- Support query parameters
- Custom header injection
- Connection pooling for performance
- Configurable timeouts (connect, read, write, call)
- Virtual threads support (for languages that support lightweight concurrency)

**Example:**
```
GET /api/workflow/{workflowId}?includeTasks=true
Headers: X-Authorization: Bearer <token>
```

#### FR-2: Authentication & Token Management

**Required:**
- JWT token-based authentication using keyId/keySecret credentials
- Token acquisition via `POST /token` endpoint
- Token caching with time-based expiration
- Automatic injection of `X-Authorization` header on all requests (except `/token` endpoint)
- Skip authentication for open-source Conductor servers (no credentials)
- Backward compatible configuration options (support legacy environment variable names)

**Token Request Format:**
```json
POST /token
{
  "keyId": "your-key-id",
  "keySecret": "your-secret"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### FR-3: Proactive Token Refresh

**Required:**
- Background scheduled task to refresh tokens before expiration
- Default refresh interval: 45 minutes (2700 seconds)
- Configurable refresh interval via environment variable or configuration
- Refresh should occur at `max(30s, refreshInterval - 30s)` to ensure tokens are refreshed before expiration
- Daemon/background thread that doesn't block application shutdown
- Initial token fetch during client initialization

**Rationale:** Prevents authentication failures due to expired tokens during normal operation.

#### FR-4: Token Caching Strategy

**Required:**
- Single token cache (one token per client instance)
- Time-based expiration matching refresh interval
- Thread-safe access to cached token
- Automatic refresh on cache miss/expiration
- Cache invalidation capability (for future 401 retry support)

**Dual Refresh Mechanism:**
1. **Scheduled Proactive Refresh** - Background task refreshes token periodically
2. **On-Demand Refresh** - Cache expiration triggers automatic refresh as backup

#### FR-5: Request/Response Handling

**Required:**
- Serialize request bodies to JSON
- Deserialize response bodies from JSON
- Support for typed responses (generic/parameterized types)
- Handle empty response bodies (204 No Content)
- Support for binary payloads (e.g., `application/octet-stream`)
- URL encoding for path and query parameters

#### FR-6: Error Handling

**Required:**
- Detect non-2xx HTTP status codes as errors
- Parse error responses from server (if structured)
- Throw/return language-appropriate exceptions/errors with:
  - HTTP status code
  - Response body
  - Response headers
  - Error message
- Handle network errors (timeouts, connection failures)
- Handle JSON deserialization errors

**Standard Error Response Format:**
```json
{
  "status": 404,
  "message": "Workflow not found",
  "instance": "workflow-id-123"
}
```

#### FR-7: Environment Variable Support

**Required:**
- Support zero-config initialization using environment variables
- Required environment variables:
  - `CONDUCTOR_SERVER_URL` - Server base URL
  - `CONDUCTOR_AUTH_KEY` - Authentication key ID
  - `CONDUCTOR_AUTH_SECRET` - Authentication secret
- Optional environment variables:
  - `CONDUCTOR_SECURITY_TOKEN_REFRESH_INTERVAL` - Token refresh interval in seconds
- Backward compatibility variables (if applicable):
  - `CONDUCTOR_SERVER_AUTH_KEY`
  - `CONDUCTOR_SERVER_AUTH_SECRET`

**Configuration Priority:**
1. Explicit constructor/builder parameters
2. Environment variables
3. System properties (language-specific)
4. Default values

#### FR-8: Builder/Configuration Pattern

**Required:**
- Fluent API for client configuration
- Support configuring:
  - Base URL/path
  - Credentials (keyId, keySecret)
  - Timeouts (connect, read, write)
  - Connection pool settings
  - Custom headers
  - SSL/TLS configuration
- Validation of required parameters before client creation

**Example Configuration API:**
```
client = Client.builder()
    .basePath("https://play.orkes.io/api")
    .credentials(keyId, secret)
    .connectTimeout(10_000)
    .readTimeout(30_000)
    .build()
```

#### FR-9: Extensibility via Header Suppliers

**Required:**
- Plugin/middleware mechanism for injecting custom headers
- Header suppliers should receive:
  - HTTP method
  - Request path
- Header suppliers should return key-value header map
- Support multiple header suppliers with merge/override logic
- Request-specific headers override supplier headers

**Use Cases:**
- Authentication (token injection)
- Request ID propagation
- Custom API keys
- Correlation IDs
- Tenant identifiers

### Non-Functional Requirements

#### NFR-1: Thread Safety

**Required:**
- All client operations must be thread-safe
- Token cache must be thread-safe
- Support concurrent requests from multiple threads
- No race conditions in token refresh logic
- Use appropriate synchronization primitives (locks, atomic operations, concurrent collections)

#### NFR-2: Performance

**Required:**
- Connection pooling to reuse HTTP connections
- Keep-alive support for persistent connections
- Non-blocking token refresh (shouldn't block requests)
- Minimal overhead for token caching (~O(1) lookup)
- Efficient JSON serialization/deserialization
- HTTP/2 multiplexing support for concurrent requests

**Targets:**
- Token cache lookup: < 1ms
- Token refresh: < 500ms (depends on network)
- Request overhead (excluding network): < 10ms

#### NFR-3: Reliability

**Required:**
- Handle transient network failures gracefully
- Log authentication failures for debugging
- Validate configuration at initialization time
- Fail fast on invalid configuration
- Clear error messages for common issues

**Recommended (Future Enhancement):**
- Automatic retry on transient failures (network errors, 5xx responses)
- Exponential backoff for retries
- Automatic token refresh on 401 responses
- Circuit breaker for failing servers

#### NFR-4: Observability

**Required:**
- Structured logging for:
  - Token refresh events (success/failure)
  - Authentication failures
  - Configuration initialization
- Log levels:
  - DEBUG: Token refresh timing, request details
  - INFO: Client initialization, configuration
  - WARN: Authentication failures, retries
  - ERROR: Fatal errors, configuration issues

**Recommended (Future Enhancement):**
- Metrics/telemetry for:
  - Token refresh success/failure rate
  - Token refresh duration
  - Request success/failure rate
  - Request duration
  - HTTP status code distribution
- Request/response logging capability
- Distributed tracing support (trace ID propagation)

#### NFR-5: Security

**Required:**
- HTTPS support with TLS 1.2+
- Certificate validation by default
- Secure credential storage (no logging of secrets)
- Option to disable SSL verification (for development only, with warnings)
- No exposure of credentials in error messages or logs
- Clear separation of authentication logic

**Best Practices:**
- Never log keySecret or token values
- Use secure random for any randomization needs
- Support custom SSL/TLS configurations
- Validate server certificates

### Optional Requirements

#### OR-1: Async/Non-Blocking Support

**Optional but Recommended:**
- Async API for non-blocking request execution
- Return futures/promises/async types
- Callback-based API for async operations
- Support for language-specific async patterns (async/await, coroutines, etc.)

#### OR-2: Retry Mechanism

**Recommended:**
- Configurable retry policy
- Exponential backoff strategy
- Retry on specific status codes (408, 429, 500, 502, 503, 504)
- Retry on network errors
- Maximum retry attempts configuration
- Retry on 401 with **automatic** token refresh

### Compliance & Compatibility

#### API Compatibility

**Required:**
- Compatible with Conductor OSS (no authentication required)
- Compatible with Orkes Conductor (JWT authentication)
- Support latest Conductor REST API version
- Backward compatible configuration options

#### Version Support

**Recommended:**
- Support for multiple Conductor API versions
- Graceful degradation for missing features
- Version detection/negotiation

---

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Application Layer                        │
│  ┌──────────────────┐  ┌──────────────────┐                     │
│  │  WorkflowClient  │  │   TaskClient     │  ...                │
│  └────────┬─────────┘  └────────┬─────────┘                     │
│           │                     │                               │
└───────────┼─────────────────────┼───────────────────────────────┘
            │                     │
            └──────────┬──────────┘
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                      ApiClient / ConductorClient                │
│  • Request building                                             │
│  • Response handling                                            │
│  • Serialization/Deserialization                                │
│  • Error handling                                               │
└─────────────────────────────────┬───────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                     HeaderSupplier Chain                        │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │         OrkesAuthentication                               │  │
│  │  • Token management                                       │  │
│  │  • Scheduled refresh                                      │  │
│  │  • Header injection                                       │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────┬───────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      HTTP Client Library                        │
│  • Connection pooling                                           │
│  • Timeouts & retries                                           │
│  • Protocol negotiation (HTTP/1.1, HTTP/2)                      │
│  • SSL/TLS                                                      │
└─────────────────────────────────┬───────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Conductor Server                              │
│  • /token endpoint (token generation)                           │
│  • /api/* endpoints (workflow, task, etc.)                      │
└─────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

**API Client**
- Coordinate request/response lifecycle
- Apply configuration (timeouts, base URL, etc.)
- Execute HTTP calls (sync and async)
- Provide builder/configuration interface

**Authentication Module**
- Manage JWT token lifecycle
- Schedule proactive token refresh
- Cache tokens efficiently
- Inject authentication headers

**Header Supplier Chain**
- Plugin architecture for header injection
- Support multiple suppliers with merge logic
- Receive context (method, path) for dynamic headers

**HTTP Client Library**
- Handle low-level HTTP communication
- Manage connection pooling
- Handle SSL/TLS
- Support HTTP/1.1 and HTTP/2

**Token Resource**
- Abstract `/token` endpoint calls
- Handle token request/response serialization

---

## Token Management

### Token Lifecycle

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Client Initialization                                        │
│    ApiClient.builder().credentials(keyId, secret).build()       │
│    └─→ Creates OrkesAuthentication instance                     │
└────────────────────────┬────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. HeaderSupplier Initialization                                │
│    OrkesAuthentication.init(client)                             │
│    ├─→ Creates TokenResource                                    │
│    ├─→ Schedules refresh task                                   │
│    └─→ Fetches initial token                                    │
└────────────────────────┬────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. Initial Token Fetch                                          │
│    refreshToken()                                               │
│    ├─→ POST /token with keyId/keySecret                         │
│    ├─→ Receive JWT token                                        │
│    └─→ Cache token for tokenRefreshInSeconds                    │
└────────────────────────┬────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. Request Execution                                            │
│    client.execute(request)                                      │
│    ├─→ buildRequest() calls HeaderSupplier.get()               │
│    ├─→ OrkesAuthentication.get() returns cached token           │
│    └─→ Request sent with X-Authorization header                 │
└────────────────────────┬────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. Background Refresh (Scheduled)                               │
│    Every (tokenRefreshInSeconds - 30) seconds:                  │
│    ├─→ refreshToken() called by ScheduledExecutorService        │
│    ├─→ POST /token with keyId/keySecret                         │
│    ├─→ Receive new JWT token                                    │
│    └─→ Update cache with new token                              │
└─────────────────────────────────────────────────────────────────┘
                         ▼
                   [Loop back to step 4]
```

### Token Refresh Strategy

#### Dual Refresh Mechanism

1. **Scheduled Proactive Refresh**
   - Background thread via `ScheduledExecutorService`
   - Runs at interval: `max(30, tokenRefreshInSeconds - 30)` seconds
   - Ensures token is refreshed BEFORE expiration
   - Example: If `tokenRefreshInSeconds = 2700` (45 min), refresh every 44.5 min

2. **On-Demand Refresh**
   - If the server sends back 401 with expired token error, then refresh the token



## Environment Variables & Configuration

### Environment Variables

#### Server Configuration

If the server URL is not specified, it should default to http://localhost:8080/api
```bash
# Conductor server URL (required when using env vars)
export CONDUCTOR_SERVER_URL="https://developer.orkescloud.com/api"
```

#### Authentication

```bash
# Current variables
export CONDUCTOR_AUTH_KEY="your-key-id"
export CONDUCTOR_AUTH_SECRET="your-key-secret"

# Legacy variables (backward compatibility)
export CONDUCTOR_SERVER_AUTH_KEY="your-key-id"
export CONDUCTOR_SERVER_AUTH_SECRET="your-key-secret"
```

Note: If the auth key and secret are not present, then authentication is disabled and client can make requests without any jwt token.

#### Token Refresh

```bash
# Override default token refresh interval (seconds)
export CONDUCTOR_SECURITY_TOKEN_REFRESH_INTERVAL="3600"
```

### Configuration Priority

For each setting, the priority order (highest to lowest):

1. **Explicit Constructor/Builder Parameters**
   ```java
   ApiClient.builder()
       .basePath("https://custom.conductor.io/api")
       .credentials(keyId, secret)
       .build();
   ```

2. **Environment Variables**
   ```java
   ApiClient client = new ApiClient(); // Uses env vars
   ```

3. **System Properties**
   ```bash
   java -DCONDUCTOR_SECURITY_TOKEN_REFRESH_INTERVAL=3600 -jar app.jar
   ```

4. **Default Values**
   - Base path: `http://localhost:8080/api`
   - Token refresh: `2700` seconds (45 minutes)

### Configuration Examples

#### Example 1: Zero-Config with Environment Variables

```bash
export CONDUCTOR_SERVER_URL="https://developer.orkescloud.com/api"
export CONDUCTOR_AUTH_KEY="abc123"
export CONDUCTOR_AUTH_SECRET="secret456"
```

```java
ApiClient client = new ApiClient();
// Automatically uses environment variables
```

#### Example 2: Explicit Configuration

```java
ApiClient client = ApiClient.builder()
    .basePath("https://play.orkes.io/api")
    .credentials("abc123", "secret456")
    .connectTimeout(10000)
    .readTimeout(30000)
    .writeTimeout(30000)
    .build();
```

#### Example 3: Mixed Configuration

```bash
export CONDUCTOR_SERVER_URL="https://play.orkes.io/api"
export CONDUCTOR_SECURITY_TOKEN_REFRESH_INTERVAL="1800"
```

```java
ApiClient client = ApiClient.builder()
    .useEnvVariables(true)
    .credentials("abc123", "secret456") // Override auth from env
    .build();
```

#### Example 4: Custom Token Refresh Interval

```java
ApiClient client = ApiClient.builder()
    .basePath("https://play.orkes.io/api")
    .credentials("abc123", "secret456")
    .addHeaderSupplier(new OrkesAuthentication(
        "abc123",
        "secret456",
        1800  // Custom: 30 minutes
    ))
    .build();
```

---

## Request Lifecycle

### Complete Request Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Application Code                                             │
│    workflowClient.startWorkflow(request)                        │
└────────────────────────┬────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. Client API Layer                                             │
│    ConductorClientRequest.builder()                             │
│        .method(POST)                                            │
│        .path("/workflow/{name}")                                │
│        .pathParams(List.of(Param("name", "my_workflow")))       │
│        .body(startRequest)                                      │
│        .build()                                                 │
└────────────────────────┬────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. ConductorClient.execute()                                    │
│    └─→ buildRequest()                                           │
│        ├─→ replacePathParams()                                  │
│        │   /workflow/{name} → /workflow/my_workflow             │
│        ├─→ buildUrl()                                           │
│        │   https://conductor.io/api/workflow/my_workflow        │
│        ├─→ addHeadersFromProviders()                            │
│        │   ├─→ OrkesAuthentication.get()                        │
│        │   │   └─→ tokenCache.get()                             │
│        │   │       └─→ refreshToken() if expired                │
│        │   └─→ Returns {"X-Authorization": "Bearer eyJ..."}     │
│        ├─→ processHeaderParams()                                │
│        │   Adds all headers to request                          │
│        ├─→ serialize()                                          │
│        │   Converts body to JSON                                │
│        └─→ Request.Builder.build()                              │
└────────────────────────┬────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. OkHttp Execution                                             │
│    okHttpClient.newCall(request)                                │
│    └─→ call.execute()                                           │
│        ├─→ Connection pool management                           │
│        ├─→ SSL/TLS handshake                                    │
│        ├─→ HTTP/2 negotiation                                   │
│        └─→ Send request                                         │
└────────────────────────┬────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. Conductor Server                                             │
│    ├─→ Validate X-Authorization JWT                            │
│    ├─→ Process request                                          │
│    └─→ Return response                                          │
└────────────────────────┬────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. ConductorClient.handleResponse()                             │
│    ├─→ Check response.isSuccessful()                            │
│    ├─→ If error (non-2xx):                                      │
│    │   ├─→ Read error body                                      │
│    │   ├─→ Try deserialize as ConductorClientException          │
│    │   └─→ Throw exception                                      │
│    └─→ If success:                                              │
│        ├─→ deserialize() response body                          │
│        └─→ Return typed result                                  │
└────────────────────────┬────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 7. Application Code                                             │
│    String workflowId = response.getData();                      │
└─────────────────────────────────────────────────────────────────┘
```

### Error Handling Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ Request Execution                                               │
│ call.execute()                                                  │
└────────────────────────┬────────────────────────────────────────┘
                         ▼
                   Is Successful?
                    (2xx status)
                         │
            ┌────────────┴────────────┐
            │                         │
           Yes                       No
            │                         │
            ▼                         ▼
    ┌───────────────┐     ┌──────────────────────┐
    │ Deserialize   │     │ Read Error Body      │
    │ Response      │     └──────────┬───────────┘
    │ Return Result │                ▼
    └───────────────┘     ┌──────────────────────────────────┐
                          │ Try Deserialize as               │
                          │ ConductorClientException         │
                          └──────────┬───────────────────────┘
                                     ▼
                              Deserialization
                               Successful?
                                     │
                        ┌────────────┴────────────┐
                        │                         │
                       Yes                       No
                        │                         │
                        ▼                         ▼
            ┌────────────────────┐   ┌─────────────────────────┐
            │ Throw Specific     │   │ Throw Generic           │
            │ ConductorClient    │   │ ConductorClientException│
            │ Exception with     │   │ with:                   │
            │ structured error   │   │ • Status code           │
            │ from server        │   │ • Response body         │
            └────────────────────┘   │ • Headers               │
                                     └─────────────────────────┘
```

**Exception Hierarchy**:

```
Throwable
 └── Exception
      └── RuntimeException
           └── ConductorClientException
                ├── status: int
                ├── message: String
                ├── responseHeaders: Map<String, List<String>>
                └── responseBody: String
```

