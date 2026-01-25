## Current Limitations

### 1. No Token Expiry Handling on 401 Responses ⚠️

**Problem**: If a token expires before the scheduled refresh or cache expiration, requests fail permanently.

**Current Behavior**:
```
Request with expired token
→ Server returns 401 Unauthorized
→ ConductorClientException thrown
→ No retry or token refresh
→ Request fails
```

**Missing Logic**:
- No detection of 401 responses
- No cache invalidation on auth failure
- No automatic retry with fresh token

**Impact**:
- Service interruptions during token expiry windows
- Manual intervention required to recover
- Poor user experience

**Scenarios Where This Fails**:
1. Server-side token TTL < client `tokenRefreshInSeconds`
2. Clock skew between client and server
3. Token revoked by server
4. Initial token already partially expired
5. Scheduled refresh task delayed/failed

---

### 2. No Retry Mechanism

**Problem**: Transient network failures or server errors cause immediate request failure.

**Current Behavior**:
```
Request → Network error → IOException → ConductorClientException → Fail
Request → Server 5xx → ConductorClientException → Fail
```

**Missing Features**:
- No automatic retry on transient failures
- No exponential backoff
- No configurable retry policy
- No circuit breaker pattern

**Impact**:
- Reduced reliability in unstable networks
- Increased error rates for transient issues
- Application needs to implement own retry logic

---

### 3. No Circuit Breaker

**Problem**: Continued requests to failing server can worsen outages.

**Missing Features**:
- No failure rate tracking
- No automatic circuit opening
- No half-open state for testing recovery
- No fallback mechanisms

---

### 4. No Request/Response Interceptors

**Problem**: Cannot easily add cross-cutting concerns like logging, metrics, or request modification.

**Current State**:
- OkHttp supports interceptors but not exposed
- No built-in request/response logging
- No request ID propagation
- No metrics collection at HTTP layer

**Workaround**:
```java
ApiClient.builder()
    .configureOkHttp(builder -> {
        builder.addInterceptor(new CustomInterceptor());
    })
    .build();
```

Limited flexibility - cannot access Conductor-specific context.

---

### 5. Limited Observability

**Problem**: Difficult to debug authentication and token refresh issues.

**Missing Features**:
- No metrics on token refresh success/failure
- No events for token expiry
- No visibility into token TTL remaining
- Limited logging of auth failures

---

### 6. Thread Pool Not Configurable

**Problem**: Scheduled token refresh uses default single-thread executor.

**Current**:
```java
private final ScheduledExecutorService tokenRefreshService =
    Executors.newSingleThreadScheduledExecutor(
        new BasicThreadFactory.Builder()
            .namingPattern("OrkesAuthenticationSupplier Token Refresh %d")
            .daemon(true)
            .build()
    );
```

**Issues**:
- Cannot configure thread pool size
- Cannot share executor across multiple clients
- Cannot inject custom executor for testing
- No shutdown hooks (though daemon thread mitigates this)

---

## Proposed Improvements

### 1. Implement Token Expiry Handling with Retry 🚀

Add automatic token refresh and retry on 401 responses.

#### Approach A: OkHttp Interceptor (Recommended)

```java
public class TokenRefreshInterceptor implements Interceptor {

    private final OrkesAuthentication auth;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    public TokenRefreshInterceptor(OrkesAuthentication auth) {
        this.auth = auth;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        // First attempt
        Response response = chain.proceed(request);

        // If 401 and not already refreshing, try to refresh token
        if (response.code() == 401 && !refreshing.get()) {
            synchronized (this) {
                if (refreshing.compareAndSet(false, true)) {
                    try {
                        response.close(); // Close original response

                        // Invalidate cached token
                        auth.invalidateToken();

                        // Force refresh
                        String newToken = auth.getToken();

                        if (newToken != null) {
                            // Retry with new token
                            Request newRequest = request.newBuilder()
                                .header("X-Authorization", newToken)
                                .build();
                            return chain.proceed(newRequest);
                        }
                    } finally {
                        refreshing.set(false);
                    }
                }
            }
        }

        return response;
    }
}
```

**Add to OrkesAuthentication**:

