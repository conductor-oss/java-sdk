package loadbalancerconfig.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DiscoverBackendsWorkerTest {

    private final DiscoverBackendsWorker worker = new DiscoverBackendsWorker();

    @Test
    void taskDefName() {
        assertEquals("lb_discover_backends", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("loadBalancer", "prod-alb-01"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsDiscoverId() {
        Task task = taskWith(Map.of("loadBalancer", "prod-alb-01"));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("discover_backendsId"));
    }

    @Test
    void returnsSuccess() {
        Task task = taskWith(Map.of("loadBalancer", "prod-alb-01"));
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void handlesDefaultLoadBalancer() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
