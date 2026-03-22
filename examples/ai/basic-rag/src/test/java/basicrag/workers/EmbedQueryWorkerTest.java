package basicrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmbedQueryWorkerTest {

    @Test
    void taskDefName() {
        EmbedQueryWorker worker = new EmbedQueryWorker();
        assertEquals("brag_embed_query", worker.getTaskDefName());
    }

    @Test
    void rejectsNullQuestion() {
        EmbedQueryWorker worker = new EmbedQueryWorker();
        Map<String, Object> input = new HashMap<>();
        input.put("question", null);
        Task task = taskWith(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("question"));
    }

    @Test
    void rejectsBlankQuestion() {
        EmbedQueryWorker worker = new EmbedQueryWorker();
        Task task = taskWith(new HashMap<>(Map.of("question", "   ")));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("question"));
    }

    @Test
    void rejectsMissingQuestion() {
        EmbedQueryWorker worker = new EmbedQueryWorker();
        Task task = taskWith(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("question"));
    }

    @Test
    void executeThrowsWithoutApiKey() {
        String key = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            EmbedQueryWorker worker = new EmbedQueryWorker();
            Task task = taskWith(new HashMap<>(Map.of("question", "What is Conductor?")));

            assertThrows(IllegalStateException.class, () -> worker.execute(task));
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
