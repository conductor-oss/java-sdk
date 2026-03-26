package dataarchival.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PurgeHotWorkerTest {

    private final PurgeHotWorker worker = new PurgeHotWorker();

    @Test
    void taskDefName() {
        assertEquals("arc_purge_hot", worker.getTaskDefName());
    }

    @Test
    void purgesWhenVerified() {
        Task task = taskWith(Map.of("staleRecordIds", List.of("R1", "R2", "R3"), "archiveVerified", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("purgedCount"));
        assertEquals(false, result.getOutputData().get("skipped"));
    }

    @Test
    void skipsWhenNotVerified() {
        Task task = taskWith(Map.of("staleRecordIds", List.of("R1", "R2"), "archiveVerified", false));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("purgedCount"));
        assertEquals(true, result.getOutputData().get("skipped"));
    }

    @Test
    void summaryContainsPurgeCount() {
        Task task = taskWith(Map.of("staleRecordIds", List.of("R1"), "archiveVerified", true));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("1"));
    }

    @Test
    void handlesEmptyIds() {
        Task task = taskWith(Map.of("staleRecordIds", List.of(), "archiveVerified", true));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("purgedCount"));
    }

    @Test
    void handlesNullIds() {
        Map<String, Object> input = new HashMap<>();
        input.put("staleRecordIds", null);
        input.put("archiveVerified", true);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("purgedCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
