package servicediscoverydevops.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Performs real health checks on the registered service endpoint:
 *   1. DNS resolution via InetAddress to verify the hostname resolves
 *   2. TCP port reachability via Socket to verify the port is open
 *   3. HTTP health check via HttpURLConnection if the endpoint is HTTP/HTTPS
 *
 * Input:
 *   - health_checkData (Map): output from RegisterWorker containing:
 *     - endpoint, serviceName, version, registrationId
 *
 * Output:
 *   - healthy (boolean): overall health status
 *   - responseTimeMs (long): HTTP response time
 *   - dnsResolved (boolean): whether hostname resolved
 *   - portReachable (boolean): whether port is reachable
 *   - httpStatus (int): HTTP status code (if applicable)
 *   - endpoint, serviceName, version, registrationId (passthrough)
 *   - lastChecked (String): ISO-8601 timestamp
 */
public class HealthCheckWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sd_health_check";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("health_checkData");
        String endpoint = data != null ? (String) data.getOrDefault("endpoint", "unknown") : "unknown";
        String serviceName = data != null ? (String) data.getOrDefault("serviceName", "unknown") : "unknown";
        String version = data != null ? (String) data.getOrDefault("version", "unknown") : "unknown";
        String registrationId = data != null ? (String) data.getOrDefault("registrationId", "unknown") : "unknown";

        System.out.println("[sd_health_check] Health check on " + endpoint + " for " + serviceName);

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("endpoint", endpoint);
        output.put("serviceName", serviceName);
        output.put("version", version);
        output.put("registrationId", registrationId);

        boolean dnsResolved = false;
        boolean portReachable = false;
        boolean healthy = false;
        long responseTimeMs = 0;

        String hostname = RegisterWorker.extractHostname(endpoint);
        int port = extractPort(endpoint);

        // Check 1: DNS resolution
        try {
            InetAddress addr = InetAddress.getByName(hostname);
            dnsResolved = true;
            output.put("resolvedAddress", addr.getHostAddress());
            System.out.println("  DNS: " + hostname + " -> " + addr.getHostAddress());
        } catch (Exception e) {
            System.out.println("  DNS: FAILED for " + hostname + " - " + e.getMessage());
            output.put("dnsError", e.getMessage());
        }

        // Check 2: TCP port reachability
        if (dnsResolved && port > 0) {
            try (Socket socket = new Socket()) {
                long portStart = System.currentTimeMillis();
                socket.connect(new java.net.InetSocketAddress(hostname, port), 5000);
                portReachable = true;
                long portTime = System.currentTimeMillis() - portStart;
                output.put("portCheckMs", portTime);
                System.out.println("  Port " + port + ": OPEN (" + portTime + "ms)");
            } catch (Exception e) {
                System.out.println("  Port " + port + ": CLOSED/UNREACHABLE - " + e.getMessage());
                output.put("portError", e.getMessage());
            }
        }

        // Check 3: HTTP health check
        if (dnsResolved && (endpoint.startsWith("http://") || endpoint.startsWith("https://"))) {
            try {
                // Try /health or /healthz endpoint first, fall back to root
                String healthUrl = endpoint.endsWith("/") ? endpoint + "health" : endpoint + "/health";
                long httpStart = System.currentTimeMillis();

                HttpURLConnection conn = (HttpURLConnection) URI.create(healthUrl).toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "ServiceDiscovery-HealthCheck/1.0");

                int statusCode;
                try {
                    statusCode = conn.getResponseCode();
                } catch (Exception e) {
                    // Try root URL if /health fails
                    conn.disconnect();
                    conn = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    statusCode = conn.getResponseCode();
                }

                responseTimeMs = System.currentTimeMillis() - httpStart;
                conn.disconnect();

                output.put("httpStatus", statusCode);
                healthy = statusCode >= 200 && statusCode < 400;
                System.out.println("  HTTP: " + statusCode + " (" + responseTimeMs + "ms)");
            } catch (Exception e) {
                System.out.println("  HTTP: FAILED - " + e.getMessage());
                output.put("httpError", e.getMessage());
                // If DNS resolved and port is reachable but HTTP failed,
                // consider it partially healthy
                healthy = portReachable;
            }
        } else {
            // Non-HTTP endpoint - healthy if DNS resolves and port is reachable
            healthy = dnsResolved && (port <= 0 || portReachable);
        }

        output.put("healthy", healthy);
        output.put("dnsResolved", dnsResolved);
        output.put("portReachable", portReachable);
        output.put("responseTimeMs", responseTimeMs);
        output.put("lastChecked", Instant.now().toString());

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }

    /**
     * Extracts port number from an endpoint URL.
     */
    static int extractPort(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            int port = uri.getPort();
            if (port > 0) return port;
            // Default ports
            if ("https".equals(uri.getScheme())) return 443;
            if ("http".equals(uri.getScheme())) return 80;
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }
}