```java
public void invalidateToken() {
    tokenCache.invalidateAll();
}
```

**Registration**:

```java
@Override
public void init(ConductorClient client) {
    this.tokenResource = new TokenResource(client);
    scheduleTokenRefresh();

    // Register interceptor
    if (client instanceof ApiClient) {
        // Access OkHttpClient and add interceptor
        // This requires exposing OkHttpClient or accepting it in init()
    }

    try {
        getToken();
    } catch (Throwable t) {
        LOGGER.error(t.getMessage(), t);
    }
}
```

**Pros**:
- ✅ Automatic retry on 401
- ✅ Token invalidation before refresh
- ✅ Prevents concurrent refresh attempts
- ✅ Works for all requests automatically

**Cons**:
- ⚠️ Requires access to OkHttpClient
- ⚠️ Need to handle interceptor ordering
- ⚠️ Only retries once (not multiple times)

#### Approach B: Enhanced Response Handler

Modify `ConductorClient.handleResponse()` to detect 401 and trigger retry.

```java
protected <T> T handleResponse(Response response, Type returnType) {
    if (!response.isSuccessful()) {
        // Check for 401 Unauthorized
        if (response.code() == 401) {
            // Notify HeaderSuppliers of auth failure
            for (HeaderSupplier supplier : headerSuppliers) {
                if (supplier instanceof OrkesAuthentication) {
                    ((OrkesAuthentication) supplier).onAuthFailure();
                }
            }
        }

        String respBody = bodyAsString(response);
        // ... rest of error handling
    }

    // ... rest of success handling
}
```

**Add to OrkesAuthentication**:

```java
public void onAuthFailure() {
    LOGGER.warn("Authentication failure detected, invalidating token cache");
    tokenCache.invalidateAll();
}
```

**Pros**:
- ✅ Simple implementation
- ✅ No interceptor complexity
- ✅ Works with existing architecture

**Cons**:
- ❌ No automatic retry (still throws exception)
- ❌ Application must handle retry logic
- ❌ Only invalidates cache, doesn't retry

#### Approach C: Retry at Execute Level (Most Comprehensive)

Wrap execution with retry logic.

```java
private <T> ConductorClientResponse<T> executeWithRetry(Call call, Type returnType) {
    int maxRetries = 2;
    int attempt = 0;

    while (attempt <= maxRetries) {
        try {
            Response response = call.clone().execute();

            // Check for 401 on first attempt
            if (response.code() == 401 && attempt == 0) {
                response.close();

                // Invalidate token and refresh
                for (HeaderSupplier supplier : headerSuppliers) {
                    if (supplier instanceof OrkesAuthentication) {
                        ((OrkesAuthentication) supplier).invalidateToken();
                    }
                }

                // Rebuild request with new token
                Request newRequest = buildRequest(/* same params */);
                call = okHttpClient.newCall(newRequest);
                attempt++;
                continue;
            }

            T data = handleResponse(response, returnType);
            return new ConductorClientResponse<>(
                response.code(),
                response.headers().toMultimap(),
                data
            );

        } catch (IOException e) {
            attempt++;
            if (attempt > maxRetries) {
                throw new ConductorClientException(e);
            }
            // Exponential backoff
            Thread.sleep((long) Math.pow(2, attempt) * 1000);
        }
    }

    throw new ConductorClientException("Max retries exceeded");
}
```

**Pros**:
- ✅ Full retry logic with token refresh
- ✅ Can retry on other transient errors (network, 5xx)
- ✅ Exponential backoff support
- ✅ Configurable retry policy

**Cons**:
- ⚠️ More complex implementation
- ⚠️ Need to handle call cloning properly
- ⚠️ Need to rebuild request with new headers

---

### 2. Add Retry Mechanism with Exponential Backoff

Implement configurable retry policy for transient failures.

#### Configuration API

