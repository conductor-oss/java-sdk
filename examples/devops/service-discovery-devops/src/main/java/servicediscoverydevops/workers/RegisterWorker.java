package servicediscoverydevops.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.InetAddress;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers a new service instance by performing real DNS lookups on the
 * endpoint hostname using java.net.InetAddress. Resolves the endpoint's
 * hostname to IP addresses and records them as part of the registration.
 *
 * Input:
 *   - serviceName (String): name of the service
 *   - endpoint (String): service URL (e.g., http://pay-svc:8080)
 *   - version (String): service version
 *
 * Output:
 *   - registrationId (String): deterministic ID derived from inputs
 *   - serviceName, endpoint, version (passthrough)
 *   - status (String): "registered"
 *   - resolvedAddresses (List): IP addresses from DNS resolution
 *   - hostname (String): extracted hostname
 */
public class RegisterWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sd_register";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().getOrDefault("serviceName", "unknown-service");
        String endpoint = (String) task.getInputData().getOrDefault("endpoint", "http://localhost:8080");
        String version = (String) task.getInputData().getOrDefault("version", "0.0.0");

        // Deterministic registration ID derived from inputs
        String registrationId = "reg-" + serviceName + "-" + version;

        System.out.println("[sd_register] Registering service: " + serviceName + " v" + version + " at " + endpoint);

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("registrationId", registrationId);
        output.put("serviceName", serviceName);
        output.put("endpoint", endpoint);
        output.put("version", version);

        // Perform real DNS lookup on the endpoint hostname
        String hostname = extractHostname(endpoint);
        output.put("hostname", hostname);

        try {
            InetAddress[] addresses = InetAddress.getAllByName(hostname);
            List<String> resolvedAddresses = new ArrayList<>();
            for (InetAddress addr : addresses) {
                resolvedAddresses.add(addr.getHostAddress());
            }
            output.put("resolvedAddresses", resolvedAddresses);
            output.put("dnsResolved", true);
            output.put("status", "registered");
            System.out.println("  DNS resolved " + hostname + " to " + resolvedAddresses);
        } catch (Exception e) {
            output.put("resolvedAddresses", List.of());
            output.put("dnsResolved", false);
            output.put("dnsError", e.getMessage());
            output.put("status", "registered");
            System.out.println("  DNS resolution failed for " + hostname + ": " + e.getMessage());
        }

        output.put("registeredAt", Instant.now().toString());
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }

    /**
     * Extracts the hostname from a URL string.
     */
    static String extractHostname(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            String host = uri.getHost();
            return host != null ? host : endpoint;
        } catch (Exception e) {
            // Fallback: try to extract hostname manually
            String s = endpoint;
            if (s.contains("://")) s = s.substring(s.indexOf("://") + 3);
            if (s.contains(":")) s = s.substring(0, s.indexOf(":"));
            if (s.contains("/")) s = s.substring(0, s.indexOf("/"));
            return s;
        }
    }
}
