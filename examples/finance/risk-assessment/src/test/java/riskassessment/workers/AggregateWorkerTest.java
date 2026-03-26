package riskassessment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AggregateWorkerTest {

    private final AggregateWorker worker = new AggregateWorker();

    @Test
    void taskDefName() {
        assertEquals("rsk_aggregate", worker.getTaskDefName());
    }

    @Test
    void computesLowRisk() {
        Task task = taskWith(Map.of("marketRisk", 10, "creditRisk", 10, "operationalRisk", 10));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Low", result.getOutputData().get("riskCategory"));
        assertEquals(true, result.getOutputData().get("capitalAdequate"));
    }

    @Test
    void computesHighRisk() {
        Task task = taskWith(Map.of("marketRisk", 80, "creditRisk", 70, "operationalRisk", 60));
        TaskResult result = worker.execute(task);

        assertEquals("High", result.getOutputData().get("riskCategory"));
        assertEquals(false, result.getOutputData().get("capitalAdequate"));
    }

    @Test
    void computesModerateRisk() {
        Task task = taskWith(Map.of("marketRisk", 40, "creditRisk", 30, "operationalRisk", 35));
        TaskResult result = worker.execute(task);

        assertEquals("Moderate", result.getOutputData().get("riskCategory"));
    }

    @Test
    void handlesStringInputs() {
        Task task = taskWith(Map.of("marketRisk", "20", "creditRisk", "15", "operationalRisk", "10"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("overallRisk"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