```java
public class RetryConfig {
    private final int maxRetries;
    private final long initialBackoffMs;
    private final long maxBackoffMs;
    private final double backoffMultiplier;
    private final Set<Integer> retryableStatusCodes;
    private final boolean retryOnNetworkError;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int maxRetries = 3;
        private long initialBackoffMs = 1000;
        private long maxBackoffMs = 30000;
        private double backoffMultiplier = 2.0;
        private Set<Integer> retryableStatusCodes = Set.of(408, 429, 500, 502, 503, 504);
        private boolean retryOnNetworkError = true;

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder initialBackoffMs(long initialBackoffMs) {
            this.initialBackoffMs = initialBackoffMs;
            return this;
        }

        public Builder maxBackoffMs(long maxBackoffMs) {
            this.maxBackoffMs = maxBackoffMs;
            return this;
        }

        public Builder backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        public Builder retryableStatusCodes(Set<Integer> retryableStatusCodes) {
            this.retryableStatusCodes = retryableStatusCodes;
            return this;
        }

        public Builder retryOnNetworkError(boolean retryOnNetworkError) {
            this.retryOnNetworkError = retryOnNetworkError;
            return this;
        }

        public RetryConfig build() {
            return new RetryConfig(this);
        }
    }
}
```

#### Usage

```java
ApiClient client = ApiClient.builder()
    .basePath("https://conductor.io/api")
    .credentials(keyId, secret)
    .retryConfig(RetryConfig.builder()
        .maxRetries(3)
        .initialBackoffMs(1000)
        .maxBackoffMs(30000)
        .backoffMultiplier(2.0)
        .retryableStatusCodes(Set.of(408, 429, 500, 502, 503, 504))
        .retryOnNetworkError(true)
        .build())
    .build();
```

---

### 3. Add Circuit Breaker Support

Implement circuit breaker pattern to prevent cascading failures.

#### Integration with Resilience4j

```java
public class CircuitBreakerConfig {
    private final float failureRateThreshold;
    private final int minimumNumberOfCalls;
    private final Duration waitDurationInOpenState;
    private final int permittedNumberOfCallsInHalfOpenState;

    // Builder pattern...
}
```

#### Usage

```java
CircuitBreaker circuitBreaker = CircuitBreaker.of("conductor-api",
    CircuitBreakerConfig.custom()
        .failureRateThreshold(50)
        .minimumNumberOfCalls(10)
        .waitDurationInOpenState(Duration.ofSeconds(60))
        .permittedNumberOfCallsInHalfOpenState(5)
        .build());

ApiClient client = ApiClient.builder()
    .basePath("https://conductor.io/api")
    .credentials(keyId, secret)
    .circuitBreaker(circuitBreaker)
    .build();
```

---

### 4. Enhanced Observability

Add metrics, logging, and events for authentication and requests.

#### Metrics

```java
public interface ApiClientMetrics {
    void recordTokenRefresh(boolean success, Duration duration);
    void recordRequest(String method, String path, int statusCode, Duration duration);
    void recordAuthFailure(String reason);
}
```

#### Usage

```java
ApiClient client = ApiClient.builder()
    .basePath("https://conductor.io/api")
    .credentials(keyId, secret)
    .metrics(new PrometheusApiClientMetrics())
    .build();
```

**Metrics Exposed**:
- `conductor_api_token_refresh_total` (counter)
- `conductor_api_token_refresh_duration_seconds` (histogram)
- `conductor_api_request_total` (counter) - labeled by method, path, status
- `conductor_api_request_duration_seconds` (histogram)
- `conductor_api_auth_failure_total` (counter)

---

### 5. Request/Response Interceptors

Expose interceptor registration API.

```java
public interface RequestInterceptor {
    void beforeRequest(Request.Builder requestBuilder);
}

public interface ResponseInterceptor {
    void afterResponse(Response response);
}
```

#### Usage

```java
ApiClient client = ApiClient.builder()
    .basePath("https://conductor.io/api")
    .credentials(keyId, secret)
    .requestInterceptor(request -> {
        // Add request ID
        request.header("X-Request-ID", UUID.randomUUID().toString());
    })
    .responseInterceptor(response -> {
        // Log response
        LOGGER.info("Response: {} - {}", response.code(), response.request().url());
    })
    .build();
```

---

### 6. Token Refresh Visibility

Add methods to inspect token state.

