package basicrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateAnswerWorkerTest {

    @Test
    void taskDefName() {
        GenerateAnswerWorker worker = new GenerateAnswerWorker();
        assertEquals("brag_generate_answer", worker.getTaskDefName());
    }

    @Test
    void rejectsNullQuestion() {
        GenerateAnswerWorker worker = new GenerateAnswerWorker();
        Map<String, Object> input = new HashMap<>();
        input.put("question", null);
        input.put("context", List.of(Map.of("text", "some doc")));
        Task task = taskWith(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("question"));
    }

    @Test
    void rejectsBlankQuestion() {
        GenerateAnswerWorker worker = new GenerateAnswerWorker();
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "   ",
                "context", List.of(Map.of("text", "some doc"))
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("question"));
    }

    @Test
    void rejectsNullContext() {
        GenerateAnswerWorker worker = new GenerateAnswerWorker();
        Map<String, Object> input = new HashMap<>();
        input.put("question", "What is Conductor?");
        input.put("context", null);
        Task task = taskWith(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("context"));
    }

    @Test
    void rejectsEmptyContext() {
        GenerateAnswerWorker worker = new GenerateAnswerWorker();
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is Conductor?",
                "context", List.of()
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("context"));
    }

    @Test
    void rejectsMissingContext() {
        GenerateAnswerWorker worker = new GenerateAnswerWorker();
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is Conductor?"
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("context"));
    }

    @Test
    void executeThrowsWithoutApiKey() {
        String key = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            GenerateAnswerWorker worker = new GenerateAnswerWorker();
            Task task = taskWith(new HashMap<>(Map.of(
                    "question", "What is Conductor?",
                    "context", List.of(Map.of("text", "Conductor is an orchestration platform."))
            )));

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
