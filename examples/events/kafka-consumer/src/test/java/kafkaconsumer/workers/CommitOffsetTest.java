package kafkaconsumer.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CommitOffsetTest {

    private final CommitOffset worker = new CommitOffset();

    @Test
    void taskDefName() {
        assertEquals("kc_commit_offset", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith("user-events", 3, "14582", "applied");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputCommittedIsTrue() {
        Task task = taskWith("user-events", 3, "14582", "applied");
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("committed"));
    }

    @Test
    void outputContainsCommittedOffset() {
        Task task = taskWith("user-events", 3, "14582", "applied");
        TaskResult result = worker.execute(task);
        assertEquals("14582", result.getOutputData().get("committedOffset"));
    }

    @Test
    void nextOffsetIs14583() {
        Task task = taskWith("user-events", 3, "14582", "applied");
        TaskResult result = worker.execute(task);
        assertEquals(14583, result.getOutputData().get("nextOffset"));
    }

    @Test
    void outputContainsCommittedAt() {
        Task task = taskWith("user-events", 3, "14582", "applied");
        TaskResult result = worker.execute(task);
        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("committedAt"));
    }

    @Test
    void committedOffsetPreservesInputOffset() {
        Task task = taskWith("orders", 0, "500", "applied");
        TaskResult result = worker.execute(task);
        assertEquals("500", result.getOutputData().get("committedOffset"));
    }

    @Test
    void deterministicOutputForSameInput() {
        Task task1 = taskWith("user-events", 3, "14582", "applied");
        Task task2 = taskWith("user-events", 3, "14582", "applied");

        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("committed"), r2.getOutputData().get("committed"));
        assertEquals(r1.getOutputData().get("committedOffset"), r2.getOutputData().get("committedOffset"));
        assertEquals(r1.getOutputData().get("nextOffset"), r2.getOutputData().get("nextOffset"));
        assertEquals(r1.getOutputData().get("committedAt"), r2.getOutputData().get("committedAt"));
    }

    @Test
    void acceptsDifferentProcessingResults() {
        Task task = taskWith("events", 1, "200", "skipped");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("committed"));
    }

    private Task taskWith(String topic, int partition, String offset, String processingResult) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("topic", topic);
        input.put("partition", partition);
        input.put("offset", offset);
        input.put("processingResult", processingResult);
        task.setInputData(input);
        return task;
    }
}
