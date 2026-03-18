package mapreduce.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MprReduceWorkerTest {

    private final MprReduceWorker worker = new MprReduceWorker();

    @Test
    void taskDefName() {
        assertEquals("mpr_reduce", worker.getTaskDefName());
    }

    @Test
    void aggregatesCountsFromThreeMappers() {
        Task task = taskWith(Map.of(
                "mapped1", List.of(Map.of("docIndex", 0, "count", 3)),
                "mapped2", List.of(Map.of("docIndex", 0, "count", 2)),
                "mapped3", List.of(Map.of("docIndex", 0, "count", 1))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(6, result.getOutputData().get("totalOccurrences"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void filtersZeroCountDocuments() {
        Task task = taskWith(Map.of(
                "mapped1", List.of(Map.of("docIndex", 0, "count", 5), Map.of("docIndex", 1, "count", 0)),
                "mapped2", List.of(),
                "mapped3", List.of()));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> reduced = (List<Map<String, Object>>) result.getOutputData().get("reduced");
        assertEquals(1, reduced.size(), "Only non-zero docs should appear in reduced");
        assertEquals(5, result.getOutputData().get("totalOccurrences"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalOccurrences"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
