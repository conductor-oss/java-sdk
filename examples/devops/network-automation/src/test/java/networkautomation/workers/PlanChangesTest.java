package networkautomation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanChangesTest {

    private final PlanChanges worker = new PlanChanges();

    @Test
    void taskDefName() {
        assertEquals("na_plan_changes", worker.getTaskDefName());
    }

    @Test
    void plansFirewallUpdate() {
        Task task = taskWith(Map.of("changeType", "firewall-update"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("firewallRules"));
        assertEquals(2, result.getOutputData().get("vlanChanges"));
        assertEquals(true, result.getOutputData().get("planned"));
    }

    @Test
    void plansVlanMigration() {
        Task task = taskWith(Map.of("changeType", "vlan-migration"));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("firewallRules"));
        assertEquals(5, result.getOutputData().get("vlanChanges"));
    }

    @Test
    void plansGeneralChange() {
        Task task = taskWith(Map.of("changeType", "general"));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("firewallRules"));
        assertEquals(1, result.getOutputData().get("vlanChanges"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("plan_changesData", null);
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void processedIsTrue() {
        Task task = taskWith(Map.of("changeType", "firewall-update"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsAllFields() {
        Task task = taskWith(Map.of("changeType", "firewall-update"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("firewallRules"));
        assertNotNull(result.getOutputData().get("vlanChanges"));
        assertNotNull(result.getOutputData().get("planned"));
        assertNotNull(result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> dataMap) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("plan_changesData", dataMap);
        task.setInputData(input);
        return task;
    }
}