```java
public class OrkesAuthentication implements HeaderSupplier {

    // ... existing code ...

    public boolean isTokenCached() {
        return tokenCache.getIfPresent(TOKEN_CACHE_KEY) != null;
    }

    public Optional<String> getCachedToken() {
        return Optional.ofNullable(tokenCache.getIfPresent(TOKEN_CACHE_KEY));
    }

    public void forceRefresh() {
        tokenCache.invalidateAll();
        getToken(); // Force refresh
    }

    public long getTokenRefreshInterval() {
        return tokenRefreshInSeconds;
    }

    public Instant getLastRefreshTime() {
        return lastRefreshTime; // Add field to track this
    }

    public Instant getNextRefreshTime() {
        return lastRefreshTime.plusSeconds(tokenRefreshInSeconds - 30);
    }
}
```

---

## Java SDK Implementation Reference

This section provides implementation details specific to the Java SDK as a reference for developers working with or porting the design to other languages.

### Technology Stack

- **HTTP Client**: OkHttp 4.x
- **Caching**: Google Guava Cache
- **Threading**: Java ScheduledExecutorService (with virtual thread support)
- **JSON**: Jackson ObjectMapper
- **Concurrency**: Thread-safe collections (ConcurrentHashMap, CopyOnWriteArrayList)

### Core Classes

#### 1. ApiClient

**Location**: `orkes-client/src/main/java/io/orkes/conductor/client/ApiClient.java`

**Purpose**: Main HTTP client extending `ConductorClient` with authentication support.

**Key Design**:
- Extends `ConductorClient` for backward compatibility
- Builder pattern via `ApiClientBuilder` inner class
- Three constructors: with credentials, without credentials, from environment variables
- Delegates core HTTP operations to parent class
- Converts between v2 API types (`Pair`) and new API types (`Param`)

**Example Usage**:
```java
// From environment variables
ApiClient client = new ApiClient();

// With explicit configuration
ApiClient client = ApiClient.builder()
    .basePath("https://play.orkes.io/api")
    .credentials(keyId, secret)
    .connectTimeout(10000)
    .build();
```

---

#### 2. ConductorClient

**Location**: `conductor-client/src/main/java/com/netflix/conductor/client/http/ConductorClient.java`

**Purpose**: Base HTTP client providing core functionality.

**Key Design**:
- Manages OkHttpClient lifecycle
- Builds requests with path/query parameter substitution
- Serializes/deserializes JSON, text, and binary payloads
- Collects headers from HeaderSupplier chain
- Converts HTTP errors to `ConductorClientException`
- Builder pattern for configuration

**Request Flow**:
1. `execute()` → `buildRequest()` → `addHeadersFromProviders()` → OkHttp
2. Response → `handleResponse()` → deserialize or throw exception

---

#### 3. OrkesAuthentication

**Location**: `orkes-client/src/main/java/io/orkes/conductor/client/http/OrkesAuthentication.java`

**Purpose**: Implements `HeaderSupplier` for JWT token management.

**Key Design**:
- Guava Cache for token storage (single entry, time-based expiration)
- `ScheduledExecutorService` for background refresh
- Daemon thread doesn't block JVM shutdown
- Loader pattern: `tokenCache.get(KEY, this::refreshToken)`
- Refresh interval: `max(30, tokenRefreshInSeconds - 30)` seconds

**Implementation Details**:
```java
// Token cache with automatic expiration
this.tokenCache = CacheBuilder.newBuilder()
    .expireAfterWrite(this.tokenRefreshInSeconds, TimeUnit.SECONDS)
    .build();

// Scheduled refresh
tokenRefreshService.scheduleAtFixedRate(
    this::refreshToken,
    refreshInterval,
    refreshInterval,
    TimeUnit.SECONDS
);

// Header injection
public Map<String, String> get(String method, String path) {
    if ("/token".equalsIgnoreCase(path)) {
        return Map.of(); // Skip for /token endpoint
    }
    return Map.of("X-Authorization", getToken());
}
```

---

#### 4. TokenResource

**Location**: `orkes-client/src/main/java/io/orkes/conductor/client/http/TokenResource.java`

**Purpose**: Wrapper for `/token` endpoint.

