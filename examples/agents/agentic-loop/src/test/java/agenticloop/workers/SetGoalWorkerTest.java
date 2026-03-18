package agenticloop.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SetGoalWorkerTest {

    private final SetGoalWorker worker = new SetGoalWorker();

    @Test
    void taskDefName() {
        assertEquals("al_set_goal", worker.getTaskDefName());
    }

    @Test
    void setsGoalWithActiveStatus() {
        Task task = taskWith(Map.of("goal", "Research best practices for distributed systems"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Research best practices for distributed systems", result.getOutputData().get("goal"));
        assertEquals("active", result.getOutputData().get("status"));
    }

    @Test
    void outputContainsGoalAndStatus() {
        Task task = taskWith(Map.of("goal", "Build a microservice"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("goal"));
        assertTrue(result.getOutputData().containsKey("status"));
        assertEquals(2, result.getOutputData().size());
    }

    @Test
    void handlesNullGoal() {
        Map<String, Object> input = new HashMap<>();
        input.put("goal", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("No goal specified", result.getOutputData().get("goal"));
        assertEquals("active", result.getOutputData().get("status"));
    }

    @Test
    void handlesMissingGoal() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("No goal specified", result.getOutputData().get("goal"));
        assertEquals("active", result.getOutputData().get("status"));
    }

    @Test
    void handlesBlankGoal() {
        Task task = taskWith(Map.of("goal", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("No goal specified", result.getOutputData().get("goal"));
    }

    @Test
    void handlesEmptyGoal() {
        Task task = taskWith(Map.of("goal", ""));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("No goal specified", result.getOutputData().get("goal"));
    }

    @Test
    void preservesExactGoalText() {
        String goal = "Optimize database query performance for high-traffic endpoints";
        Task task = taskWith(Map.of("goal", goal));
        TaskResult result = worker.execute(task);

        assertEquals(goal, result.getOutputData().get("goal"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
