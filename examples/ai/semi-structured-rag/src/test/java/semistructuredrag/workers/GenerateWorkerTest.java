package semistructuredrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateWorkerTest {

    private final GenerateWorker worker = new GenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("ss_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerWithContext() {
        String context = "[TABLE] revenue = $4.2M (from financials_db)\n"
                + "[TEXT] Q3 earnings exceeded expectations (chunk chunk-001, score 0.91)";

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What were the Q3 revenue figures?",
                "context", context
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("Q3 revenue figures"));
        assertTrue(answer.contains("2 sources"));
    }

    @Test
    void answerIncludesSourceCount() {
        String context = "[TABLE] revenue = $4.2M (from financials_db)\n"
                + "[TABLE] count = 342 (from hr_db)\n"
                + "[TEXT] Growth was strong (chunk chunk-001, score 0.91)";

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test question",
                "context", context
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("3 sources"));
    }

    @Test
    void handlesEmptyContext() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test question",
                "context", ""
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("0 sources"));
    }

    @Test
    void handlesNullContext() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test question"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("0 sources"));
    }

    @Test
    void handlesNullQuestion() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void answerMentionsStructuredAndUnstructured() {
        String context = "[TABLE] revenue = $4.2M (from financials_db)";

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is revenue?",
                "context", context
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("structured and unstructured"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
