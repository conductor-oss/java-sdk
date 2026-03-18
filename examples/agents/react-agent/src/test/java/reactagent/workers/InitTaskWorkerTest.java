package reactagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InitTaskWorkerTest {

    private final InitTaskWorker worker = new InitTaskWorker();

    @Test
    void taskDefName() {
        assertEquals("rx_init_task", worker.getTaskDefName());
    }

    @Test
    void initializesWithQuestion() {
        Task task = taskWith(Map.of("question", "What is the current world population?"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("What is the current world population?", result.getOutputData().get("question"));
    }

    @Test
    void returnsEmptyContext() {
        Task task = taskWith(Map.of("question", "Test question"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> context = (List<String>) result.getOutputData().get("context");
        assertNotNull(context);
        assertTrue(context.isEmpty());
    }

    @Test
    void handlesEmptyQuestion() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", "");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("question"));
    }

    @Test
    void handlesNullQuestion() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("question"));
    }

    @Test
    void handlesMissingQuestion() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("question"));
        assertNotNull(result.getOutputData().get("context"));
    }

    @Test
    void preservesQuestionExactly() {
        String complexQuestion = "How many people live on Earth as of 2024?";
        Task task = taskWith(Map.of("question", complexQuestion));
        TaskResult result = worker.execute(task);

        assertEquals(complexQuestion, result.getOutputData().get("question"));
    }

    @Test
    void outputContainsBothFields() {
        Task task = taskWith(Map.of("question", "Test"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("question"));
        assertTrue(result.getOutputData().containsKey("context"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
