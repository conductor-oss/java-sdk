package pdfprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateSummaryWorkerTest {

    private final GenerateSummaryWorker worker = new GenerateSummaryWorker();

    @Test
    void taskDefName() {
        assertEquals("pd_generate_summary", worker.getTaskDefName());
    }

    @Test
    void generatesSummaryWithSections() {
        List<Map<String, Object>> sections = List.of(
                Map.of("title", "Introduction", "content", "Intro text"),
                Map.of("title", "Architecture", "content", "Arch text")
        );
        Map<String, Object> analysis = Map.of("avgWordsPerSection", 25);
        Task task = taskWith(Map.of("sections", sections, "analysis", analysis));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("2 chapters"));
        assertTrue(summary.contains("introduction"));
        assertTrue(summary.contains("architecture"));
        assertTrue(summary.contains("25 words per section"));
    }

    @Test
    void summaryContainsChapterCount() {
        List<Map<String, Object>> sections = List.of(
                Map.of("title", "One", "content", "c1"),
                Map.of("title", "Two", "content", "c2"),
                Map.of("title", "Three", "content", "c3")
        );
        Task task = taskWith(Map.of("sections", sections, "analysis", Map.of("avgWordsPerSection", 10)));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("3 chapters"));
    }

    @Test
    void handlesEmptySections() {
        Task task = taskWith(Map.of("sections", List.of(), "analysis", Map.of("avgWordsPerSection", 0)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("0 chapters"));
    }

    @Test
    void handlesNullSections() {
        Map<String, Object> input = new HashMap<>();
        input.put("sections", null);
        input.put("analysis", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("summary"));
    }

    @Test
    void handlesMissingSections() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("0 chapters"));
    }

    @Test
    void handlesNullAnalysis() {
        Map<String, Object> input = new HashMap<>();
        input.put("sections", List.of(Map.of("title", "Test", "content", "data")));
        input.put("analysis", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("0 words per section"));
    }

    @Test
    void summaryIncludesAvgWords() {
        List<Map<String, Object>> sections = List.of(
                Map.of("title", "Data", "content", "text")
        );
        Task task = taskWith(Map.of("sections", sections, "analysis", Map.of("avgWordsPerSection", 42)));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("42 words per section"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