**Implementation**:
```java
public ConductorClientResponse<TokenResponse> generate(GenerateTokenRequest body) {
    ConductorClientRequest request = ConductorClientRequest.builder()
            .method(Method.POST)
            .path("/token")
            .body(body)
            .build();
    return client.execute(request, new TypeReference<>() {});
}
```

---

### Configuration

#### Environment Variables

**Implementation in ApiClientBuilder**:
```java
protected void applyEnvVariables() {
    super.applyEnvVariables(); // Gets CONDUCTOR_SERVER_URL

    String conductorAuthKey = System.getenv("CONDUCTOR_AUTH_KEY");
    if (conductorAuthKey == null) {
        // Backward compatibility
        conductorAuthKey = System.getenv("CONDUCTOR_SERVER_AUTH_KEY");
    }

    String conductorAuthSecret = System.getenv("CONDUCTOR_AUTH_SECRET");
    if (conductorAuthSecret == null) {
        // Backward compatibility
        conductorAuthSecret = System.getenv("CONDUCTOR_SERVER_AUTH_SECRET");
    }

    if (conductorAuthKey != null && conductorAuthSecret != null) {
        this.credentials(conductorAuthKey, conductorAuthSecret);
    }
}
```

#### Token Refresh Interval

**Priority Order Implementation**:
```java
private long getTokenRefreshInSeconds(long tokenRefreshInSeconds) {
    if (tokenRefreshInSeconds == 0) {
        // Check environment variable
        String refreshInterval = System.getenv("CONDUCTOR_SECURITY_TOKEN_REFRESH_INTERVAL");
        if (refreshInterval == null) {
            // Check system property
            refreshInterval = System.getProperty("CONDUCTOR_SECURITY_TOKEN_REFRESH_INTERVAL");
        }
        if (refreshInterval != null) {
            try {
                return Integer.parseInt(refreshInterval);
            } catch (Exception ignored) {}
        }
        return 2700; // Default: 45 minutes
    }
    return tokenRefreshInSeconds;
}
```

---

### Usage Examples

#### Example 1: Basic Setup with Environment Variables

```bash
export CONDUCTOR_SERVER_URL="https://play.orkes.io/api"
export CONDUCTOR_AUTH_KEY="my-key-id"
export CONDUCTOR_AUTH_SECRET="my-secret"
```

```java
public class MyApp {
    public static void main(String[] args) {
        // Zero-config setup
        ApiClient client = new ApiClient();

        WorkflowClient workflowClient = new WorkflowClient(client);
        String workflowId = workflowClient.startWorkflow(
            "my_workflow", 1, "", Map.of());

        System.out.println("Started workflow: " + workflowId);
    }
}
```

### Example 2: Custom Configuration

```java
public class MyApp {
    public static void main(String[] args) {
        ApiClient client = ApiClient.builder()
            .basePath("https://play.orkes.io/api")
            .credentials("my-key-id", "my-secret")
            .connectTimeout(10000)
            .readTimeout(30000)
            .writeTimeout(30000)
            .connectionPoolConfig(new ConnectionPoolConfig(
                50,      // maxIdleConnections
                5,       // keepAliveDuration
                TimeUnit.MINUTES
            ))
            .build();

        WorkflowClient workflowClient = new WorkflowClient(client);
        TaskClient taskClient = new TaskClient(client);

        // Use clients...
    }
}
```

### Example 3: Custom Token Refresh Interval

```java
public class MyApp {
    public static void main(String[] args) {
        // Create custom OrkesAuthentication with 30-minute refresh
        OrkesAuthentication auth = new OrkesAuthentication(
            "my-key-id",
            "my-secret",
            1800  // 30 minutes
        );

        ApiClient client = ApiClient.builder()
            .basePath("https://play.orkes.io/api")
            .addHeaderSupplier(auth)
            .build();

        WorkflowClient workflowClient = new WorkflowClient(client);
    }
}
```

### Example 4: Multiple Clients with Shared Authentication

