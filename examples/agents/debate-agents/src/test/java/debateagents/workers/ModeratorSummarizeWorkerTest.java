package debateagents.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModeratorSummarizeWorkerTest {

    private final ModeratorSummarizeWorker worker = new ModeratorSummarizeWorker();

    @Test
    void taskDefName() {
        assertEquals("da_moderator_summarize", worker.getTaskDefName());
    }

    @Test
    void returnsSummaryAndVerdict() {
        Task task = taskWith(Map.of("topic", "Microservices vs Monolithic", "totalRounds", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String summary = (String) result.getOutputData().get("summary");
        assertNotNull(summary);
        assertTrue(summary.contains("Microservices vs Monolithic"));
        assertTrue(summary.contains("3 rounds"));

        String verdict = (String) result.getOutputData().get("verdict");
        assertNotNull(verdict);
        assertTrue(verdict.contains("Microservices vs Monolithic"));
    }

    @Test
    void summaryContainsKeyThemes() {
        Task task = taskWith(Map.of("topic", "Microservices", "totalRounds", 3));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("PRO"));
        assertTrue(summary.contains("CON"));
    }

    @Test
    void verdictContainsTopic() {
        Task task = taskWith(Map.of("topic", "Cloud Computing", "totalRounds", 2));
        TaskResult result = worker.execute(task);

        String verdict = (String) result.getOutputData().get("verdict");
        assertTrue(verdict.contains("Cloud Computing"));
    }

    @Test
    void handlesNullTopic() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", null);
        input.put("totalRounds", 3);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("summary"));
        assertNotNull(result.getOutputData().get("verdict"));
    }

    @Test
    void handlesMissingTopic() {
        Task task = taskWith(Map.of("totalRounds", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("the given topic"));
    }

    @Test
    void handlesBlankTopic() {
        Task task = taskWith(Map.of("topic", "   ", "totalRounds", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("the given topic"));
    }

    @Test
    void handlesStringTotalRounds() {
        Task task = taskWith(Map.of("topic", "AI Ethics", "totalRounds", "5"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("5 rounds"));
    }

    @Test
    void handlesNullTotalRounds() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", "AI Ethics");
        input.put("totalRounds", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("3 rounds"));
    }

    @Test
    void handlesMissingTotalRounds() {
        Task task = taskWith(Map.of("topic", "AI Ethics"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("3 rounds"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
