package autonomousagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SetGoalWorkerTest {

    private final SetGoalWorker worker = new SetGoalWorker();

    @Test
    void taskDefName() {
        assertEquals("aa_set_goal", worker.getTaskDefName());
    }

    @Test
    void returnsGoalForMission() {
        Task task = taskWith(Map.of("mission", "Set up production monitoring for the platform"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Build and deploy a monitoring dashboard with alerting capabilities",
                result.getOutputData().get("goal"));
    }

    @Test
    void returnsConstraints() {
        Task task = taskWith(Map.of("mission", "Set up production monitoring for the platform"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> constraints = (List<String>) result.getOutputData().get("constraints");
        assertNotNull(constraints);
        assertEquals(3, constraints.size());
        assertEquals("Must complete in 3 steps", constraints.get(0));
        assertEquals("Use existing infrastructure", constraints.get(1));
        assertEquals("Zero-downtime deployment", constraints.get(2));
    }

    @Test
    void outputContainsGoalAndConstraints() {
        Task task = taskWith(Map.of("mission", "Any mission"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("goal"));
        assertTrue(result.getOutputData().containsKey("constraints"));
    }

    @Test
    void handlesEmptyMission() {
        Map<String, Object> input = new HashMap<>();
        input.put("mission", "");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("goal"));
    }

    @Test
    void handlesNullMission() {
        Map<String, Object> input = new HashMap<>();
        input.put("mission", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("goal"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("goal"));
        assertNotNull(result.getOutputData().get("constraints"));
    }

    @Test
    void goalIsNonEmpty() {
        Task task = taskWith(Map.of("mission", "Test"));
        TaskResult result = worker.execute(task);

        String goal = (String) result.getOutputData().get("goal");
        assertFalse(goal.isEmpty());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
