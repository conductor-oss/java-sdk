package autonomousagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteStepWorkerTest {

    private final ExecuteStepWorker worker = new ExecuteStepWorker();

    @Test
    void taskDefName() {
        assertEquals("aa_execute_step", worker.getTaskDefName());
    }

    @Test
    void iteration1ReturnsMetricsPipeline() {
        Task task = taskWith(Map.of(
                "plan", List.of("step1", "step2", "step3"),
                "iteration", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Metrics pipeline deployed: 15 exporters active, scrape interval 15s, retention 30d",
                result.getOutputData().get("result"));
        assertEquals(1, result.getOutputData().get("stepNumber"));
        assertEquals("complete", result.getOutputData().get("status"));
    }

    @Test
    void iteration2ReturnsDashboard() {
        Task task = taskWith(Map.of(
                "plan", List.of("step1", "step2", "step3"),
                "iteration", 2));
        TaskResult result = worker.execute(task);

        assertEquals("Dashboard created: 12 panels covering CPU, memory, disk, network, and application metrics",
                result.getOutputData().get("result"));
        assertEquals(2, result.getOutputData().get("stepNumber"));
    }

    @Test
    void iteration3ReturnsAlerts() {
        Task task = taskWith(Map.of(
                "plan", List.of("step1", "step2", "step3"),
                "iteration", 3));
        TaskResult result = worker.execute(task);

        assertEquals("Alerts configured: 8 rules for critical thresholds, Slack + PagerDuty notifications enabled",
                result.getOutputData().get("result"));
        assertEquals(3, result.getOutputData().get("stepNumber"));
    }

    @Test
    void outputContainsResultStepNumberAndStatus() {
        Task task = taskWith(Map.of("plan", List.of("s1"), "iteration", 1));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertTrue(result.getOutputData().containsKey("stepNumber"));
        assertTrue(result.getOutputData().containsKey("status"));
        assertEquals("complete", result.getOutputData().get("status"));
    }

    @Test
    void handlesNullPlan() {
        Map<String, Object> input = new HashMap<>();
        input.put("plan", null);
        input.put("iteration", 1);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingIteration() {
        Task task = taskWith(Map.of("plan", List.of("s1")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("stepNumber"));
    }

    @Test
    void handlesStringIteration() {
        Task task = taskWith(Map.of("plan", List.of("s1", "s2", "s3"), "iteration", "2"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("stepNumber"));
        assertEquals("Dashboard created: 12 panels covering CPU, memory, disk, network, and application metrics",
                result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
        assertEquals(1, result.getOutputData().get("stepNumber"));
        assertEquals("complete", result.getOutputData().get("status"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
