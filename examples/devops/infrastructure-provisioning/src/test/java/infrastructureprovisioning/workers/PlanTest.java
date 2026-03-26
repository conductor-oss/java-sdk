package infrastructureprovisioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PlanTest {

    private final Plan worker = new Plan();

    @Test
    void taskDefName() {
        assertEquals("ip_plan", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith("production", "us-east-1", "kubernetes-cluster");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsPlan() {
        Task task = taskWith("production", "us-east-1", "kubernetes-cluster");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("plan"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void planContainsType() {
        Task task = taskWith("production", "us-east-1", "kubernetes-cluster");
        TaskResult result = worker.execute(task);
        Map<String, Object> plan = (Map<String, Object>) result.getOutputData().get("plan");
        assertEquals("kubernetes-cluster", plan.get("type"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void planContainsRegion() {
        Task task = taskWith("production", "us-east-1", "kubernetes-cluster");
        TaskResult result = worker.execute(task);
        Map<String, Object> plan = (Map<String, Object>) result.getOutputData().get("plan");
        assertEquals("us-east-1", plan.get("region"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void planContainsSize() {
        Task task = taskWith("production", "us-east-1", "kubernetes-cluster");
        TaskResult result = worker.execute(task);
        Map<String, Object> plan = (Map<String, Object>) result.getOutputData().get("plan");
        assertEquals("medium", plan.get("size"));
    }

    @Test
    void deterministicOutput() {
        Task task = taskWith("production", "us-east-1", "kubernetes-cluster");
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);
        assertEquals(r1.getOutputData(), r2.getOutputData());
    }

    private Task taskWith(String environment, String region, String resourceType) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("environment", environment);
        input.put("region", region);
        input.put("resourceType", resourceType);
        task.setInputData(input);
        return task;
    }
}
