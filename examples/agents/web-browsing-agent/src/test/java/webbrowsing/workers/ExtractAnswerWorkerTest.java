package webbrowsing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtractAnswerWorkerTest {

    private final ExtractAnswerWorker worker = new ExtractAnswerWorker();

    @Test
    void taskDefName() {
        assertEquals("wb_extract_answer", worker.getTaskDefName());
    }

    @Test
    void returnsNonEmptyAnswer() {
        List<Map<String, Object>> pageContents = List.of(
                Map.of("url", "https://a.com", "title", "Page A",
                        "content", "Conductor is a workflow engine."));

        Task task = taskWith(Map.of(
                "question", "What is Conductor?",
                "pageContents", pageContents));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertFalse(answer.isBlank());
    }

    @Test
    void returnsSourcesMatchingPageContents() {
        List<Map<String, Object>> pageContents = List.of(
                Map.of("url", "https://a.com", "title", "Page A", "content", "content a"),
                Map.of("url", "https://b.com", "title", "Page B", "content", "content b")
        );

        Task task = taskWith(Map.of(
                "question", "test question",
                "pageContents", pageContents));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> sources =
                (List<Map<String, String>>) result.getOutputData().get("sources");
        assertNotNull(sources);
        assertEquals(2, sources.size());
        assertEquals("Page A", sources.get(0).get("title"));
        assertEquals("https://a.com", sources.get(0).get("url"));
        assertEquals("Page B", sources.get(1).get("title"));
        assertEquals("https://b.com", sources.get(1).get("url"));
    }

    @Test
    void returnsConfidenceScore() {
        List<Map<String, Object>> pageContents = List.of(
                Map.of("url", "https://a.com", "title", "A", "content", "text"));

        Task task = taskWith(Map.of(
                "question", "test",
                "pageContents", pageContents));
        TaskResult result = worker.execute(task);

        assertEquals(0.91, result.getOutputData().get("confidence"));
    }

    @Test
    void returnsPositiveWordCount() {
        List<Map<String, Object>> pageContents = List.of(
                Map.of("url", "https://a.com", "title", "A", "content", "text"));

        Task task = taskWith(Map.of(
                "question", "test",
                "pageContents", pageContents));
        TaskResult result = worker.execute(task);

        int wordCount = ((Number) result.getOutputData().get("wordCount")).intValue();
        assertTrue(wordCount > 0, "Word count should be positive");
    }

    @Test
    void handlesNullQuestion() {
        List<Map<String, Object>> pageContents = List.of(
                Map.of("url", "https://a.com", "title", "A", "content", "text"));

        Map<String, Object> input = new HashMap<>();
        input.put("question", null);
        input.put("pageContents", pageContents);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void handlesNullPageContents() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", "test");
        input.put("pageContents", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, String>> sources =
                (List<Map<String, String>>) result.getOutputData().get("sources");
        assertEquals(0, sources.size());
    }

    @Test
    void handlesEmptyPageContents() {
        Task task = taskWith(Map.of(
                "question", "test",
                "pageContents", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, String>> sources =
                (List<Map<String, String>>) result.getOutputData().get("sources");
        assertEquals(0, sources.size());
    }

    @Test
    void answerContainsConductorInfo() {
        List<Map<String, Object>> pageContents = List.of(
                Map.of("url", "https://a.com", "title", "A", "content", "text"));

        Task task = taskWith(Map.of(
                "question", "What are features of Conductor?",
                "pageContents", pageContents));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("Conductor"), "Answer should mention Conductor");
        assertTrue(answer.contains("workflow"), "Answer should mention workflow");
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
