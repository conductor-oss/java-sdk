package multiagentresearch.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WriteReportWorkerTest {

    private final WriteReportWorker worker = new WriteReportWorker();

    @Test
    void taskDefName() {
        assertEquals("ra_write_report", worker.getTaskDefName());
    }

    @Test
    void producesReportWithAllFields() {
        Map<String, Object> synthesis = Map.of(
                "topic", "AI in SE",
                "webSourceCount", 3,
                "paperSourceCount", 2,
                "dbSourceCount", 3,
                "avgCredibility", 0.89);
        List<String> keyInsights = List.of("insight1", "insight2", "insight3", "insight4");

        Task task = taskWith(Map.of(
                "topic", "AI in software engineering",
                "synthesis", synthesis,
                "keyInsights", keyInsights,
                "sourceCount", 8));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("title"));
        assertNotNull(result.getOutputData().get("executiveSummary"));
        assertEquals(5, result.getOutputData().get("sections"));
        assertEquals(3200, result.getOutputData().get("wordCount"));
    }

    @Test
    void titleIncludesTopic() {
        Task task = taskWith(Map.of(
                "topic", "quantum computing",
                "sourceCount", 5));
        TaskResult result = worker.execute(task);

        String title = (String) result.getOutputData().get("title");
        assertTrue(title.contains("quantum computing"));
    }

    @Test
    void executiveSummaryIncludesSourceCount() {
        Task task = taskWith(Map.of(
                "topic", "blockchain",
                "sourceCount", 12));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("executiveSummary");
        assertTrue(summary.contains("12"));
    }

    @Test
    void executiveSummaryIncludesTopic() {
        Task task = taskWith(Map.of(
                "topic", "machine learning",
                "sourceCount", 5));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("executiveSummary");
        assertTrue(summary.contains("machine learning"));
    }

    @Test
    void sectionsIsFive() {
        Task task = taskWith(Map.of("topic", "test"));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("sections"));
    }

    @Test
    void wordCountIs3200() {
        Task task = taskWith(Map.of("topic", "test"));
        TaskResult result = worker.execute(task);

        assertEquals(3200, result.getOutputData().get("wordCount"));
    }

    @Test
    void handlesNullTopic() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", null);
        input.put("sourceCount", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("title"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("title"));
        assertNotNull(result.getOutputData().get("executiveSummary"));
    }

    @Test
    void handlesNonNumericSourceCount() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", "test");
        input.put("sourceCount", "not-a-number");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("executiveSummary");
        assertTrue(summary.contains("0"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
