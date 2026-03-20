package treeofthought.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefineProblemWorkerTest {

    private final DefineProblemWorker worker = new DefineProblemWorker();

    @Test
    void taskDefName() {
        assertEquals("tt_define_problem", worker.getTaskDefName());
    }

    @Test
    void passesTheProblemThrough() {
        Task task = taskWith(Map.of("problem", "Design a highly available system"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Design a highly available system", result.getOutputData().get("problem"));
    }

    @Test
    void handlesLongProblemStatement() {
        String longProblem = "Design a system that ".repeat(50);
        Task task = taskWith(Map.of("problem", longProblem));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(longProblem, result.getOutputData().get("problem"));
    }

    @Test
    void handlesEmptyProblem() {
        Map<String, Object> input = new HashMap<>();
        input.put("problem", "");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("No problem defined", result.getOutputData().get("problem"));
    }

    @Test
    void handlesNullProblem() {
        Map<String, Object> input = new HashMap<>();
        input.put("problem", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("No problem defined", result.getOutputData().get("problem"));
    }

    @Test
    void handlesMissingProblemKey() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("No problem defined", result.getOutputData().get("problem"));
    }

    @Test
    void outputContainsProblemKey() {
        Task task = taskWith(Map.of("problem", "Scale to 10x traffic"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("problem"));
    }

    @Test
    void handlesProblemWithSpecialCharacters() {
        String problem = "Handle 10x spikes & ensure <99.99%> uptime \"guaranteed\"";
        Task task = taskWith(Map.of("problem", problem));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(problem, result.getOutputData().get("problem"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
