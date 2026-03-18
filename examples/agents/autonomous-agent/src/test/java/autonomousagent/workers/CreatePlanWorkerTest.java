package autonomousagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreatePlanWorkerTest {

    private final CreatePlanWorker worker = new CreatePlanWorker();

    @Test
    void taskDefName() {
        assertEquals("aa_create_plan", worker.getTaskDefName());
    }

    @Test
    void returnsPlanForGoal() {
        Task task = taskWith(Map.of(
                "goal", "Build and deploy a monitoring dashboard with alerting capabilities",
                "constraints", List.of("Must complete in 3 steps")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<String> plan = (List<String>) result.getOutputData().get("plan");
        assertNotNull(plan);
        assertEquals(3, plan.size());
    }

    @Test
    void planContainsExpectedSteps() {
        Task task = taskWith(Map.of(
                "goal", "Build monitoring dashboard",
                "constraints", List.of("Must complete in 3 steps")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> plan = (List<String>) result.getOutputData().get("plan");
        assertEquals("Set up metrics collection pipeline (Prometheus + exporters)", plan.get(0));
        assertEquals("Build Grafana dashboard with key panels and thresholds", plan.get(1));
        assertEquals("Configure alert rules and notification channels", plan.get(2));
    }

    @Test
    void returnsEstimatedSteps() {
        Task task = taskWith(Map.of(
                "goal", "Build monitoring dashboard",
                "constraints", List.of("Must complete in 3 steps")));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("estimatedSteps"));
    }

    @Test
    void outputContainsPlanAndEstimatedSteps() {
        Task task = taskWith(Map.of("goal", "Any goal", "constraints", List.of()));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("plan"));
        assertTrue(result.getOutputData().containsKey("estimatedSteps"));
    }

    @Test
    void handlesNullGoal() {
        Map<String, Object> input = new HashMap<>();
        input.put("goal", null);
        input.put("constraints", List.of("c1"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("plan"));
    }

    @Test
    void handlesNullConstraints() {
        Map<String, Object> input = new HashMap<>();
        input.put("goal", "Some goal");
        input.put("constraints", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("plan"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("plan"));
        assertEquals(3, result.getOutputData().get("estimatedSteps"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
