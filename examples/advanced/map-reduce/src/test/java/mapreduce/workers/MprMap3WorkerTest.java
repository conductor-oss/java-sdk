package mapreduce.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MprMap3WorkerTest {

    private final MprMap3Worker worker = new MprMap3Worker();

    @Test
    void taskDefName() {
        assertEquals("mpr_map_3", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void countsSearchTermInDocuments() {
        Task task = taskWith(Map.of(
                "partition", List.of("java is great", "java java java"),
                "searchTerm", "java"));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> mapped = (List<Map<String, Object>>) result.getOutputData().get("mapped");
        assertEquals(2, mapped.size());
        assertEquals(1, mapped.get(0).get("count"));
        assertEquals(3, mapped.get(1).get("count"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("mapped"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
