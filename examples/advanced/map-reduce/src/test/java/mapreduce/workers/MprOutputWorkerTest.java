package mapreduce.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MprOutputWorkerTest {

    private final MprOutputWorker worker = new MprOutputWorker();

    @Test
    void taskDefName() {
        assertEquals("mpr_output", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void summaryContainsSearchTerm() {
        Task task = taskWith(Map.of("searchTerm", "conductor",
                "reducedResult", List.of(Map.of("docIndex", 0, "count", 3))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> summary = (Map<String, Object>) result.getOutputData().get("summary");
        assertEquals("conductor", summary.get("term"));
        assertEquals(3, summary.get("totalOccurrences"));
        assertEquals(1, summary.get("documentsWithMatches"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesEmptyReducedResult() {
        Task task = taskWith(Map.of("searchTerm", "nothing"));
        TaskResult result = worker.execute(task);

        Map<String, Object> summary = (Map<String, Object>) result.getOutputData().get("summary");
        assertEquals(0, summary.get("totalOccurrences"));
        assertEquals(0, summary.get("documentsWithMatches"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("summary"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
