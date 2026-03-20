package nestedswitch.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NsEuStandardWorkerTest {

    private final NsEuStandardWorker worker = new NsEuStandardWorker();

    @Test
    void taskDefName() {
        assertEquals("ns_eu_standard", worker.getTaskDefName());
    }

    @Test
    void handlesEuStandardRequest() {
        Task task = taskWith(Map.of("region", "EU", "tier", "standard", "amount", 200));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ns_eu_standard", result.getOutputData().get("handler"));
        assertEquals(true, result.getOutputData().get("done"));
    }

    @Test
    void outputIsAlwaysDeterministic() {
        Task task1 = taskWith(Map.of("region", "EU", "tier", "standard", "amount", 10));
        Task task2 = taskWith(Map.of("region", "EU", "tier", "standard", "amount", 20));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("handler"), result2.getOutputData().get("handler"));
        assertEquals(result1.getOutputData().get("done"), result2.getOutputData().get("done"));
    }

    @Test
    void statusIsAlwaysCompleted() {
        Task task = taskWith(Map.of("region", "EU", "tier", "standard", "amount", 50));
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
