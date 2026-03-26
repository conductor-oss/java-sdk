package dataarchival.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IdentifyStaleWorkerTest {

    private final IdentifyStaleWorker worker = new IdentifyStaleWorker();

    @Test
    void taskDefName() {
        assertEquals("arc_identify_stale", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void identifiesStaleRecords() {
        String oldDate = Instant.now().minus(200, ChronoUnit.DAYS).toString();
        String recentDate = Instant.now().minus(10, ChronoUnit.DAYS).toString();
        List<Map<String, Object>> records = List.of(
                Map.of("id", "R1", "lastAccessed", oldDate),
                Map.of("id", "R2", "lastAccessed", recentDate));
        Task task = taskWith(Map.of("records", records, "retentionDays", 90));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("totalCount"));
        assertEquals(1, result.getOutputData().get("staleCount"));
        List<Object> staleIds = (List<Object>) result.getOutputData().get("staleIds");
        assertTrue(staleIds.contains("R1"));
    }

    @Test
    void allFreshRecords() {
        String recent = Instant.now().minus(5, ChronoUnit.DAYS).toString();
        List<Map<String, Object>> records = List.of(
                Map.of("id", "R1", "lastAccessed", recent));
        Task task = taskWith(Map.of("records", records, "retentionDays", 90));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("staleCount"));
    }

    @Test
    void allStaleRecords() {
        String old = "2020-01-01T00:00:00Z";
        List<Map<String, Object>> records = List.of(
                Map.of("id", "R1", "lastAccessed", old),
                Map.of("id", "R2", "lastAccessed", old));
        Task task = taskWith(Map.of("records", records, "retentionDays", 30));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("staleCount"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("records", List.of(), "retentionDays", 90));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("totalCount"));
        assertEquals(0, result.getOutputData().get("staleCount"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        input.put("retentionDays", 90);
        Task task = taskWith(input);
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
