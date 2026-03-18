package multiagentsupport.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SolutionProposeWorkerTest {

    private final SolutionProposeWorker worker = new SolutionProposeWorker();

    @Test
    void taskDefName() {
        assertEquals("cs_solution_propose", worker.getTaskDefName());
    }

    @Test
    void returnsResponseWithFixSteps() {
        Task task = taskWith(Map.of(
                "kbArticles", List.of(Map.of("id", "KB-1001", "title", "Test")),
                "description", "App crashes on login",
                "severity", "high"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String response = (String) result.getOutputData().get("response");
        assertNotNull(response);
        assertTrue(response.contains("fix steps"));
        assertTrue(response.contains("restart"));
    }

    @Test
    void returnsSolutionTypeKnownFix() {
        Task task = taskWith(Map.of(
                "kbArticles", List.of(),
                "description", "Error on page",
                "severity", "medium"));
        TaskResult result = worker.execute(task);

        assertEquals("known_fix", result.getOutputData().get("solutionType"));
    }

    @Test
    void returnsReferencedArticles() {
        Task task = taskWith(Map.of(
                "kbArticles", List.of(),
                "description", "Some issue",
                "severity", "high"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> refs = (List<String>) result.getOutputData().get("referencedArticles");
        assertNotNull(refs);
        assertEquals(3, refs.size());
        assertTrue(refs.contains("KB-1001"));
        assertTrue(refs.contains("KB-1002"));
        assertTrue(refs.contains("KB-1003"));
    }

    @Test
    void handlesNullDescription() {
        Map<String, Object> input = new HashMap<>();
        input.put("description", null);
        input.put("severity", null);
        input.put("kbArticles", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("response"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("response"));
        assertNotNull(result.getOutputData().get("referencedArticles"));
        assertNotNull(result.getOutputData().get("solutionType"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of("description", "Test", "severity", "low"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("response"));
        assertTrue(result.getOutputData().containsKey("referencedArticles"));
        assertTrue(result.getOutputData().containsKey("solutionType"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
