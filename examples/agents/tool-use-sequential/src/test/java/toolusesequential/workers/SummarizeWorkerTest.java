package toolusesequential.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SummarizeWorkerTest {

    private final SummarizeWorker worker = new SummarizeWorker();

    @Test
    void taskDefName() {
        assertEquals("ts_summarize", worker.getTaskDefName());
    }

    @Test
    void returnsSummaryString() {
        Task task = taskWith(Map.of(
                "query", "What is Conductor?",
                "sourceUrl", "https://orkes.io/docs",
                "sourceTitle", "Orkes Docs"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String summary = (String) result.getOutputData().get("summary");
        assertNotNull(summary);
        assertTrue(summary.length() > 100);
        assertTrue(summary.contains("Conductor"));
    }

    @Test
    void returnsConfidenceScore() {
        Task task = taskWith(Map.of("query", "Conductor"));
        TaskResult result = worker.execute(task);

        assertEquals(0.94, result.getOutputData().get("confidence"));
    }

    @Test
    void returnsPositiveWordCount() {
        Task task = taskWith(Map.of("query", "Conductor"));
        TaskResult result = worker.execute(task);

        Object wordCount = result.getOutputData().get("wordCount");
        assertNotNull(wordCount);
        assertTrue(((Number) wordCount).intValue() > 0);
    }

    @Test
    void returnsSourceUrl() {
        Task task = taskWith(Map.of("query", "test", "sourceUrl", "https://example.com/page"));
        TaskResult result = worker.execute(task);

        assertEquals("https://example.com/page", result.getOutputData().get("sourceUrl"));
    }

    @Test
    void summaryMentionsKeyTopics() {
        Task task = taskWith(Map.of("query", "What is Conductor?"));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("Netflix"));
        assertTrue(summary.contains("workflow"));
        assertTrue(summary.contains("orchestration"));
    }

    @Test
    void handlesNullQuery() {
        Map<String, Object> input = new HashMap<>();
        input.put("query", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("summary"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("summary"));
        assertEquals("https://example.com", result.getOutputData().get("sourceUrl"));
    }

    @Test
    void handlesBlankSourceTitle() {
        Task task = taskWith(Map.of("query", "test", "sourceTitle", "  "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
