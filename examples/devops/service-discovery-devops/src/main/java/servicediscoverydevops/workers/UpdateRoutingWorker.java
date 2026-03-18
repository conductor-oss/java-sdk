package servicediscoverydevops.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Updates the routing table by performing real connectivity checks and writing
 * the routing configuration to a JSON file on disk. Adds a service
 * instance to a load balancer by:
 *   1. Verifying the endpoint is reachable (DNS + port check)
 *   2. Writing the routing entry to /tmp/service-routing/
 *   3. Counting active instances by reading existing routing files
 *
 * Input:
 *   - update_routingData (Map): from HealthCheckWorker containing
 *     serviceName, endpoint, healthy, registrationId, etc.
 *
 * Output:
 *   - updated (boolean): whether routing was updated
 *   - routingStrategy (String): load balancing strategy
 *   - activeInstances (int): number of active instances
 *   - loadBalancer (String): load balancer type
 *   - serviceName, endpoint (passthrough)
 *   - routingFile (String): path to the routing config file
 */
public class UpdateRoutingWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sd_update_routing";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("update_routingData");
        String serviceName = data != null ? (String) data.getOrDefault("serviceName", "unknown") : "unknown";
        String endpoint = data != null ? (String) data.getOrDefault("endpoint", "unknown") : "unknown";
        boolean healthy = data != null && Boolean.TRUE.equals(data.get("healthy"));
        String registrationId = data != null ? (String) data.getOrDefault("registrationId", "unknown") : "unknown";

        System.out.println("[sd_update_routing] Updating routing for " + serviceName + " at " + endpoint);

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("serviceName", serviceName);
        output.put("endpoint", endpoint);

        try {
            // Write routing entry to a real file
            Path routingDir = Path.of(System.getProperty("java.io.tmpdir"), "service-routing", serviceName);
            Files.createDirectories(routingDir);

            String routingFileName = registrationId + ".json";
            Path routingFile = routingDir.resolve(routingFileName);

            // Perform a real DNS check to get the routable IP
            String hostname = RegisterWorker.extractHostname(endpoint);
            String routeAddress = hostname;
            try {
                InetAddress addr = InetAddress.getByName(hostname);
                routeAddress = addr.getHostAddress();
            } catch (Exception e) {
                // Use hostname if DNS fails
            }

            // Verify port reachability
            int port = HealthCheckWorker.extractPort(endpoint);
            boolean portOpen = false;
            if (port > 0) {
                try (Socket socket = new Socket()) {
                    socket.connect(new java.net.InetSocketAddress(hostname, port), 3000);
                    portOpen = true;
                } catch (Exception e) {
                    // Port not reachable
                }
            }

            String json = "{\n"
                    + "  \"registrationId\": \"" + registrationId + "\",\n"
                    + "  \"serviceName\": \"" + serviceName + "\",\n"
                    + "  \"endpoint\": \"" + endpoint + "\",\n"
                    + "  \"routeAddress\": \"" + routeAddress + "\",\n"
                    + "  \"healthy\": " + healthy + ",\n"
                    + "  \"portReachable\": " + portOpen + ",\n"
                    + "  \"weight\": 100,\n"
                    + "  \"updatedAt\": \"" + Instant.now() + "\"\n"
                    + "}\n";

            Files.writeString(routingFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Count active instances by counting routing files for this service
            long activeInstances;
            try (var stream = Files.list(routingDir)) {
                activeInstances = stream.filter(p -> p.toString().endsWith(".json")).count();
            }

            System.out.println("  Route address: " + routeAddress);
            System.out.println("  Port reachable: " + portOpen);
            System.out.println("  Active instances: " + activeInstances);
            System.out.println("  Routing file: " + routingFile);

            output.put("updated", true);
            output.put("routingStrategy", "round-robin");
            output.put("activeInstances", (int) activeInstances);
            output.put("loadBalancer", "envoy-proxy");
            output.put("routeAddress", routeAddress);
            output.put("portReachable", portOpen);
            output.put("routingFile", routingFile.toString());

        } catch (Exception e) {
            System.out.println("  Routing update error: " + e.getMessage());
            output.put("updated", false);
            output.put("routingStrategy", "round-robin");
            output.put("activeInstances", 1);
            output.put("loadBalancer", "envoy-proxy");
            output.put("error", e.getMessage());
        }

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
