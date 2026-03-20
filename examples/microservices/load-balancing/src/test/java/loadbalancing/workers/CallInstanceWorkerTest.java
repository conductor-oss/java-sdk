package loadbalancing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CallInstanceWorkerTest {

    private final CallInstanceWorker worker = new CallInstanceWorker();

    @Test
    void taskDefName() {
        assertEquals("lb_call_instance", worker.getTaskDefName());
    }

    @Test
    void processesPartition() {
        Task task = taskWith(Map.of("instanceId", "inst-1", "host", "10.0.1.10:8080", "partition", "batch-part-1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals("inst-1", r.get("instanceId"));
        assertEquals("10.0.1.10:8080", r.get("host"));
        assertEquals("batch-part-1", r.get("partition"));
    }

    @Test
    void returnsRecordsProcessed() {
        Task task = taskWith(Map.of("instanceId", "inst-2", "host", "10.0.1.11:8080", "partition", "batch-part-2"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals(50, r.get("recordsProcessed"));
    }

    @Test
    void returnsLatency() {
        Task task = taskWith(Map.of("instanceId", "inst-3", "host", "10.0.1.12:8080", "partition", "batch-part-3"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals(45, r.get("latencyMs"));
    }

    @Test
    void handlesNullInstanceId() {
        Map<String, Object> input = new HashMap<>();
        input.put("instanceId", null);
        input.put("host", "10.0.1.10:8080");
        input.put("partition", "batch-part-1");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void resultContainsPartitionInfo() {
        Task task = taskWith(Map.of("instanceId", "inst-1", "host", "host:8080", "partition", "part-x"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals("part-x", r.get("partition"));
    }

    @Test
    void outputContainsResultKey() {
        Task task = taskWith(Map.of("instanceId", "inst-1", "host", "host:8080", "partition", "part-1"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