```java
public class MyApp {
    public static void main(String[] args) {
        // Shared authentication instance
        OrkesAuthentication auth = new OrkesAuthentication(
            "my-key-id",
            "my-secret"
        );

        // Client for US region
        ApiClient usClient = ApiClient.builder()
            .basePath("https://us.orkes.io/api")
            .addHeaderSupplier(auth)
            .build();

        // Client for EU region
        ApiClient euClient = ApiClient.builder()
            .basePath("https://eu.orkes.io/api")
            .addHeaderSupplier(auth)
            .build();

        // Both clients share the same token cache and refresh schedule
    }
}
```

### Example 5: Testing with Mock Authentication

```java
public class TestAuthSupplier implements HeaderSupplier {
    private final String mockToken;

    public TestAuthSupplier(String mockToken) {
        this.mockToken = mockToken;
    }

    @Override
    public void init(ConductorClient client) {
        // No-op for testing
    }

    @Override
    public Map<String, String> get(String method, String path) {
        return Map.of("X-Authorization", mockToken);
    }
}

public class MyTest {
    @Test
    public void testWorkflowExecution() {
        ApiClient client = ApiClient.builder()
            .basePath("http://localhost:8080/api")
            .addHeaderSupplier(new TestAuthSupplier("test-token"))
            .build();

        WorkflowClient workflowClient = new WorkflowClient(client);

        // Test workflow operations...
    }
}
```

### Example 6: Custom Interceptor for Logging

```java
public class LoggingInterceptor implements Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        long startTime = System.nanoTime();

        LOGGER.info("→ {} {} ", request.method(), request.url());

        Response response = chain.proceed(request);

        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        LOGGER.info("← {} {} ({}ms)", response.code(), request.url(), duration);

        return response;
    }
}

public class MyApp {
    public static void main(String[] args) {
        ApiClient client = ApiClient.builder()
            .basePath("https://play.orkes.io/api")
            .credentials("my-key-id", "my-secret")
            .configureOkHttp(builder -> {
                builder.addInterceptor(new LoggingInterceptor());
            })
            .build();

        WorkflowClient workflowClient = new WorkflowClient(client);
    }
}
```

### Example 7: Graceful Shutdown

```java
public class MyApp {
    public static void main(String[] args) {
        ApiClient client = ApiClient.builder()
            .basePath("https://play.orkes.io/api")
            .credentials("my-key-id", "my-secret")
            .build();

        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down ApiClient...");
            client.shutdown();
        }));

        WorkflowClient workflowClient = new WorkflowClient(client);

        // Application logic...
    }
}
```

---

### Implementation Status

**Current Java SDK Status**:

✅ **Fully Implemented**:
- Proactive token refresh with dual mechanism (scheduled + cache-based)
- Clean separation of concerns via HeaderSupplier pattern
- Flexible configuration via environment variables or builder
- Backward compatibility with v2 API
- Production-ready HTTP client with connection pooling
- HTTP/2 support
- Virtual thread support

⚠️ **Areas for Improvement**:
1. **No 401 retry logic** - Expired tokens cause permanent failures
2. **No general retry mechanism** - Transient failures not handled
3. **Limited observability** - Difficult to debug auth issues
4. **No circuit breaker** - Can worsen outages
5. **Interceptor API not exposed** - Limited extensibility

🚀 **Recommended Enhancements** (See [Proposed Improvements](#proposed-improvements) section):
1. Token refresh interceptor for automatic 401 retry
2. Configurable retry policy with exponential backoff
3. Metrics/observability for token refresh and requests
4. Circuit breaker support
5. Token inspection API for debugging

---

## Summary

This design document provides a language-agnostic blueprint for implementing a Conductor API client with authentication support. The key design principles are:

- **Reliability**: Dual token refresh mechanism prevents most authentication failures
- **Flexibility**: Environment-based configuration with builder pattern for customization
- **Extensibility**: Plugin architecture via header suppliers for cross-cutting concerns
- **Security**: Secure credential handling with no logging of sensitive data
- **Performance**: Connection pooling, HTTP/2, and efficient caching

The design works well for stable environments with properly configured token TTLs. For production deployments in varied network conditions, implementing the proposed enhancements (retry logic, circuit breaker, observability) is recommended.

For language-specific implementations, refer to:
- [Requirements](#requirements) section for functional and non-functional requirements
- [Java SDK Implementation Reference](#java-sdk-implementation-reference) for concrete examples
