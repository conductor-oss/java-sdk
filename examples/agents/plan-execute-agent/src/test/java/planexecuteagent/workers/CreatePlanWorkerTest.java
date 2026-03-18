package planexecuteagent.workers;

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
        assertEquals("pe_create_plan", worker.getTaskDefName());
    }

    @Test
    void createsThreeStepPlan() {
        Task task = taskWith(Map.of("objective", "Develop go-to-market strategy for new SaaS product"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<String> steps = (List<String>) result.getOutputData().get("steps");
        assertNotNull(steps);
        assertEquals(3, steps.size());
    }

    @Test
    void returnsTotalSteps() {
        Task task = taskWith(Map.of("objective", "Develop go-to-market strategy"));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("totalSteps"));
    }

    @Test
    void firstStepIsMarketData() {
        Task task = taskWith(Map.of("objective", "Develop strategy"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> steps = (List<String>) result.getOutputData().get("steps");
        assertEquals("Gather market data and competitor analysis", steps.get(0));
    }

    @Test
    void secondStepIsTrendAnalysis() {
        Task task = taskWith(Map.of("objective", "Develop strategy"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> steps = (List<String>) result.getOutputData().get("steps");
        assertEquals("Analyze trends and identify opportunities", steps.get(1));
    }

    @Test
    void thirdStepIsRecommendations() {
        Task task = taskWith(Map.of("objective", "Develop strategy"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> steps = (List<String>) result.getOutputData().get("steps");
        assertEquals("Generate strategic recommendations", steps.get(2));
    }

    @Test
    void handlesEmptyObjective() {
        Map<String, Object> input = new HashMap<>();
        input.put("objective", "");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("steps"));
    }

    @Test
    void handlesMissingObjective() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("steps"));
        assertEquals(3, result.getOutputData().get("totalSteps"));
    }

    @Test
    void handlesNullObjective() {
        Map<String, Object> input = new HashMap<>();
        input.put("objective", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("steps"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
