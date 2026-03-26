package networkautomation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuditNetworkTest {

    private final AuditNetwork worker = new AuditNetwork();

    @Test
    void taskDefName() {
        assertEquals("na_audit", worker.getTaskDefName());
    }

    @Test
    void auditsProdDatacenter() {
        Task task = taskWith("prod-datacenter", "firewall-update");
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("AUDIT-1353", result.getOutputData().get("auditId"));
        assertEquals("prod-datacenter", result.getOutputData().get("network"));
        assertEquals(8, result.getOutputData().get("switches"));
        assertEquals(4, result.getOutputData().get("routers"));
        assertEquals(12, result.getOutputData().get("totalDevices"));
    }

    @Test
    void auditsStagingDatacenter() {
        Task task = taskWith("staging-datacenter", "firewall-update");
        TaskResult result = worker.execute(task);

        assertEquals(4, result.getOutputData().get("switches"));
        assertEquals(2, result.getOutputData().get("routers"));
        assertEquals(6, result.getOutputData().get("totalDevices"));
    }

    @Test
    void auditsUnknownNetwork() {
        Task task = taskWith("dev-lab", "general");
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("switches"));
        assertEquals(1, result.getOutputData().get("routers"));
    }

    @Test
    void handlesNullNetwork() {
        Map<String, Object> input = new HashMap<>();
        input.put("network", null);
        input.put("changeType", "general");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("default-network", result.getOutputData().get("network"));
    }

    @Test
    void handlesNullChangeType() {
        Map<String, Object> input = new HashMap<>();
        input.put("network", "prod-datacenter");
        input.put("changeType", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("general", result.getOutputData().get("changeType"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void totalDevicesIsSumOfSwitchesAndRouters() {
        Task task = taskWith("prod-datacenter", "firewall-update");
        TaskResult result = worker.execute(task);

        int switches = (int) result.getOutputData().get("switches");
        int routers = (int) result.getOutputData().get("routers");
        int total = (int) result.getOutputData().get("totalDevices");
        assertEquals(switches + routers, total);
    }

    private Task taskWith(String network, String changeType) {
        Map<String, Object> input = new HashMap<>();
        input.put("network", network);
        input.put("changeType", changeType);
        return taskWith(input);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
